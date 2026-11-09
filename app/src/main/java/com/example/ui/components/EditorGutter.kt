package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorThemeColors

@Composable
fun EditorGutter(
    lineCount: Int,
    currentLine: Int,
    themeColors: EditorThemeColors,
    fontSize: TextUnit,
    lineHeight: TextUnit = (fontSize.value * 1.4f).sp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(themeColors.gutterBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .widthIn(min = 36.dp),
        horizontalAlignment = Alignment.End
    ) {
        val total = lineCount.coerceAtLeast(1)
        for (line in 1..total) {
            val isCurrent = line == currentLine
            Text(
                text = line.toString(),
                color = if (isCurrent) themeColors.primary else themeColors.gutterTextColor,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
        }
    }
}
