package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EditorThemeColors

@Composable
fun CloneGitHubDialog(
    themeColors: EditorThemeColors,
    isCloning: Boolean,
    cloneStatus: String?,
    onClone: (repoUrlOrSlug: String, token: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var repoUrlInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var showTokenField by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isCloning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Clone from GitHub",
                        color = themeColors.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter a public repository URL or slug to download directly into your workspace.",
                    color = themeColors.gutterTextColor,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = repoUrlInput,
                    onValueChange = { repoUrlInput = it },
                    label = { Text("Repository (e.g. facebook/react or URL)") },
                    placeholder = { Text("owner/repo or https://github.com/...", color = themeColors.gutterTextColor.copy(alpha = 0.5f)) },
                    singleLine = true,
                    enabled = !isCloning,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = themeColors.surfaceVariant,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional PAT toggle
                if (!showTokenField) {
                    TextButton(
                        onClick = { showTokenField = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColors.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Personal Access Token (for private repos/rate limits)", fontSize = 11.sp, color = themeColors.primary)
                    }
                } else {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("GitHub Token (Optional)") },
                        singleLine = true,
                        enabled = !isCloning,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = themeColors.surfaceVariant,
                            focusedTextColor = themeColors.textColor,
                            unfocusedTextColor = themeColors.textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isCloning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = cloneStatus ?: "Cloning repository...",
                            color = themeColors.textColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCloning
                    ) {
                        Text("Cancel", color = themeColors.gutterTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (repoUrlInput.isNotBlank()) {
                                onClone(repoUrlInput.trim(), tokenInput.trim().ifEmpty { null })
                            }
                        },
                        enabled = !isCloning && repoUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text(
                            "Clone Repo",
                            color = themeColors.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
