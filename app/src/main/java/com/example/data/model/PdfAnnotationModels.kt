package com.example.data.model

data class AnnotationPoint(
    val x: Float,
    val y: Float
)

data class DrawPathItem(
    val points: List<AnnotationPoint>,
    val colorHex: String,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class TextAnnotationItem(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val colorHex: String,
    val fontSize: Float = 14f
)

data class PageAnnotations(
    val pageIndex: Int,
    val paths: List<DrawPathItem> = emptyList(),
    val textNotes: List<TextAnnotationItem> = emptyList()
)
