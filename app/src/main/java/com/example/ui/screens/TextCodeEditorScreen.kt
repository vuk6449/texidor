package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.EditorPreferences
import com.example.editor.syntax.CompletionItem
import com.example.editor.syntax.EditorLanguage
import com.example.editor.syntax.SyntaxHighlighter
import com.example.ui.components.AccessoryBar
import com.example.ui.components.EditorGutter
import com.example.ui.components.IntelliSenseBar
import com.example.ui.theme.EditorThemeColors

@Composable
fun TextCodeEditorScreen(
    currentDocument: DocumentEntity,
    openTabs: List<DocumentEntity>,
    editorValue: androidx.compose.ui.text.input.TextFieldValue,
    preferences: EditorPreferences,
    themeColors: EditorThemeColors,
    completions: List<CompletionItem>,
    onEditorValueChanged: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onTabSelected: (DocumentEntity) -> Unit,
    onCloseTab: (Long) -> Unit,
    onApplyCompletion: (CompletionItem) -> Unit,
    onInsertSymbol: (String) -> Unit,
    onInsertTab: () -> Unit,
    onToggleComment: () -> Unit,
    onEnterKey: () -> Boolean,
    searchMatches: List<IntRange> = emptyList(),
    currentMatchIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val language = remember(currentDocument.language) {
        EditorLanguage.fromId(currentDocument.language)
    }

    val syntaxHighlighter = remember(language, themeColors, searchMatches, currentMatchIndex) {
        SyntaxHighlighter(language, themeColors, searchMatches, currentMatchIndex)
    }

    val lineCount by remember(editorValue.text) {
        derivedStateOf {
            if (editorValue.text.isEmpty()) 1 else editorValue.text.lines().size
        }
    }

    val currentLineIndex by remember(editorValue.selection, editorValue.text) {
        derivedStateOf {
            val cursor = editorValue.selection.start.coerceIn(0, editorValue.text.length)
            editorValue.text.take(cursor).count { it == '\n' } + 1
        }
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val font = when (preferences.fontFamilyName) {
        "SansSerif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        else -> FontFamily.Monospace
    }

    val fontSize = preferences.fontSizeSp.sp
    val lineHeight = (preferences.fontSizeSp * 1.45f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.editorBackground)
    ) {
        // Tabs bar across top if more than 1 tab open
        if (openTabs.size > 1) {
            TabsRow(
                tabs = openTabs,
                currentTabId = currentDocument.id,
                themeColors = themeColors,
                onTabSelected = onTabSelected,
                onCloseTab = onCloseTab
            )
        }

        // Main Editor Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Line Numbers Gutter
                if (preferences.showLineNumbers) {
                    EditorGutter(
                        lineCount = lineCount,
                        currentLine = currentLineIndex,
                        themeColors = themeColors,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }

                // Text Editing Area
                val textModifier = if (preferences.wordWrap) {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                }

                BasicTextField(
                    value = editorValue,
                    onValueChange = onEditorValueChanged,
                    visualTransformation = syntaxHighlighter,
                    textStyle = TextStyle(
                        color = themeColors.textColor,
                        fontSize = fontSize,
                        fontFamily = font,
                        lineHeight = lineHeight
                    ),
                    cursorBrush = SolidColor(themeColors.primary),
                    modifier = textModifier
                        .testTag("code_editor_textfield")
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter) {
                                onEnterKey()
                            } else {
                                false
                            }
                        }
                )
            }
        }

        // IntelliSense completion bar (shows if suggestions exist)
        if (completions.isNotEmpty()) {
            IntelliSenseBar(
                completions = completions,
                themeColors = themeColors,
                onSelectCompletion = onApplyCompletion
            )
        }

        // Bottom coding accessories keyboard bar
        AccessoryBar(
            themeColors = themeColors,
            onInsertSymbol = onInsertSymbol,
            onInsertTab = onInsertTab,
            onToggleComment = onToggleComment
        )
    }
}

@Composable
private fun TabsRow(
    tabs: List<DocumentEntity>,
    currentTabId: Long,
    themeColors: EditorThemeColors,
    onTabSelected: (DocumentEntity) -> Unit,
    onCloseTab: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(themeColors.surface)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tab in tabs) {
            val isSelected = tab.id == currentTabId
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(if (isSelected) themeColors.editorBackground else themeColors.surface)
                    .clickable { onTabSelected(tab) }
                    .padding(start = 10.dp, end = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) themeColors.primary else themeColors.textColor.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { onCloseTab(tab.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = if (isSelected) themeColors.textColor.copy(alpha = 0.7f) else themeColors.gutterTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Active bottom hairline accent
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(themeColors.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}
