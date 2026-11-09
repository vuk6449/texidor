package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.theme.EditorThemeColors
import com.example.ui.viewmodel.UiStats

@Composable
fun DocumentStatsDialog(
    document: DocumentEntity?,
    stats: UiStats,
    themeColors: EditorThemeColors,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = {
            Text(
                text = "Document Statistics",
                color = themeColors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (document != null) {
                    Text(
                        text = document.title,
                        color = themeColors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Format: ${document.fileType} (${document.language.uppercase()})",
                        color = themeColors.gutterTextColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                StatRow("Words", stats.words.toString(), themeColors)
                StatRow("Characters", stats.characters.toString(), themeColors)
                StatRow("Characters (no spaces)", stats.charactersNoSpaces.toString(), themeColors)
                StatRow("Lines", stats.lines.toString(), themeColors)
                StatRow("Reading Time", "~${stats.readingTimeMinutes} min", themeColors)
                StatRow("Size", "${stats.bytes} bytes", themeColors)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = themeColors.primary)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    themeColors: EditorThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = themeColors.textColor.copy(alpha = 0.8f), fontSize = 13.sp)
        Text(text = value, color = themeColors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
