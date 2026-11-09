package com.example.editor.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerManager(private val pdfFile: File) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    val isReady: Boolean
        get() = pdfRenderer != null

    init {
        openRenderer()
    }

    private fun openRenderer() {
        try {
            if (pdfFile.exists() && pdfFile.length() > 0) {
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor?.let {
                    pdfRenderer = PdfRenderer(it)
                }
            }
        } catch (_: Exception) {
            close()
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Fill with pure white background
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            try {
                page?.close()
            } catch (_: Exception) {}
        }
    }

    fun close() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
    }
}
