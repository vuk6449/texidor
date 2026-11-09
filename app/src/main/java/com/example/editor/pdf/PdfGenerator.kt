package com.example.editor.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.PageAnnotations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    /**
     * Converts a plain text or code string into a multi-page PDF document.
     */
    suspend fun generatePdfFromText(
        title: String,
        content: String,
        outputFile: File,
        isCode: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val margin = 40f
        val printableWidth = pageWidth - (margin * 2)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = if (isCode) 10f else 12f
            typeface = if (isCode) Typeface.MONOSPACE else Typeface.DEFAULT
            color = Color.rgb(30, 41, 59)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.rgb(148, 163, 184)
        }

        val lineSpacing = if (isCode) 15f else 18f
        val lines = mutableListOf<String>()

        // Split text into wrapped lines
        for (rawLine in content.lines()) {
            if (rawLine.isEmpty()) {
                lines.add("")
                continue
            }
            var remaining = rawLine
            while (remaining.isNotEmpty()) {
                val charsToFit = textPaint.breakText(remaining, true, printableWidth, null)
                if (charsToFit <= 0) break
                lines.add(remaining.substring(0, charsToFit))
                remaining = remaining.substring(charsToFit)
            }
        }

        var lineIndex = 0
        var pageNum = 1

        while (lineIndex < lines.size || pageNum == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            var y = margin + 20f

            // First page header
            if (pageNum == 1) {
                canvas.drawText(title, margin, y, titlePaint)
                y += 30f
                // Subtle horizontal rule
                val linePaint = Paint().apply {
                    color = Color.rgb(226, 232, 240)
                    strokeWidth = 1f
                }
                canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
            }

            // Draw content lines
            while (lineIndex < lines.size && y + lineSpacing < pageHeight - margin - 20f) {
                val lineText = lines[lineIndex]
                if (isCode) {
                    val lineNumStr = "${lineIndex + 1}".padStart(4, ' ') + "  "
                    val lineNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10f
                        typeface = Typeface.MONOSPACE
                        color = Color.rgb(148, 163, 184)
                    }
                    canvas.drawText(lineNumStr, margin, y, lineNumPaint)
                    canvas.drawText(lineText, margin + 35f, y, textPaint)
                } else {
                    canvas.drawText(lineText, margin, y, textPaint)
                }
                y += lineSpacing
                lineIndex++
            }

            // Footer
            val footerText = "OmniEdit  •  Page $pageNum"
            canvas.drawText(footerText, margin, pageHeight - margin, footerPaint)

            pdfDocument.finishPage(page)
            pageNum++

            if (lines.isEmpty()) break
        }

        try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Exports an existing PDF merged with user's freehand drawings and text stamp annotations.
     */
    suspend fun exportAnnotatedPdf(
        originalFile: File,
        outputFile: File,
        annotationsMap: Map<Int, PageAnnotations>
    ): Boolean = withContext(Dispatchers.IO) {
        if (!originalFile.exists()) return@withContext false

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = ParcelFileDescriptor.open(originalFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (pageIndex in 0 until pageCount) {
                val origPage = renderer.openPage(pageIndex)
                val width = origPage.width
                val height = origPage.height

                // Render high quality bitmap of original page
                val bitmapScale = 2f
                val bmpW = (width * bitmapScale).toInt().coerceAtLeast(100)
                val bmpH = (height * bitmapScale).toInt().coerceAtLeast(100)
                val pageBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                val bmpCanvas = Canvas(pageBitmap)
                bmpCanvas.drawColor(Color.WHITE)
                origPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                origPage.close()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageIndex + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Draw base page bitmap scaled to page bounds
                val destRect = android.graphics.Rect(0, 0, width, height)
                val srcRect = android.graphics.Rect(0, 0, bmpW, bmpH)
                val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(pageBitmap, srcRect, destRect, bmpPaint)

                // Overlay user annotations if any
                val pageAnn = annotationsMap[pageIndex]
                if (pageAnn != null) {
                    // Draw strokes
                    for (pathItem in pageAnn.paths) {
                        if (pathItem.points.size < 2) continue
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = pathItem.strokeWidth
                            color = try {
                                Color.parseColor(pathItem.colorHex)
                            } catch (_: Exception) {
                                Color.RED
                            }
                            if (pathItem.isHighlighter) {
                                alpha = 100 // Semi-transparent highlight
                            }
                        }

                        val path = Path()
                        val first = pathItem.points.first()
                        path.moveTo(first.x * width, first.y * height)
                        for (pt in pathItem.points.drop(1)) {
                            path.lineTo(pt.x * width, pt.y * height)
                        }
                        canvas.drawPath(path, strokePaint)
                    }

                    // Draw text annotations
                    for (textItem in pageAnn.textNotes) {
                        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = textItem.fontSize
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            color = try {
                                Color.parseColor(textItem.colorHex)
                            } catch (_: Exception) {
                                Color.BLACK
                            }
                        }
                        // Draw background pill for text note
                        val bgPaint = Paint().apply {
                            color = Color.argb(220, 255, 255, 255)
                            style = Paint.Style.FILL
                        }
                        val textWidth = notePaint.measureText(textItem.text)
                        val textX = textItem.x * width
                        val textY = textItem.y * height
                        canvas.drawRect(
                            textX - 4f,
                            textY - textItem.fontSize - 4f,
                            textX + textWidth + 8f,
                            textY + 6f,
                            bgPaint
                        )
                        canvas.drawText(textItem.text, textX, textY, notePaint)
                    }
                }

                pdfDocument.finishPage(pdfPage)
                pageBitmap.recycle()
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
            pdfDocument.close()
        }
    }

    /**
     * Creates a pre-populated sample PDF with modern vector cards and text
     * so user can test reading, zooming, and drawing annotations immediately.
     */
    fun createSamplePdf(context: Context): File {
        val file = File(context.filesDir, "OmniEdit_Sample_Guide.pdf")
        if (file.exists() && file.length() > 0) return file

        val pdfDocument = PdfDocument()
        val width = 595
        val height = 842

        // Page 1: Guide & Drawing canvas
        val pageInfo1 = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val c1 = page1.canvas
        c1.drawColor(Color.rgb(248, 250, 252))

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            color = Color.rgb(71, 85, 105)
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        c1.drawText("OmniEdit PDF Annotation Guide", 40f, 60f, headerPaint)
        c1.drawText("Annotate, sign, highlight, and export anywhere.", 40f, 85f, subPaint)

        // Card 1
        c1.drawRoundRect(40f, 110f, 555f, 240f, 16f, 16f, cardPaint)
        c1.drawRoundRect(40f, 110f, 555f, 240f, 16f, 16f, borderPaint)

        val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(2, 132, 199)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.rgb(51, 65, 85)
        }
        c1.drawText("1. Freehand Markup & Signature", 60f, 145f, cardTitlePaint)
        c1.drawText("Select the Pen tool from the top toolbar to draw annotations,", 60f, 175f, bodyPaint)
        c1.drawText("sketch diagrams, or write your signature directly on this page.", 60f, 195f, bodyPaint)
        c1.drawText("Use the Highlighter tool for translucent text highlighting.", 60f, 215f, bodyPaint)

        // Signature box
        val sigBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        c1.drawRoundRect(40f, 270f, 555f, 420f, 16f, 16f, sigBoxPaint)
        c1.drawRoundRect(40f, 270f, 555f, 420f, 16f, 16f, borderPaint)
        val sigTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(100, 116, 139)
        }
        c1.drawText("Practice Area: Draw your Signature or Markups Below ↓", 60f, 305f, sigTitle)

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(148, 163, 184)
        }
        c1.drawText("Page 1 of 2  •  OmniEdit Document Studio", 40f, 800f, footerPaint)
        pdfDocument.finishPage(page1)

        // Page 2: Text annotation demo
        val pageInfo2 = PdfDocument.PageInfo.Builder(width, height, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val c2 = page2.canvas
        c2.drawColor(Color.WHITE)

        c2.drawText("Contract Terms & Review Notes", 40f, 60f, headerPaint)
        c2.drawText("OmniEdit allows adding floating text stamps and notes.", 40f, 85f, subPaint)

        val reviewText = """
Clause 4.1: Software Freedom & Open Access
The software is provided free of charge, with no subscription tiers,
no hidden fees, and zero advertising interruptions.

Clause 4.2: Data Sovereignty
All text files, code files, and PDF annotations remain strictly
stored locally on the user's device. No cloud transmission occurs.

Clause 4.3: Compatibility
Full support for Android Phones, Tablets, Foldables, and Android TV.
""".trimIndent()

        var ty = 140f
        for (line in reviewText.lines()) {
            c2.drawText(line, 40f, ty, bodyPaint)
            ty += 22f
        }

        c2.drawText("Page 2 of 2  •  OmniEdit Document Studio", 40f, 800f, footerPaint)
        pdfDocument.finishPage(page2)

        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (_: Exception) {}
        finally {
            pdfDocument.close()
        }

        return file
    }
}
