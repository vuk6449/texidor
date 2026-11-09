package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ProjectTemplateType
import com.example.ui.theme.EditorThemeColors

@Composable
fun NewProjectDialog(
    themeColors: EditorThemeColors,
    onCreateProject: (name: String, template: ProjectTemplateType) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf("MyProject") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplateType.WEB_APP) }

    Dialog(onDismissRequest = onDismiss) {
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
                        imageVector = Icons.Default.Workspaces,
                        contentDescription = null,
                        tint = themeColors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "New Project Workspace",
                        color = themeColors.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = themeColors.surfaceVariant,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT TEMPLATE",
                    color = themeColors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProjectTemplateType.values()) { template ->
                        val isSelected = template == selectedTemplate
                        val icon = getTemplateIcon(template)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) themeColors.primary.copy(alpha = 0.15f)
                                    else themeColors.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColors.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedTemplate = template
                                    if (projectName.isBlank() || projectName.startsWith("My")) {
                                        projectName = when (template) {
                                            ProjectTemplateType.WEB_APP -> "WebStudioApp"
                                            ProjectTemplateType.REACT_TAILWIND -> "ReactTailwindApp"
                                            ProjectTemplateType.NASM_ASSEMBLY -> "NasmKernel"
                                            ProjectTemplateType.NODE_JS -> "NodeScript"
                                            ProjectTemplateType.MARKDOWN_DOCS -> "Documentation"
                                            ProjectTemplateType.BLANK -> "MyWorkspace"
                                        }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) themeColors.primary else themeColors.gutterTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.title,
                                        color = if (isSelected) themeColors.primary else themeColors.textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = template.description,
                                        color = themeColors.gutterTextColor,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = themeColors.gutterTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (projectName.isNotBlank()) {
                                onCreateProject(projectName.trim(), selectedTemplate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                    ) {
                        Text(
                            "Create Project",
                            color = themeColors.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getTemplateIcon(template: ProjectTemplateType): ImageVector {
    return when (template) {
        ProjectTemplateType.WEB_APP -> Icons.Default.Language
        ProjectTemplateType.REACT_TAILWIND -> Icons.Default.Code
        ProjectTemplateType.NASM_ASSEMBLY -> Icons.Default.Memory
        ProjectTemplateType.NODE_JS -> Icons.Default.Terminal
        ProjectTemplateType.MARKDOWN_DOCS -> Icons.Default.Description
        ProjectTemplateType.BLANK -> Icons.Default.Folder
    }
}
