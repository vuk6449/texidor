package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorThemeColors

@Composable
fun AccessoryBar(
    themeColors: EditorThemeColors,
    onInsertSymbol: (String) -> Unit,
    onInsertTab: () -> Unit,
    onToggleComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "{", "}", "(", ")", "[", "]", "<", ">", "=", "==", "!=", "=>", "->",
        ":", ";", "\"", "'", "`", ",", ".", "/", "\\", "_", "-", "+", "*",
        "&", "&&", "|", "||", "$", "#", "!", "?", "%"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(themeColors.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab button
        AccessoryButton(
            themeColors = themeColors,
            onClick = onInsertTab,
            testTag = "tab_button"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardTab,
                    contentDescription = "Tab",
                    tint = themeColors.primary,
                    modifier = Modifier.height(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "Tab",
                    color = themeColors.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Comment toggle button
        AccessoryButton(
            themeColors = themeColors,
            onClick = onToggleComment,
            testTag = "comment_button"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Comment",
                    tint = themeColors.accent,
                    modifier = Modifier.height(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "//",
                    color = themeColors.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(themeColors.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Coding symbols
        for (sym in symbols) {
            AccessoryButton(
                themeColors = themeColors,
                onClick = { onInsertSymbol(sym) },
                testTag = "symbol_$sym"
            ) {
                Text(
                    text = sym,
                    color = themeColors.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
private fun AccessoryButton(
    themeColors: EditorThemeColors,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(themeColors.surfaceVariant.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = themeColors.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
