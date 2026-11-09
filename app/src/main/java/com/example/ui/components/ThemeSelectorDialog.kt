package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import com.example.data.model.EditorPreferences
import com.example.ui.theme.EditorThemeColors
import com.example.ui.theme.ThemeRegistry

@Composable
fun ThemeSelectorDialog(
    preferences: EditorPreferences,
    currentThemeColors: EditorThemeColors,
    onSelectTheme: (AppTheme) -> Unit,
    onFontSizeDelta: (Float) -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onToggleAutoSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = currentThemeColors.surface,
        title = {
            Text(
                text = "Themes & Editor Preferences",
                color = currentThemeColors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SELECT COLOR PALETTE",
                    color = currentThemeColors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(AppTheme.entries) { theme ->
                        val themeColor = ThemeRegistry.getTheme(theme)
                        val isSelected = theme == preferences.theme

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) currentThemeColors.surfaceVariant else Color.Transparent)
                                .clickable { onSelectTheme(theme) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("theme_${theme.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color swatches
                                Row(modifier = Modifier.padding(end = 10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.editorBackground)
                                            .border(1.dp, currentThemeColors.textColor.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.primary)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.accent)
                                    )
                                }
                                Text(
                                    text = theme.displayName,
                                    color = currentThemeColors.textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = currentThemeColors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = currentThemeColors.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Font size row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Font Size: ${preferences.fontSizeSp.toInt()} sp",
                        color = currentThemeColors.textColor,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onFontSizeDelta(-1f) }) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease font",
                                tint = currentThemeColors.textColor
                            )
                        }
                        IconButton(onClick = { onFontSizeDelta(1f) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase font",
                                tint = currentThemeColors.textColor
                            )
                        }
                    }
                }

                // Line numbers toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show Line Numbers",
                        color = currentThemeColors.textColor,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = preferences.showLineNumbers,
                        onCheckedChange = { onToggleLineNumbers() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentThemeColors.primary,
                            checkedTrackColor = currentThemeColors.surfaceVariant
                        )
                    )
                }

                // Word wrap toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Word Wrap",
                        color = currentThemeColors.textColor,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = preferences.wordWrap,
                        onCheckedChange = { onToggleWordWrap() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentThemeColors.primary,
                            checkedTrackColor = currentThemeColors.surfaceVariant
                        )
                    )
                }

                // Auto-save toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Auto-Save",
                            color = currentThemeColors.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Persists changes to database every 3s",
                            color = currentThemeColors.gutterTextColor,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = preferences.autoSaveEnabled,
                        onCheckedChange = { onToggleAutoSave() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentThemeColors.primary,
                            checkedTrackColor = currentThemeColors.surfaceVariant
                        ),
                        modifier = Modifier.testTag("auto_save_toggle")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = currentThemeColors.primary)
            ) {
                Text("Done")
            }
        }
    )
}
