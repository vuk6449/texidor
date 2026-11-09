package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.theme.EditorThemeColors

data class FileSearchResult(
    val document: DocumentEntity,
    val matchedInTitle: Boolean,
    val matchingLineNumber: Int? = null,
    val snippet: String? = null
)

@Composable
fun FileSearchDialog(
    allDocuments: List<DocumentEntity>,
    themeColors: EditorThemeColors,
    onSelectDocument: (DocumentEntity, lineTarget: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "NAME", "CONTENT", "CODE", "TXT", "PDF"

    val filterOptions = listOf(
        "ALL" to "All",
        "NAME" to "File Name",
        "CONTENT" to "In Content",
        "CODE" to "Code",
        "TXT" to "Notes",
        "PDF" to "PDF"
    )

    val searchResults = remember(searchQuery, activeFilter, allDocuments) {
        val query = searchQuery.trim()
        val results = mutableListOf<FileSearchResult>()

        for (doc in allDocuments) {
            // Apply category filter
            val matchesCategory = when (activeFilter) {
                "CODE" -> doc.fileType == "CODE"
                "TXT" -> doc.fileType == "TXT"
                "PDF" -> doc.fileType == "PDF"
                else -> true
            }
            if (!matchesCategory) continue

            if (query.isEmpty()) {
                // If query empty, show recent files
                results.add(FileSearchResult(document = doc, matchedInTitle = true))
                continue
            }

            val queryLower = query.lowercase()
            val titleMatches = doc.title.lowercase().contains(queryLower)

            if (titleMatches && activeFilter != "CONTENT") {
                results.add(
                    FileSearchResult(
                        document = doc,
                        matchedInTitle = true
                    )
                )
            }

            // Search inside content
            if (activeFilter != "NAME" && doc.fileType != "PDF") {
                val lines = doc.content.lines()
                for ((idx, line) in lines.withIndex()) {
                    if (line.lowercase().contains(queryLower)) {
                        results.add(
                            FileSearchResult(
                                document = doc,
                                matchedInTitle = false,
                                matchingLineNumber = idx + 1,
                                snippet = line.trim()
                            )
                        )
                        // Limit to top 3 line matches per document to avoid flooding
                        if (results.count { it.document.id == doc.id && !it.matchedInTitle } >= 3) {
                            break
                        }
                    }
                }
            }
        }
        results
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FindInPage,
                    contentDescription = null,
                    tint = themeColors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find in Files",
                    color = themeColors.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search file names or code/text...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = themeColors.gutterTextColor)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = themeColors.surfaceVariant,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filters Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filterOptions) { (key, label) ->
                        val isSelected = activeFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) themeColors.primary else themeColors.surfaceVariant)
                                .clickable { activeFilter = key }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) themeColors.surface else themeColors.textColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results count
                Text(
                    text = if (searchQuery.isEmpty()) {
                        "Showing ${searchResults.size} files"
                    } else {
                        "Found ${searchResults.size} matches"
                    },
                    color = themeColors.gutterTextColor,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = themeColors.surfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                // Results list
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching files or content found.",
                            color = themeColors.gutterTextColor,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                searchQuery = searchQuery,
                                themeColors = themeColors,
                                onClick = {
                                    onSelectDocument(result.document, result.matchingLineNumber)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
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
private fun SearchResultItem(
    result: FileSearchResult,
    searchQuery: String,
    themeColors: EditorThemeColors,
    onClick: () -> Unit
) {
    val icon = when (result.document.fileType) {
        "PDF" -> Icons.Default.PictureAsPdf
        "CODE" -> Icons.Default.Code
        else -> Icons.Default.Description
    }
    val iconTint = when (result.document.fileType) {
        "PDF" -> Color(0xFFEF4444)
        "CODE" -> themeColors.primary
        else -> themeColors.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = highlightMatchingText(result.document.title, searchQuery, themeColors.primary),
                    color = themeColors.textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = result.document.language.uppercase(),
                    color = themeColors.gutterTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (result.snippet != null && result.matchingLineNumber != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Line ${result.matchingLineNumber}: ",
                        color = themeColors.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = highlightMatchingText(result.snippet, searchQuery, Color(0xFFFBBF24)),
                        color = themeColors.textColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun highlightMatchingText(
    text: String,
    query: String,
    highlightColor: Color
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    return buildAnnotatedString {
        var currentIndex = 0
        val queryLower = query.lowercase()
        val textLower = text.lowercase()

        while (currentIndex < text.length) {
            val matchIndex = textLower.indexOf(queryLower, currentIndex)
            if (matchIndex == -1) {
                append(text.substring(currentIndex))
                break
            } else {
                append(text.substring(currentIndex, matchIndex))
                withStyle(
                    SpanStyle(
                        background = highlightColor.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(matchIndex, matchIndex + query.length))
                }
                currentIndex = matchIndex + query.length
            }
        }
    }
}
