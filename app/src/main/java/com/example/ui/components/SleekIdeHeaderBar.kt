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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.editor.syntax.EditorLanguage
import com.example.ui.theme.EditorThemeColors

@Composable
fun SleekIdeHeaderBar(
    currentDoc: DocumentEntity?,
    isModified: Boolean,
    isWideScreen: Boolean,
    isExecuting: Boolean,
    isHtmlPreviewVisible: Boolean,
    themeColors: EditorThemeColors,
    onToggleDrawer: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleFindReplace: () -> Unit,
    onSearchFiles: () -> Unit,
    onRunCode: () -> Unit,
    onTogglePreview: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenStats: () -> Unit,
    onExportPdf: () -> Unit,
    onExportDocument: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onOpenTerminal: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(themeColors.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT CLUSTER: Drawer Toggle & Document Breadcrumb Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (!isWideScreen) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
                            .clickable(onClick = onToggleDrawer)
                            .testTag("menu_drawer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Project Files",
                            tint = themeColors.textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Active Document Breadcrumb Pill
                Box {
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                width = 1.dp,
                                color = themeColors.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (currentDoc != null && currentDoc.fileType != "PDF") {
                                    showLanguageMenu = true
                                }
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language mini tag
                        val langLabel = when {
                            currentDoc?.fileType == "PDF" -> "PDF"
                            currentDoc != null -> currentDoc.language.uppercase().take(4)
                            else -> "TXT"
                        }
                        Text(
                            text = langLabel,
                            color = themeColors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Filename
                        Text(
                            text = currentDoc?.title ?: "Texidor",
                            color = themeColors.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )

                        // Unsaved modified indicator dot
                        if (isModified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(themeColors.primary)
                            )
                        }

                        if (currentDoc != null && currentDoc.fileType != "PDF") {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Language",
                                tint = themeColors.textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Language dropdown
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        for (lang in EditorLanguage.entries) {
                            DropdownMenuItem(
                                text = { Text(lang.displayName, fontSize = 13.sp) },
                                onClick = {
                                    onSetLanguage(lang.id)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT CLUSTER: Run Pill + Quick Action Toolbar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // RUN PILL (Highlighted Primary IDE Action)
                if (currentDoc?.fileType != "PDF") {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.primary)
                            .clickable(onClick = onRunCode)
                            .padding(horizontal = 10.dp)
                            .testTag("run_code_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = themeColors.surface
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run Code",
                                    tint = themeColors.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExecuting) "Running" else "Run",
                                color = themeColors.surface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // COMPACT ACTION CAPSULE
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColors.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            width = 1.dp,
                            color = themeColors.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (isModified) themeColors.primary else themeColors.textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (currentDoc?.fileType != "PDF") {
                        // Undo
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = themeColors.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Search & Replace
                        IconButton(
                            onClick = onToggleFindReplace,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Find & Replace",
                                tint = themeColors.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Web Preview button for HTML / Markdown / Web projects
                    val isWebOrMd = currentDoc?.language == "html" ||
                            currentDoc?.language == "markdown" ||
                            currentDoc?.language == "typescript" ||
                            currentDoc?.language == "javascript" ||
                            currentDoc?.title?.endsWith(".html", ignoreCase = true) == true ||
                            currentDoc?.title?.endsWith(".md", ignoreCase = true) == true ||
                            currentDoc?.title?.endsWith(".markdown", ignoreCase = true) == true ||
                            currentDoc?.title?.endsWith(".ts", ignoreCase = true) == true ||
                            currentDoc?.title?.endsWith(".tsx", ignoreCase = true) == true
                    if (isWebOrMd) {
                        IconButton(
                            onClick = onTogglePreview,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Preview",
                                tint = if (isHtmlPreviewVisible) themeColors.primary else themeColors.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Integrated Ubuntu Terminal button
                    IconButton(
                        onClick = onOpenTerminal,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Claude & GitHub Copilot AI Assistant button
                    IconButton(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("ai_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Claude & Copilot",
                            tint = themeColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = themeColors.textColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ubuntu Shell Terminal", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onOpenTerminal()
                                    showOverflowMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Claude & Copilot AI", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = themeColors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onOpenAiAssistant()
                                    showOverflowMenu = false
                                }
                            )
                            HorizontalDivider(color = themeColors.surfaceVariant)

                            if (currentDoc?.fileType != "PDF") {
                                DropdownMenuItem(
                                    text = { Text("Run Code (Ctrl+R)", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        onRunCode()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Redo (Ctrl+Y)", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Redo,
                                            contentDescription = null,
                                            tint = themeColors.textColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        onRedo()
                                        showOverflowMenu = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Find in Files (Ctrl+P)", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FindInPage,
                                        contentDescription = null,
                                        tint = themeColors.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onSearchFiles()
                                    showOverflowMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Theme Color Scheme", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = themeColors.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onOpenThemePicker()
                                    showOverflowMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Document Statistics", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = themeColors.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onOpenStats()
                                    showOverflowMenu = false
                                }
                            )

                            if (currentDoc?.fileType != "PDF") {
                                DropdownMenuItem(
                                    text = { Text("Export as PDF", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = themeColors.textColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        onExportPdf()
                                        showOverflowMenu = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Export to Storage", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = themeColors.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onExportDocument()
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 1px subtle IDE hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}
