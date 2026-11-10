package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.SleekIdeHeaderBar
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
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.syntax.EditorLanguage
import com.example.editor.execution.ExecutionResult
import com.example.ui.components.DocumentStatsDialog
import com.example.ui.components.ExecutionConsoleSheet
import com.example.ui.components.FileDrawerContent
import com.example.ui.components.FindReplaceBar
import com.example.ui.components.NewFileDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.CloneGitHubDialog
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.IntegratedTerminalSheet
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.components.VscProjectExplorer
import com.example.ui.screens.HtmlPreviewScreen
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.TextCodeEditorScreen
import com.example.ui.theme.ThemeRegistry
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyNavigationBarVisibility(true)

        setContent {
            val preferences by viewModel.preferences.collectAsState()
            val themeColors = remember(preferences.theme) {
                ThemeRegistry.getTheme(preferences.theme)
            }

            // Reactively update system navigation bar (III O <) visibility based on user preference
            LaunchedEffect(preferences.hideNavigationBar) {
                applyNavigationBarVisibility(preferences.hideNavigationBar)
            }

            OmniEditApp(
                viewModel = viewModel,
                themeColors = themeColors
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && viewModel.preferences.value.hideNavigationBar) {
            applyNavigationBarVisibility(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.preferences.value.hideNavigationBar) {
            applyNavigationBarVisibility(true)
        }
    }

    /**
     * Hides the system navigation bar ("III O <" 3-button bar or gesture pill) in immersive sticky mode.
     * When hidden, the entire bottom screen is available for IDE tools without obstruction.
     * Swiping up from the bottom edge transiently reveals the system bar when needed.
     */
    private fun applyNavigationBarVisibility(hide: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hide) {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniEditApp(
    viewModel: EditorViewModel,
    themeColors: com.example.ui.theme.EditorThemeColors
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val allDocs by viewModel.allDocuments.collectAsState()
    val currentDoc by viewModel.currentDocument.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val editorValue by viewModel.editorValue.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val completions by viewModel.completions.collectAsState()

    val isFindReplaceVisible by viewModel.isFindReplaceVisible.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    val isCaseSensitive by viewModel.isCaseSensitive.collectAsState()

    // PDF state
    val pdfPageBitmap by viewModel.pdfPageBitmap.collectAsState()
    val currentPdfPage by viewModel.currentPdfPage.collectAsState()
    val pdfPageCount by viewModel.pdfPageCount.collectAsState()
    val activePdfTool by viewModel.activePdfTool.collectAsState()
    val activeDrawColorHex by viewModel.activeDrawColorHex.collectAsState()
    val activeStrokeWidth by viewModel.activeStrokeWidth.collectAsState()
    val annotationsMap by viewModel.annotationsMap.collectAsState()
    val currentDrawingPoints by viewModel.currentDrawingPoints.collectAsState()

    // Dialogs
    val isStatsVisible by viewModel.isStatsVisible.collectAsState()
    val isThemeDialogVisible by viewModel.isThemeDialogVisible.collectAsState()
    val isNewFileDialogVisible by viewModel.isNewFileDialogVisible.collectAsState()
    val isHtmlPreviewVisible by viewModel.isHtmlPreviewVisible.collectAsState()
    val isSearchFilesDialogVisible by viewModel.isSearchFilesDialogVisible.collectAsState()
    val isExecutionConsoleVisible by viewModel.isExecutionConsoleVisible.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val executionResult by viewModel.executionResult.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val drawerFilter by viewModel.drawerFilter.collectAsState()
    val drawerSearch by viewModel.drawerSearch.collectAsState()

    // Projects & Workspaces
    val currentProject by viewModel.currentProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val projectFileTree by viewModel.projectFileTree.collectAsState()
    val activeProjectFiles by viewModel.activeProjectFiles.collectAsState()
    val currentProjectFilePath by viewModel.currentProjectFilePath.collectAsState()
    val isNewProjectDialogVisible by viewModel.isNewProjectDialogVisible.collectAsState()
    val isCloneGitHubDialogVisible by viewModel.isCloneGitHubDialogVisible.collectAsState()
    val isCloning by viewModel.isCloning.collectAsState()
    val cloneStatus by viewModel.cloneStatus.collectAsState()
    val isTerminalVisible by viewModel.isTerminalVisible.collectAsState()
    val isAiAssistantVisible by viewModel.isAiAssistantVisible.collectAsState()
    val recentDocsList by viewModel.recentDocsList.collectAsState()

    // SAF File & Folder Pickers
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri)
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDocumentToUri(uri)
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.openFolder(uri)
        }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportActiveProjectToZip(uri)
        }
    }

    val exportFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.exportActiveProjectToFolder(uri)
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = drawerState.isOpen || isHtmlPreviewVisible || isFindReplaceVisible || isSearchFilesDialogVisible || isExecutionConsoleVisible || isTerminalVisible || isAiAssistantVisible) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (isTerminalVisible) {
            viewModel.setTerminalVisible(false)
        } else if (isAiAssistantVisible) {
            viewModel.setAiAssistantVisible(false)
        } else if (isExecutionConsoleVisible) {
            viewModel.setExecutionConsoleVisible(false)
        } else if (isSearchFilesDialogVisible) {
            viewModel.setSearchFilesDialogVisible(false)
        } else if (isHtmlPreviewVisible) {
            viewModel.setHtmlPreviewVisible(false)
        } else if (isFindReplaceVisible) {
            viewModel.toggleFindReplace()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
            .onKeyEvent { keyEvent ->
                // Android TV & Hardware Keyboard Shortcuts: Ctrl+S, Ctrl+F, Ctrl+P, Ctrl+Z, Ctrl+N, Ctrl+R
                if (keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.S -> { viewModel.saveCurrentDocument(); true }
                        Key.F -> { viewModel.toggleFindReplace(); true }
                        Key.P -> { viewModel.setSearchFilesDialogVisible(true); true }
                        Key.Z -> { viewModel.undo(); true }
                        Key.Y -> { viewModel.redo(); true }
                        Key.N -> { viewModel.setNewFileDialogVisible(true); true }
                        Key.R -> { viewModel.runCurrentCode(); true }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        val isWideScreen = maxWidth > 840.dp // Tablet / Foldable expanded / TV

        val drawerContentComposable: @Composable () -> Unit = {
            VscProjectExplorer(
                currentProject = currentProject,
                allProjects = allProjects,
                fileTree = projectFileTree,
                selectedFilePath = currentProjectFilePath,
                themeColors = themeColors,
                recentDocuments = recentDocsList.ifEmpty { allDocs },
                onSelectFile = { relPath ->
                    viewModel.openProjectFile(relPath)
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                },
                onSelectDocument = { doc ->
                    viewModel.openDocument(doc)
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                },
                onCreateFile = { parentRelPath, fileName ->
                    viewModel.createProjectFile(parentRelPath, fileName)
                },
                onCreateFolder = { parentRelPath, folderName ->
                    viewModel.createProjectFolder(parentRelPath, folderName)
                },
                onDeleteItem = { relPath ->
                    viewModel.deleteProjectItem(relPath)
                },
                onSelectProject = { proj ->
                    viewModel.openProject(proj)
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                },
                onOpenFolder = {
                    openFolderLauncher.launch(null)
                },
                onNewProject = {
                    viewModel.setNewProjectDialogVisible(true)
                },
                onCloneGitHub = {
                    viewModel.setCloneGitHubDialogVisible(true)
                },
                onExportZip = {
                    val defaultZipName = "${currentProject?.name ?: "project"}.zip"
                    exportZipLauncher.launch(defaultZipName)
                },
                onExportFolder = {
                    exportFolderLauncher.launch(null)
                },
                onRefreshTree = {
                    viewModel.refreshProjectTree()
                },
                onOpenTerminal = {
                    viewModel.setTerminalVisible(true)
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                },
                onOpenAiAssistant = {
                    viewModel.setAiAssistantVisible(true)
                    if (!isWideScreen) {
                        coroutineScope.launch { drawerState.close() }
                    }
                }
            )
        }

        if (isWideScreen) {
            // Dual-pane layout on Tablets, Large Screens, and TVs
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(themeColors.surface)
                ) {
                    drawerContentComposable()
                }

                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = themeColors.surfaceVariant
                )

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EditorScaffoldContent(
                        viewModel = viewModel,
                        themeColors = themeColors,
                        currentDoc = currentDoc,
                        openTabs = openTabs,
                        editorValue = editorValue,
                        isModified = isModified,
                        preferences = preferences,
                        completions = completions,
                        isFindReplaceVisible = isFindReplaceVisible,
                        searchQuery = searchQuery,
                        replaceQuery = replaceQuery,
                        searchMatches = searchMatches,
                        currentMatchIndex = currentMatchIndex,
                        isCaseSensitive = isCaseSensitive,
                        pdfPageBitmap = pdfPageBitmap,
                        currentPdfPage = currentPdfPage,
                        pdfPageCount = pdfPageCount,
                        activePdfTool = activePdfTool,
                        activeDrawColorHex = activeDrawColorHex,
                        activeStrokeWidth = activeStrokeWidth,
                        annotationsMap = annotationsMap,
                        currentDrawingPoints = currentDrawingPoints,
                        isHtmlPreviewVisible = isHtmlPreviewVisible,
                        isExecutionConsoleVisible = isExecutionConsoleVisible,
                        isExecuting = isExecuting,
                        executionResult = executionResult,
                        activeProjectFiles = activeProjectFiles,
                        isTerminalVisible = isTerminalVisible,
                        isAiAssistantVisible = isAiAssistantVisible,
                        recentDocsList = recentDocsList,
                        currentProject = currentProject,
                        snackbarHostState = snackbarHostState,
                        onToggleDrawer = { /* wide screen, drawer is fixed */ },
                        onExportDocument = {
                            val defaultName = currentDoc?.title ?: "document.txt"
                            exportDocumentLauncher.launch(defaultName)
                        },
                        isWideScreen = true
                    )
                }
            }
        } else {
            // Single-pane with navigation drawer on Phones
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = themeColors.surface,
                        modifier = Modifier.width(320.dp)
                    ) {
                        drawerContentComposable()
                    }
                }
            ) {
                EditorScaffoldContent(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    currentDoc = currentDoc,
                    openTabs = openTabs,
                    editorValue = editorValue,
                    isModified = isModified,
                    preferences = preferences,
                    completions = completions,
                    isFindReplaceVisible = isFindReplaceVisible,
                    searchQuery = searchQuery,
                    replaceQuery = replaceQuery,
                    searchMatches = searchMatches,
                    currentMatchIndex = currentMatchIndex,
                    isCaseSensitive = isCaseSensitive,
                    pdfPageBitmap = pdfPageBitmap,
                    currentPdfPage = currentPdfPage,
                    pdfPageCount = pdfPageCount,
                    activePdfTool = activePdfTool,
                    activeDrawColorHex = activeDrawColorHex,
                    activeStrokeWidth = activeStrokeWidth,
                    annotationsMap = annotationsMap,
                    currentDrawingPoints = currentDrawingPoints,
                    isHtmlPreviewVisible = isHtmlPreviewVisible,
                    isExecutionConsoleVisible = isExecutionConsoleVisible,
                    isExecuting = isExecuting,
                    executionResult = executionResult,
                    activeProjectFiles = activeProjectFiles,
                    isTerminalVisible = isTerminalVisible,
                    isAiAssistantVisible = isAiAssistantVisible,
                    recentDocsList = recentDocsList,
                    currentProject = currentProject,
                    snackbarHostState = snackbarHostState,
                    onToggleDrawer = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onExportDocument = {
                        val defaultName = currentDoc?.title ?: "document.txt"
                        exportDocumentLauncher.launch(defaultName)
                    },
                    isWideScreen = false
                )
            }
        }

        // Dialogs
        if (isThemeDialogVisible) {
            ThemeSelectorDialog(
                preferences = preferences,
                currentThemeColors = themeColors,
                onSelectTheme = { theme -> viewModel.setTheme(theme) },
                onFontSizeDelta = { delta -> viewModel.updateFontSize(delta) },
                onToggleLineNumbers = { viewModel.toggleLineNumbers() },
                onToggleWordWrap = { viewModel.toggleWordWrap() },
                onToggleAutoSave = { viewModel.toggleAutoSave() },
                onToggleHideNavigationBar = { viewModel.toggleHideNavigationBar() },
                onDismiss = { viewModel.setThemeDialogVisible(false) }
            )
        }

        if (isStatsVisible) {
            DocumentStatsDialog(
                document = currentDoc,
                stats = viewModel.getDocumentStats(),
                themeColors = themeColors,
                onDismiss = { viewModel.setStatsVisible(false) }
            )
        }

        if (isNewFileDialogVisible) {
            NewFileDialog(
                themeColors = themeColors,
                onCreateFile = { title, fileType, lang, starter ->
                    viewModel.createNewDocument(title, fileType, lang, starter)
                },
                onDismiss = { viewModel.setNewFileDialogVisible(false) }
            )
        }

        if (isNewProjectDialogVisible) {
            NewProjectDialog(
                themeColors = themeColors,
                onCreateProject = { name, template ->
                    viewModel.createNewProject(name, template)
                },
                onDismiss = { viewModel.setNewProjectDialogVisible(false) }
            )
        }

        if (isCloneGitHubDialogVisible) {
            CloneGitHubDialog(
                themeColors = themeColors,
                isCloning = isCloning,
                cloneStatus = cloneStatus,
                onClone = { url, token ->
                    viewModel.cloneGitHub(url, token)
                },
                onDismiss = { viewModel.setCloneGitHubDialogVisible(false) }
            )
        }

        if (isSearchFilesDialogVisible) {
            com.example.ui.components.FileSearchDialog(
                allDocuments = allDocs,
                themeColors = themeColors,
                onSelectDocument = { doc, targetLine ->
                    viewModel.openDocumentAtLine(doc, targetLine)
                },
                onDismiss = { viewModel.setSearchFilesDialogVisible(false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffoldContent(
    viewModel: EditorViewModel,
    themeColors: com.example.ui.theme.EditorThemeColors,
    currentDoc: com.example.data.model.DocumentEntity?,
    openTabs: List<com.example.data.model.DocumentEntity>,
    editorValue: androidx.compose.ui.text.input.TextFieldValue,
    isModified: Boolean,
    preferences: com.example.data.model.EditorPreferences,
    completions: List<com.example.editor.syntax.CompletionItem>,
    isFindReplaceVisible: Boolean,
    searchQuery: String,
    replaceQuery: String,
    searchMatches: List<IntRange>,
    currentMatchIndex: Int,
    isCaseSensitive: Boolean,
    pdfPageBitmap: android.graphics.Bitmap?,
    currentPdfPage: Int,
    pdfPageCount: Int,
    activePdfTool: com.example.ui.viewmodel.PdfTool,
    activeDrawColorHex: String,
    activeStrokeWidth: Float,
    annotationsMap: Map<Int, com.example.data.model.PageAnnotations>,
    currentDrawingPoints: List<com.example.data.model.AnnotationPoint>,
    isHtmlPreviewVisible: Boolean,
    isExecutionConsoleVisible: Boolean,
    isExecuting: Boolean,
    executionResult: ExecutionResult?,
    activeProjectFiles: Map<String, String>,
    isTerminalVisible: Boolean,
    isAiAssistantVisible: Boolean,
    recentDocsList: List<com.example.data.model.DocumentEntity>,
    currentProject: com.example.data.model.Project?,
    snackbarHostState: SnackbarHostState,
    onToggleDrawer: () -> Unit,
    onExportDocument: () -> Unit,
    isWideScreen: Boolean
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sleek, compact IDE header bar (replaces default Android TopAppBar)
            SleekIdeHeaderBar(
                currentDoc = currentDoc,
                isModified = isModified,
                isWideScreen = isWideScreen,
                isExecuting = isExecuting,
                isHtmlPreviewVisible = isHtmlPreviewVisible,
                themeColors = themeColors,
                onToggleDrawer = onToggleDrawer,
                onSave = { viewModel.saveCurrentDocument() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleFindReplace = { viewModel.toggleFindReplace() },
                onSearchFiles = { viewModel.setSearchFilesDialogVisible(true) },
                onRunCode = { viewModel.runCurrentCode() },
                onTogglePreview = { viewModel.setHtmlPreviewVisible(!isHtmlPreviewVisible) },
                onOpenThemePicker = { viewModel.setThemeDialogVisible(true) },
                onOpenStats = { viewModel.setStatsVisible(true) },
                onExportPdf = { viewModel.exportCurrentTextToPdf() },
                onExportDocument = onExportDocument,
                onSetLanguage = { langId -> viewModel.setLanguage(langId) },
                onOpenTerminal = { viewModel.setTerminalVisible(!isTerminalVisible) },
                onOpenAiAssistant = { viewModel.setAiAssistantVisible(!isAiAssistantVisible) }
            )
            // Find & Replace Bar
            if (isFindReplaceVisible && currentDoc?.fileType != "PDF") {
                FindReplaceBar(
                    searchQuery = searchQuery,
                    replaceQuery = replaceQuery,
                    matchCount = searchMatches.size,
                    currentMatchIndex = currentMatchIndex,
                    isCaseSensitive = isCaseSensitive,
                    themeColors = themeColors,
                    onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                    onReplaceQueryChange = { r -> viewModel.setReplaceQuery(r) },
                    onFindNext = { viewModel.findNext() },
                    onFindPrev = { viewModel.findPrevious() },
                    onReplaceCurrent = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onToggleCaseSensitive = { viewModel.toggleCaseSensitive() },
                    onClose = { viewModel.toggleFindReplace() }
                )
            }

            // Main Editor Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isHtmlPreviewVisible && currentDoc != null) {
                    val isMarkdown = currentDoc.language == "markdown" ||
                            currentDoc.title.endsWith(".md", ignoreCase = true) ||
                            currentDoc.title.endsWith(".markdown", ignoreCase = true)
                    HtmlPreviewScreen(
                        title = currentDoc.title,
                        content = editorValue.text,
                        isMarkdown = isMarkdown,
                        themeColors = themeColors,
                        projectFiles = activeProjectFiles,
                        onClose = { viewModel.setHtmlPreviewVisible(false) }
                    )
                } else if (currentDoc?.fileType == "PDF") {
                    PdfEditorScreen(
                        pageBitmap = pdfPageBitmap,
                        currentPage = currentPdfPage,
                        pageCount = pdfPageCount,
                        activeTool = activePdfTool,
                        activeColorHex = activeDrawColorHex,
                        activeStrokeWidth = activeStrokeWidth,
                        annotationsMap = annotationsMap,
                        currentDrawingPoints = currentDrawingPoints,
                        themeColors = themeColors,
                        onNextPage = { viewModel.nextPdfPage() },
                        onPrevPage = { viewModel.prevPdfPage() },
                        onSetTool = { tool -> viewModel.setPdfTool(tool) },
                        onSetColor = { hex -> viewModel.setDrawColor(hex) },
                        onAddPoint = { x, y -> viewModel.addPointToCurrentDrawing(x, y) },
                        onFinishDrawing = { viewModel.finishCurrentDrawingPath() },
                        onAddTextNote = { text, x, y -> viewModel.addTextAnnotation(text, x, y) },
                        onClearPage = { viewModel.clearCurrentPageAnnotations() },
                        onExportPdf = { viewModel.exportAnnotatedPdfToStorage() }
                    )
                } else if (currentDoc != null) {
                    TextCodeEditorScreen(
                        currentDocument = currentDoc,
                        openTabs = openTabs,
                        editorValue = editorValue,
                        preferences = preferences,
                        themeColors = themeColors,
                        completions = completions,
                        onEditorValueChanged = { viewModel.onEditorValueChanged(it) },
                        onTabSelected = { viewModel.openDocument(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onApplyCompletion = { viewModel.applyCompletion(it) },
                        onInsertSymbol = { viewModel.insertSymbol(it) },
                        onInsertTab = { viewModel.insertTab() },
                        onToggleComment = { viewModel.toggleComment() },
                        onEnterKey = { viewModel.handleEnterKey() },
                        searchMatches = searchMatches,
                        currentMatchIndex = currentMatchIndex
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No document open. Open the drawer to select or create a file.", color = themeColors.gutterTextColor)
                    }
                }
            }

            // Interactive Bottom Console & Disassembly / Registers / SQL Sheet
            if (isExecutionConsoleVisible && currentDoc?.fileType != "PDF") {
                val currentLanguage = currentDoc?.let { doc ->
                    val ext = doc.title.substringAfterLast('.', "")
                    EditorLanguage.fromId(doc.language).takeIf { it != EditorLanguage.PLAIN_TEXT }
                        ?: EditorLanguage.fromExtension(ext)
                } ?: EditorLanguage.PLAIN_TEXT

                ExecutionConsoleSheet(
                    result = executionResult,
                    isRunning = isExecuting,
                    language = currentLanguage,
                    themeColors = themeColors,
                    onRerun = { viewModel.runCurrentCode() },
                    onClear = { viewModel.clearExecutionResult() },
                    onClose = { viewModel.setExecutionConsoleVisible(false) }
                )
            }

            // Integrated Ubuntu 24.04 Termux Shell Terminal
            if (isTerminalVisible) {
                IntegratedTerminalSheet(
                    terminal = viewModel.terminalEmulator,
                    themeColors = themeColors,
                    onClose = { viewModel.setTerminalVisible(false) }
                )
            }

            // Claude 3.5 Sonnet & GitHub Copilot Workspace Assistant
            if (isAiAssistantVisible) {
                AiAssistantSheet(
                    aiManager = viewModel.aiCopilotManager,
                    connectorServer = viewModel.claudeConnectorServer,
                    currentDoc = currentDoc,
                    editorText = editorValue.text,
                    currentProject = currentProject,
                    recentDocs = recentDocsList,
                    themeColors = themeColors,
                    onInsertCode = { snippet -> viewModel.insertSymbol(snippet) },
                    onClose = { viewModel.setAiAssistantVisible(false) }
                )
            }
        }
    }
}
