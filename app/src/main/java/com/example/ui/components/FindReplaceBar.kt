package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorThemeColors

@Composable
fun FindReplaceBar(
    searchQuery: String,
    replaceQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    isCaseSensitive: Boolean,
    themeColors: EditorThemeColors,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onToggleCaseSensitive: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(themeColors.surface)
            .padding(8.dp)
    ) {
        // Find row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Find in document...", fontSize = 13.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = themeColors.textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.surfaceVariant,
                    focusedTextColor = themeColors.textColor,
                    unfocusedTextColor = themeColors.textColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("find_input")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Match count
            val countText = if (searchQuery.isEmpty()) {
                ""
            } else if (matchCount == 0) {
                "0/0"
            } else {
                "${currentMatchIndex + 1}/$matchCount"
            }
            if (countText.isNotEmpty()) {
                Text(
                    text = countText,
                    color = if (matchCount > 0) themeColors.primary else themeColors.gutterTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            IconButton(onClick = onFindPrev, modifier = Modifier.testTag("find_prev_button")) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous Match",
                    tint = themeColors.textColor
                )
            }

            IconButton(onClick = onFindNext, modifier = Modifier.testTag("find_next_button")) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next Match",
                    tint = themeColors.textColor
                )
            }

            IconButton(
                onClick = onToggleCaseSensitive,
                modifier = Modifier.testTag("case_sensitive_toggle")
            ) {
                Text(
                    text = "Aa",
                    color = if (isCaseSensitive) themeColors.primary else themeColors.gutterTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.testTag("close_find_bar")) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Find",
                    tint = themeColors.gutterTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Replace row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = replaceQuery,
                onValueChange = onReplaceQueryChange,
                placeholder = { Text("Replace with...", fontSize = 13.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = themeColors.textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = themeColors.surfaceVariant,
                    focusedTextColor = themeColors.textColor,
                    unfocusedTextColor = themeColors.textColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("replace_input")
            )

            Spacer(modifier = Modifier.width(6.dp))

            TextButton(
                onClick = onReplaceCurrent,
                colors = ButtonDefaults.textButtonColors(contentColor = themeColors.primary),
                modifier = Modifier.testTag("replace_button")
            ) {
                Text("Replace", fontSize = 12.sp)
            }

            TextButton(
                onClick = onReplaceAll,
                colors = ButtonDefaults.textButtonColors(contentColor = themeColors.accent),
                modifier = Modifier.testTag("replace_all_button")
            ) {
                Text("All", fontSize = 12.sp)
            }
        }
    }
}
