package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.Project
import com.example.data.model.ProjectFileNode
import com.example.ui.theme.EditorThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VscProjectExplorer(
    currentProject: Project?,
    allProjects: List<Project>,
    fileTree: ProjectFileNode?,
    selectedFilePath: String?,
    themeColors: EditorThemeColors,
    recentDocuments: List<DocumentEntity> = emptyList(),
    onSelectFile: (String) -> Unit,
    onSelectDocument: (DocumentEntity) -> Unit = {},
    onCreateFile: (parentRelPath: String, fileName: String) -> Unit,
    onCreateFolder: (parentRelPath: String, folderName: String) -> Unit,
    onDeleteItem: (relPath: String) -> Unit,
    onSelectProject: (Project) -> Unit,
    onOpenFolder: () -> Unit,
    onNewProject: () -> Unit,
    onCloneGitHub: () -> Unit,
    onExportZip: () -> Unit,
    onExportFolder: () -> Unit,
    onRefreshTree: () -> Unit,
    onOpenTerminal: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSidebarTab by remember { mutableStateOf("EXPLORER") } // "EXPLORER", "PROJECTS", "RECENTS", "GITHUB"
    var showProjectMenu by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var selectedParentPath by remember { mutableStateOf("") }
    var newFileNameInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.surface)
    ) {
        // VSC TOP ACTIVITY BAR TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(themeColors.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActivityTabButton(
                    icon = Icons.Default.Folder,
                    label = "Explorer",
                    isActive = activeSidebarTab == "EXPLORER",
                    themeColors = themeColors,
                    onClick = { activeSidebarTab = "EXPLORER" }
                )
                ActivityTabButton(
                    icon = Icons.Default.Workspaces,
                    label = "Projects",
                    isActive = activeSidebarTab == "PROJECTS",
                    themeColors = themeColors,
                    onClick = { activeSidebarTab = "PROJECTS" }
                )
                ActivityTabButton(
                    icon = Icons.Default.History,
                    label = "Recents",
                    isActive = activeSidebarTab == "RECENTS",
                    themeColors = themeColors,
                    onClick = { activeSidebarTab = "RECENTS" }
                )
                ActivityTabButton(
                    icon = Icons.Default.CloudDownload,
                    label = "GitHub",
                    isActive = activeSidebarTab == "GITHUB",
                    themeColors = themeColors,
                    onClick = { activeSidebarTab = "GITHUB" }
                )
            }

            // Quick actions menu button
            Box {
                IconButton(onClick = { showProjectMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Project Options", tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showProjectMenu, onDismissRequest = { showProjectMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Ubuntu Terminal", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onOpenTerminal()
                            showProjectMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Claude & Copilot AI", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onOpenAiAssistant()
                            showProjectMenu = false
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Open Folder (SAF)", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onOpenFolder()
                            showProjectMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Project...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onNewProject()
                            showProjectMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clone from GitHub...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onCloneGitHub()
                            showProjectMenu = false
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Export Project as ZIP", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onExportZip()
                            showProjectMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Folder", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            onExportFolder()
                            showProjectMenu = false
                        }
                    )
                }
            }
        }

        // TAB 1: EXPLORER VIEW
        if (activeSidebarTab == "EXPLORER") {
            // Project Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = (currentProject?.name ?: "WORKSPACE").uppercase(),
                        color = themeColors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentProject?.gitBranch != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColors.surfaceVariant)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = " ${currentProject.gitBranch}",
                                color = themeColors.textColor.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Explorer Toolbar Icons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = {
                            selectedParentPath = ""
                            newFileNameInput = ""
                            showCreateFileDialog = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = themeColors.textColor, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = {
                            selectedParentPath = ""
                            newFileNameInput = ""
                            showCreateFolderDialog = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = themeColors.textColor, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = onRefreshTree,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = themeColors.textColor, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Divider(color = themeColors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

            // File Tree List
            if (fileTree == null || fileTree.children.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("No files in workspace", color = themeColors.gutterTextColor, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary)
                        ) {
                            Text("Open Folder", color = themeColors.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    items(flattenNodes(fileTree.children)) { nodeInfo ->
                        FileTreeNodeRow(
                            node = nodeInfo.node,
                            depth = nodeInfo.depth,
                            isSelected = nodeInfo.node.path == selectedFilePath,
                            themeColors = themeColors,
                            onClick = {
                                if (nodeInfo.node.isDirectory) {
                                    nodeInfo.node.isExpanded = !nodeInfo.node.isExpanded
                                    onRefreshTree()
                                } else {
                                    onSelectFile(nodeInfo.node.path)
                                }
                            },
                            onDelete = { onDeleteItem(nodeInfo.node.path) }
                        )
                    }
                }
            }
        }

        // TAB 2: PROJECTS & WORKSPACES
        if (activeSidebarTab == "PROJECTS") {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                Text("RECENT WORKSPACES", color = themeColors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColors.textColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Folder", fontSize = 12.sp, color = themeColors.textColor)
                    }
                    Button(
                        onClick = onNewProject,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColors.surface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Project", fontSize = 12.sp, color = themeColors.surface, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allProjects) { project ->
                        val isCurrent = project.id == currentProject?.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) themeColors.primary.copy(alpha = 0.15f) else themeColors.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    if (isCurrent) themeColors.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectProject(project) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (project.isExternalFolder) Icons.Default.Folder else Icons.Default.Workspaces,
                                        contentDescription = null,
                                        tint = if (isCurrent) themeColors.primary else themeColors.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = project.name,
                                        color = if (isCurrent) themeColors.primary else themeColors.textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (project.gitRemoteUrl != null) {
                                    Text(
                                        text = project.gitRemoteUrl,
                                        color = themeColors.gutterTextColor,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB: RECENTS
        if (activeSidebarTab == "RECENTS") {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RECENTLY ACCESSED FILES",
                        color = themeColors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${recentDocuments.size} files",
                        color = themeColors.gutterTextColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (recentDocuments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = themeColors.gutterTextColor, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No recent files recorded yet", color = themeColors.gutterTextColor, fontSize = 12.sp)
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recentDocuments) { doc ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColors.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { onSelectDocument(doc) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = if (doc.fileType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description
                                    val color = if (doc.fileType == "PDF") Color(0xFFEF4444) else themeColors.primary
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            color = themeColors.textColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = doc.language.uppercase(),
                                                color = themeColors.primary,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = dateFormat.format(Date(doc.updatedAt)),
                                                color = themeColors.gutterTextColor,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: GITHUB INTEGRATION
        if (activeSidebarTab == "GITHUB") {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                Text("GITHUB INTEGRATION", color = themeColors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Clone Public Repository", fontWeight = FontWeight.Bold, color = themeColors.textColor, fontSize = 13.sp)
                        Text("Download any GitHub repository directly into a new project workspace.", color = themeColors.gutterTextColor, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCloneGitHub,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = themeColors.surface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clone from GitHub", color = themeColors.surface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Export Workspace", fontWeight = FontWeight.Bold, color = themeColors.textColor, fontSize = 13.sp)
                        Text("Package the active project into a ZIP archive or save to device storage.", color = themeColors.gutterTextColor, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onExportZip,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColors.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ZIP Archive", fontSize = 11.sp, color = themeColors.primary)
                            }
                            OutlinedButton(
                                onClick = onExportFolder,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColors.textColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("To Folder", fontSize = 11.sp, color = themeColors.textColor)
                            }
                        }
                    }
                }
            }
        }

        // Dialogs for New File / New Folder
        if (showCreateFileDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFileDialog = false },
                title = { Text("New File in Project", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        label = { Text("File Name (e.g. index.ts, styles.css)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            onCreateFile(selectedParentPath, newFileNameInput.trim())
                            showCreateFileDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("New Folder", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        label = { Text("Folder Name (e.g. components, utils)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            onCreateFolder(selectedParentPath, newFileNameInput.trim())
                            showCreateFolderDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun ActivityTabButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    themeColors: EditorThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) themeColors.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) themeColors.primary else themeColors.gutterTextColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isActive) themeColors.textColor else themeColors.gutterTextColor,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private data class NodeDepth(val node: ProjectFileNode, val depth: Int)

private fun flattenNodes(nodes: List<ProjectFileNode>, depth: Int = 0): List<NodeDepth> {
    val result = mutableListOf<NodeDepth>()
    for (node in nodes) {
        result.add(NodeDepth(node, depth))
        if (node.isDirectory && node.isExpanded && node.children.isNotEmpty()) {
            result.addAll(flattenNodes(node.children, depth + 1))
        }
    }
    return result
}

@Composable
private fun FileTreeNodeRow(
    node: ProjectFileNode,
    depth: Int,
    isSelected: Boolean,
    themeColors: EditorThemeColors,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(start = (depth * 14).dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand/Collapse caret for folders
        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = themeColors.gutterTextColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        // File/Folder Icon
        val (icon, color) = getFileIconAndColor(node, themeColors)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Filename
        Text(
            text = node.name,
            color = if (isSelected) themeColors.primary else themeColors.textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Delete / context action
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = themeColors.gutterTextColor.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp)) },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

private fun getFileIconAndColor(node: ProjectFileNode, themeColors: EditorThemeColors): Pair<ImageVector, Color> {
    if (node.isDirectory) {
        return Pair(Icons.Default.Folder, Color(0xFFEAB308))
    }
    return when (node.extension.lowercase()) {
        "ts", "tsx" -> Pair(Icons.Default.Code, Color(0xFF38BDF8))
        "js", "jsx", "mjs" -> Pair(Icons.Default.Javascript, Color(0xFFFACC15))
        "html", "htm" -> Pair(Icons.Default.Language, Color(0xFFF97316))
        "css", "scss" -> Pair(Icons.Default.Style, Color(0xFF06B6D4))
        "json" -> Pair(Icons.Default.DataObject, Color(0xFFA855F7))
        "md", "markdown" -> Pair(Icons.Default.Description, Color(0xFF4ADE80))
        "asm", "s" -> Pair(Icons.Default.Memory, Color(0xFFEC4899))
        "pdf" -> Pair(Icons.Default.PictureAsPdf, Color(0xFFEF4444))
        "py" -> Pair(Icons.Default.Terminal, Color(0xFF22C55E))
        "kt", "kts" -> Pair(Icons.Default.Code, Color(0xFF818CF8))
        "sql" -> Pair(Icons.Default.TableChart, Color(0xFF6366F1))
        "sh", "bash" -> Pair(Icons.Default.Terminal, Color(0xFF10B981))
        else -> Pair(Icons.Default.InsertDriveFile, themeColors.gutterTextColor)
    }
}
