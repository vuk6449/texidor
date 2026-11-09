package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.theme.EditorThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileDrawerContent(
    documents: List<DocumentEntity>,
    currentDocId: Long?,
    filter: String,
    searchQuery: String,
    themeColors: EditorThemeColors,
    onSelectDocument: (DocumentEntity) -> Unit,
    onNewFileClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("ALL" to "All", "TXT" to "Notes", "CODE" to "Code", "PDF" to "PDF", "FAV" to "Starred")

    val filteredDocs = documents.filter { doc ->
        val matchesFilter = when (filter) {
            "TXT" -> doc.fileType == "TXT"
            "CODE" -> doc.fileType == "CODE"
            "PDF" -> doc.fileType == "PDF"
            "FAV" -> doc.isFavorite
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.content.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(themeColors.surface)
            .padding(16.dp)
    ) {
        // App header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = themeColors.surface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Texidor",
                    color = themeColors.textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Free Document & Code Studio",
                    color = themeColors.gutterTextColor,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onNewFileClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_new_file_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontSize = 12.sp, color = themeColors.surface, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onOpenFileClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.textColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("drawer_open_file_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search in drawer
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter files...", fontSize = 12.sp) },
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
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filters) { (key, label) ->
                val isSelected = filter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) themeColors.primary else themeColors.surfaceVariant)
                        .clickable { onFilterChange(key) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = themeColors.surfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        // Files List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredDocs, key = { it.id }) { doc ->
                val isCurrent = doc.id == currentDocId
                val icon = when (doc.fileType) {
                    "PDF" -> Icons.Default.PictureAsPdf
                    "CODE" -> Icons.Default.Code
                    else -> Icons.Default.Description
                }
                val iconColor = when (doc.fileType) {
                    "PDF" -> Color(0xFFEF4444)
                    "CODE" -> themeColors.primary
                    else -> themeColors.accent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) themeColors.surfaceVariant else Color.Transparent)
                        .clickable { onSelectDocument(doc) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("file_item_${doc.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = doc.fileType,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = doc.title,
                                color = if (isCurrent) themeColors.primary else themeColors.textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(doc.updatedAt))
                            Text(
                                text = "${doc.language.uppercase()} • $dateStr",
                                color = themeColors.gutterTextColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleFavorite(doc.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star",
                                tint = if (doc.isFavorite) Color(0xFFF59E0B) else themeColors.gutterTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteDocument(doc.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = themeColors.gutterTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
