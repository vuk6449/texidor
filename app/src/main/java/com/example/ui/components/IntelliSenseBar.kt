package com.example.ui.components

import androidx.compose.foundation.background
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
import com.example.editor.syntax.CompletionItem
import com.example.editor.syntax.CompletionKind
import com.example.ui.theme.EditorThemeColors

@Composable
fun IntelliSenseBar(
    completions: List<CompletionItem>,
    themeColors: EditorThemeColors,
    onSelectCompletion: (CompletionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (completions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(themeColors.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (item in completions) {
            CompletionChip(
                item = item,
                themeColors = themeColors,
                onClick = { onSelectCompletion(item) }
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@Composable
private fun CompletionChip(
    item: CompletionItem,
    themeColors: EditorThemeColors,
    onClick: () -> Unit
) {
    val (badgeText, badgeColor) = when (item.kind) {
        CompletionKind.SNIPPET -> Pair("snip", Color(0xFFF59E0B))
        CompletionKind.KEYWORD -> Pair("key", themeColors.keywordColor)
        CompletionKind.FUNCTION -> Pair("fn", themeColors.functionColor)
        CompletionKind.TYPE -> Pair("type", themeColors.typeColor)
        CompletionKind.VARIABLE -> Pair("var", themeColors.accent)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(themeColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("completion_${item.label}"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.label,
                color = themeColors.textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            if (item.detail != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.detail,
                    color = themeColors.gutterTextColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}
