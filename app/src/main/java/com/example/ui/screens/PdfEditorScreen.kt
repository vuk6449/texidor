package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationPoint
import com.example.data.model.PageAnnotations
import com.example.ui.theme.EditorThemeColors
import com.example.ui.viewmodel.PdfTool

@Composable
fun PdfEditorScreen(
    pageBitmap: Bitmap?,
    currentPage: Int,
    pageCount: Int,
    activeTool: PdfTool,
    activeColorHex: String,
    activeStrokeWidth: Float,
    annotationsMap: Map<Int, PageAnnotations>,
    currentDrawingPoints: List<AnnotationPoint>,
    themeColors: EditorThemeColors,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    onSetTool: (PdfTool) -> Unit,
    onSetColor: (String) -> Unit,
    onAddPoint: (Float, Float) -> Unit,
    onFinishDrawing: () -> Unit,
    onAddTextNote: (String, Float, Float) -> Unit,
    onClearPage: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableStateOf(1.0f) }
    var textDialogCoords by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    val colorPalette = listOf(
        "#EF4444", // Red
        "#38BDF8", // Blue
        "#FBBF24", // Yellow
        "#22C55E", // Green
        "#A855F7", // Purple
        "#000000", // Black
        "#FFFFFF"  // White
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // PDF Controls Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page navigation
            IconButton(
                onClick = onPrevPage,
                enabled = currentPage > 0,
                modifier = Modifier.size(36.dp).testTag("pdf_prev_page")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page", tint = themeColors.textColor)
            }

            Text(
                text = "Page ${currentPage + 1} of ${pageCount.coerceAtLeast(1)}",
                color = themeColors.textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = onNextPage,
                enabled = currentPage + 1 < pageCount,
                modifier = Modifier.size(36.dp).testTag("pdf_next_page")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Page", tint = themeColors.textColor)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Zoom controls
            IconButton(
                onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = themeColors.textColor)
            }

            IconButton(
                onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = themeColors.textColor)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Export Annotated PDF
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 11.sp, color = themeColors.surface, fontWeight = FontWeight.Bold)
            }
        }

        // Tool Selector Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolChip(
                icon = Icons.Default.PanTool,
                label = "Pan",
                isSelected = activeTool == PdfTool.NONE,
                themeColors = themeColors,
                onClick = { onSetTool(PdfTool.NONE) }
            )

            ToolChip(
                icon = Icons.Default.Edit,
                label = "Pen",
                isSelected = activeTool == PdfTool.PEN,
                themeColors = themeColors,
                onClick = { onSetTool(PdfTool.PEN) }
            )

            ToolChip(
                icon = Icons.Default.Highlight,
                label = "Highlight",
                isSelected = activeTool == PdfTool.HIGHLIGHTER,
                themeColors = themeColors,
                onClick = { onSetTool(PdfTool.HIGHLIGHTER) }
            )

            ToolChip(
                icon = Icons.Default.TextFields,
                label = "Text Stamp",
                isSelected = activeTool == PdfTool.TEXT_NOTE,
                themeColors = themeColors,
                onClick = { onSetTool(PdfTool.TEXT_NOTE) }
            )

            ToolChip(
                icon = Icons.Default.Delete,
                label = "Clear",
                isSelected = false,
                themeColors = themeColors,
                onClick = onClearPage
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Color Swatches
            for (hex in colorPalette) {
                val isSelected = activeColorHex == hex
                val swatchColor = try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) {
                    Color.Red
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) themeColors.textColor else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onSetColor(hex) }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        // PDF Canvas Viewer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pageBitmap != null) {
                val aspectRatio = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
                val displayWidth = (600.dp * zoomScale)
                val displayHeight = (displayWidth / aspectRatio)

                Box(
                    modifier = Modifier
                        .size(displayWidth, displayHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .pointerInput(activeTool) {
                            if (activeTool == PdfTool.PEN || activeTool == PdfTool.HIGHLIGHTER) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        onAddPoint(offset.x / size.width, offset.y / size.height)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        onAddPoint(
                                            change.position.x / size.width,
                                            change.position.y / size.height
                                        )
                                    },
                                    onDragEnd = {
                                        onFinishDrawing()
                                    }
                                )
                            } else if (activeTool == PdfTool.TEXT_NOTE) {
                                detectTapGestures { offset ->
                                    textDialogCoords = Pair(
                                        offset.x / size.width,
                                        offset.y / size.height
                                    )
                                }
                            }
                        }
                ) {
                    // Base PDF rendered page
                    Image(
                        bitmap = pageBitmap.asImageBitmap(),
                        contentDescription = "PDF Page $currentPage",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay vector drawings and text notes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Existing saved paths
                        val pageAnn = annotationsMap[currentPage]
                        if (pageAnn != null) {
                            for (p in pageAnn.paths) {
                                if (p.points.size < 2) continue
                                val pathColor = try {
                                    Color(android.graphics.Color.parseColor(p.colorHex))
                                } catch (_: Exception) {
                                    Color.Red
                                }
                                val alpha = if (p.isHighlighter) 0.35f else 1f
                                val path = Path()
                                path.moveTo(p.points.first().x * canvasW, p.points.first().y * canvasH)
                                for (pt in p.points.drop(1)) {
                                    path.lineTo(pt.x * canvasW, pt.y * canvasH)
                                }
                                drawPath(
                                    path = path,
                                    color = pathColor.copy(alpha = alpha),
                                    style = Stroke(
                                        width = p.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // In-progress active drawing path
                        if (currentDrawingPoints.size >= 2) {
                            val activeColor = try {
                                Color(android.graphics.Color.parseColor(activeColorHex))
                            } catch (_: Exception) {
                                Color.Red
                            }
                            val isHighlighter = activeTool == PdfTool.HIGHLIGHTER
                            val activePath = Path()
                            activePath.moveTo(
                                currentDrawingPoints.first().x * canvasW,
                                currentDrawingPoints.first().y * canvasH
                            )
                            for (pt in currentDrawingPoints.drop(1)) {
                                activePath.lineTo(pt.x * canvasW, pt.y * canvasH)
                            }
                            drawPath(
                                path = activePath,
                                color = activeColor.copy(alpha = if (isHighlighter) 0.4f else 1f),
                                style = Stroke(
                                    width = if (isHighlighter) 22f else activeStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Render text stamp notes on page
                    val notes = annotationsMap[currentPage]?.textNotes ?: emptyList()
                    for (note in notes) {
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = (displayWidth * note.x),
                                    top = (displayHeight * note.y)
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEF08A)) // Yellow sticky note
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.text,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColors.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rendering PDF page...", color = themeColors.textColor, fontSize = 13.sp)
                }
            }
        }
    }

    // Text Stamp Input Dialog
    if (textDialogCoords != null) {
        AlertDialog(
            onDismissRequest = { textDialogCoords = null },
            title = { Text("Add Text Annotation Stamp", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteInputText,
                    onValueChange = { noteInputText = it },
                    placeholder = { Text("Enter annotation text...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pdf_text_note_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coords = textDialogCoords
                        if (coords != null && noteInputText.isNotBlank()) {
                            onAddTextNote(noteInputText.trim(), coords.first, coords.second)
                        }
                        noteInputText = ""
                        textDialogCoords = null
                    }
                ) {
                    Text("Add Stamp")
                }
            },
            dismissButton = {
                TextButton(onClick = { textDialogCoords = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    themeColors: EditorThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) themeColors.primary else themeColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) themeColors.surface else themeColors.textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) themeColors.surface else themeColors.textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
