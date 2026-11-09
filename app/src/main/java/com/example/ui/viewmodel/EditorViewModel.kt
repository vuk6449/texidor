package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationPoint
import com.example.data.model.AppTheme
import com.example.data.model.DocumentEntity
import com.example.data.model.DrawPathItem
import com.example.data.model.EditorPreferences
import com.example.data.model.PageAnnotations
import com.example.data.model.TextAnnotationItem
import com.example.data.repository.DocumentRepository
import com.example.editor.pdf.PdfGenerator
import com.example.editor.pdf.PdfViewerManager
import com.example.editor.execution.ExecutionResult
import com.example.editor.execution.LanguageExecutor
import com.example.data.model.Project
import com.example.data.model.ProjectFileNode
import com.example.data.model.ProjectTemplateType
import com.example.data.repository.ProjectManager
import com.example.data.repository.GitHubManager
import com.example.editor.ai.AiCopilotManager
import com.example.editor.connector.ClaudeConnectorServer
import com.example.editor.terminal.UbuntuShellEmulator
import com.example.editor.ts.TypeScriptCompiler
import com.example.editor.syntax.CompletionItem
import com.example.editor.syntax.EditorLanguage
import com.example.editor.syntax.IntelliSenseEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class PdfTool {
    NONE,
    PEN,
    HIGHLIGHTER,
    TEXT_NOTE,
    ERASER
}

data class UiStats(
    val words: Int,
    val characters: Int,
    val charactersNoSpaces: Int,
    val lines: Int,
    val readingTimeMinutes: Int,
    val bytes: Int
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    private var pdfViewerManager: PdfViewerManager? = null

    val allDocuments: StateFlow<List<DocumentEntity>>

    private val _currentDocument = MutableStateFlow<DocumentEntity?>(null)
    val currentDocument: StateFlow<DocumentEntity?> = _currentDocument.asStateFlow()

    private val _openTabs = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val openTabs: StateFlow<List<DocumentEntity>> = _openTabs.asStateFlow()

    private val _editorValue = MutableStateFlow(TextFieldValue(""))
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _preferences = MutableStateFlow(EditorPreferences())
    val preferences: StateFlow<EditorPreferences> = _preferences.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastSnapshotTime = 0L

    // Search & Replace
    private val _isFindReplaceVisible = MutableStateFlow(false)
    val isFindReplaceVisible: StateFlow<Boolean> = _isFindReplaceVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _isCaseSensitive = MutableStateFlow(false)
    val isCaseSensitive: StateFlow<Boolean> = _isCaseSensitive.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<IntRange>>(emptyList())
    val searchMatches: StateFlow<List<IntRange>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    // IntelliSense
    private val _completions = MutableStateFlow<List<CompletionItem>>(emptyList())
    val completions: StateFlow<List<CompletionItem>> = _completions.asStateFlow()

    private var intelliSenseJob: Job? = null
    private var autoSaveJob: Job? = null

    // PDF Mode State
    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount: StateFlow<Int> = _pdfPageCount.asStateFlow()

    private val _currentPdfPage = MutableStateFlow(0)
    val currentPdfPage: StateFlow<Int> = _currentPdfPage.asStateFlow()

    private val _pdfPageBitmap = MutableStateFlow<Bitmap?>(null)
    val pdfPageBitmap: StateFlow<Bitmap?> = _pdfPageBitmap.asStateFlow()

    private val _pdfZoom = MutableStateFlow(1.0f)
    val pdfZoom: StateFlow<Float> = _pdfZoom.asStateFlow()

    private val _activePdfTool = MutableStateFlow(PdfTool.NONE)
    val activePdfTool: StateFlow<PdfTool> = _activePdfTool.asStateFlow()

    private val _activeDrawColorHex = MutableStateFlow("#EF4444")
    val activeDrawColorHex: StateFlow<String> = _activeDrawColorHex.asStateFlow()

    private val _activeStrokeWidth = MutableStateFlow(6f)
    val activeStrokeWidth: StateFlow<Float> = _activeStrokeWidth.asStateFlow()

    private val _annotationsMap = MutableStateFlow<Map<Int, PageAnnotations>>(emptyMap())
    val annotationsMap: StateFlow<Map<Int, PageAnnotations>> = _annotationsMap.asStateFlow()

    private val _currentDrawingPoints = MutableStateFlow<List<AnnotationPoint>>(emptyList())
    val currentDrawingPoints: StateFlow<List<AnnotationPoint>> = _currentDrawingPoints.asStateFlow()

    // Dialogs & Sheets
    private val _isStatsVisible = MutableStateFlow(false)
    val isStatsVisible: StateFlow<Boolean> = _isStatsVisible.asStateFlow()

    private val _isThemeDialogVisible = MutableStateFlow(false)
    val isThemeDialogVisible: StateFlow<Boolean> = _isThemeDialogVisible.asStateFlow()

    private val _isNewFileDialogVisible = MutableStateFlow(false)
    val isNewFileDialogVisible: StateFlow<Boolean> = _isNewFileDialogVisible.asStateFlow()

    private val _isHtmlPreviewVisible = MutableStateFlow(false)
    val isHtmlPreviewVisible: StateFlow<Boolean> = _isHtmlPreviewVisible.asStateFlow()

    private val _isSearchFilesDialogVisible = MutableStateFlow(false)
    val isSearchFilesDialogVisible: StateFlow<Boolean> = _isSearchFilesDialogVisible.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _drawerFilter = MutableStateFlow("ALL") // "ALL", "TXT", "CODE", "PDF", "FAV"
    val drawerFilter: StateFlow<String> = _drawerFilter.asStateFlow()

    private val _drawerSearch = MutableStateFlow("")
    val drawerSearch: StateFlow<String> = _drawerSearch.asStateFlow()

    // Code Execution Console & Runner
    private val _executionResult = MutableStateFlow<ExecutionResult?>(null)
    val executionResult: StateFlow<ExecutionResult?> = _executionResult.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isExecutionConsoleVisible = MutableStateFlow(false)
    val isExecutionConsoleVisible: StateFlow<Boolean> = _isExecutionConsoleVisible.asStateFlow()

    // Projects & Workspaces
    val projectManager: ProjectManager = ProjectManager(application)
    val gitHubManager: GitHubManager = GitHubManager(projectManager)

    private val _allProjects = MutableStateFlow<List<Project>>(emptyList())
    val allProjects: StateFlow<List<Project>> = _allProjects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _projectFileTree = MutableStateFlow<ProjectFileNode?>(null)
    val projectFileTree: StateFlow<ProjectFileNode?> = _projectFileTree.asStateFlow()

    private val _activeProjectFiles = MutableStateFlow<Map<String, String>>(emptyMap())
    val activeProjectFiles: StateFlow<Map<String, String>> = _activeProjectFiles.asStateFlow()

    private val _currentProjectFilePath = MutableStateFlow<String?>(null)
    val currentProjectFilePath: StateFlow<String?> = _currentProjectFilePath.asStateFlow()

    private val _isNewProjectDialogVisible = MutableStateFlow(false)
    val isNewProjectDialogVisible: StateFlow<Boolean> = _isNewProjectDialogVisible.asStateFlow()

    private val _isCloneGitHubDialogVisible = MutableStateFlow(false)
    val isCloneGitHubDialogVisible: StateFlow<Boolean> = _isCloneGitHubDialogVisible.asStateFlow()

    private val _isCloning = MutableStateFlow(false)
    val isCloning: StateFlow<Boolean> = _isCloning.asStateFlow()

    private val _cloneStatus = MutableStateFlow<String?>(null)
    val cloneStatus: StateFlow<String?> = _cloneStatus.asStateFlow()

    private val _tsTranspileStatus = MutableStateFlow<String?>(null)
    val tsTranspileStatus: StateFlow<String?> = _tsTranspileStatus.asStateFlow()

    // Terminal, AI Assistant & Claude MCP Connector
    val terminalEmulator = UbuntuShellEmulator(application)
    val aiCopilotManager = AiCopilotManager()
    lateinit var claudeConnectorServer: ClaudeConnectorServer

    private val _isTerminalVisible = MutableStateFlow(false)
    val isTerminalVisible: StateFlow<Boolean> = _isTerminalVisible.asStateFlow()

    private val _isAiAssistantVisible = MutableStateFlow(false)
    val isAiAssistantVisible: StateFlow<Boolean> = _isAiAssistantVisible.asStateFlow()

    private val _recentDocsList = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val recentDocsList: StateFlow<List<DocumentEntity>> = _recentDocsList.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DocumentRepository(db.documentDao(), application)

        allDocuments = repository.allDocuments.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Initialize Claude Custom Connector Server (MCP)
        claudeConnectorServer = ClaudeConnectorServer(
            context = application,
            terminalEmulator = terminalEmulator,
            getActiveDocument = { _currentDocument.value },
            getAllDocuments = { allDocuments.value },
            getRecentDocuments = { _recentDocsList.value },
            getActiveProject = { _currentProject.value },
            getAllProjects = { _allProjects.value },
            onSaveDocument = { title, content ->
                viewModelScope.launch {
                    val existing = allDocuments.value.find { it.title.equals(title, ignoreCase = true) }
                    if (existing != null) {
                        repository.saveDocument(existing.copy(content = content, updatedAt = System.currentTimeMillis()))
                        if (_currentDocument.value?.id == existing.id) {
                            _editorValue.value = TextFieldValue(content)
                        }
                    } else {
                        createNewDocument(title = title, fileType = "TEXT", language = "plaintext", starterContent = content)
                    }
                }
            }
        )
        claudeConnectorServer.start(8080)

        viewModelScope.launch {
            repository.initSamplePdfIfMissing()
        }

        // Open initial document when list loads
        viewModelScope.launch {
            allDocuments.collect { docs ->
                if (_currentDocument.value == null && docs.isNotEmpty()) {
                    openDocument(docs.first())
                }
            }
        }

        // Background periodic auto-save loop
        viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_preferences.value.autoSaveEnabled && _isModified.value && _currentDocument.value != null) {
                    saveCurrentDocument(isSilent = true)
                }
            }
        }

        // Initialize projects list or sample
        viewModelScope.launch {
            loadProjectsList()
        }
    }

    fun openDocument(doc: DocumentEntity) {
        // If switching from modified document, auto-save first
        if (_isModified.value) {
            saveCurrentDocument()
        }

        _currentDocument.value = doc

        // Track recently accessed documents
        val recents = _recentDocsList.value.toMutableList()
        recents.removeAll { it.id == doc.id || it.title == doc.title }
        recents.add(0, doc)
        _recentDocsList.value = recents.take(25)

        // Sync terminal working directory to document or project directory
        if (doc.localFilePath != null) {
            val f = File(doc.localFilePath)
            if (f.parentFile?.exists() == true) {
                terminalEmulator.setWorkingDirectory(f.parentFile!!)
            }
        } else if (_currentProject.value != null && !_currentProject.value!!.isExternalFolder) {
            terminalEmulator.setWorkingDirectory(File(_currentProject.value!!.rootPath))
        }

        // Add to tabs if not already present
        val currentTabs = _openTabs.value.toMutableList()
        if (currentTabs.none { it.id == doc.id }) {
            currentTabs.add(doc)
            _openTabs.value = currentTabs
        }

        if (doc.fileType == "PDF") {
            loadPdfDocument(doc)
        } else {
            closePdfRenderer()
            _editorValue.value = TextFieldValue(doc.content, TextRange(0))
            _isModified.value = false
            undoStack.clear()
            redoStack.clear()
            undoStack.add(_editorValue.value)
            updateCompletions()
        }
    }

    fun closeTab(docId: Long) {
        val currentTabs = _openTabs.value.toMutableList()
        val index = currentTabs.indexOfFirst { it.id == docId }
        if (index != -1) {
            currentTabs.removeAt(index)
            _openTabs.value = currentTabs
            if (_currentDocument.value?.id == docId) {
                if (currentTabs.isNotEmpty()) {
                    val nextDoc = currentTabs.getOrElse(index.coerceAtMost(currentTabs.size - 1)) { currentTabs.first() }
                    openDocument(nextDoc)
                } else {
                    _currentDocument.value = null
                    _editorValue.value = TextFieldValue("")
                }
            }
        }
    }

    fun onEditorValueChanged(newValue: TextFieldValue) {
        val previous = _editorValue.value
        _editorValue.value = newValue
        _isModified.value = true

        // Push to undo stack with 600ms debounce or major structural change
        val now = System.currentTimeMillis()
        if (now - lastSnapshotTime > 600L || Math.abs(newValue.text.length - previous.text.length) > 5) {
            undoStack.add(newValue)
            if (undoStack.size > 100) undoStack.removeAt(0)
            redoStack.clear()
            lastSnapshotTime = now
        }

        // Trigger IntelliSense completions
        updateCompletions()
        checkTypeScriptTranspile()

        // Trigger auto-save debounce (2.5s) if enabled
        if (_preferences.value.autoSaveEnabled) {
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch {
                delay(2500)
                if (_preferences.value.autoSaveEnabled && _isModified.value) {
                    saveCurrentDocument(isSilent = true)
                }
            }
        }
    }

    private fun updateCompletions() {
        intelliSenseJob?.cancel()
        intelliSenseJob = viewModelScope.launch {
            delay(150)
            val doc = _currentDocument.value ?: return@launch
            if (doc.fileType != "CODE") {
                _completions.value = emptyList()
                return@launch
            }
            val lang = EditorLanguage.fromId(doc.language)
            val currentVal = _editorValue.value
            val suggestions = IntelliSenseEngine.getCompletions(
                code = currentVal.text,
                cursorIndex = currentVal.selection.end,
                language = lang
            )
            _completions.value = suggestions
        }
    }

    fun applyCompletion(item: CompletionItem) {
        val updated = IntelliSenseEngine.applyCompletion(_editorValue.value, item)
        _editorValue.value = updated
        _isModified.value = true
        undoStack.add(updated)
        redoStack.clear()
        updateCompletions()
    }

    fun insertSymbol(symbol: String) {
        val current = _editorValue.value
        val cursor = current.selection.start
        val newText = current.text.substring(0, cursor) + symbol + current.text.substring(current.selection.end)
        val updated = current.copy(text = newText, selection = TextRange(cursor + symbol.length))
        _editorValue.value = updated
        _isModified.value = true
        undoStack.add(updated)
        redoStack.clear()
        updateCompletions()
    }

    fun insertTab() {
        val spaces = " ".repeat(_preferences.value.tabSpaces)
        insertSymbol(spaces)
    }

    fun handleEnterKey(): Boolean {
        if (!_preferences.value.autoIndent) return false
        val updated = IntelliSenseEngine.handleEnterSmartIndent(_editorValue.value, _preferences.value.tabSpaces)
        _editorValue.value = updated
        _isModified.value = true
        undoStack.add(updated)
        redoStack.clear()
        updateCompletions()
        return true
    }

    fun toggleComment() {
        val doc = _currentDocument.value ?: return
        val lang = EditorLanguage.fromId(doc.language)
        val prefix = lang.commentPrefix + " "
        val current = _editorValue.value
        val text = current.text
        val cursor = current.selection.start

        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val newLine = if (line.trimStart().startsWith(lang.commentPrefix)) {
            val idx = line.indexOf(lang.commentPrefix)
            val removeCount = if (line.length > idx + prefix.length && line.substring(idx, idx + prefix.length) == prefix) {
                prefix.length
            } else {
                lang.commentPrefix.length
            }
            line.removeRange(idx, idx + removeCount)
        } else {
            val leading = line.takeWhile { it == ' ' || it == '\t' }
            leading + prefix + line.drop(leading.length)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        _editorValue.value = current.copy(text = newText)
        _isModified.value = true
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeLast()
            redoStack.add(current)
            val previous = undoStack.last()
            _editorValue.value = previous
            _isModified.value = true
            updateCompletions()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.add(next)
            _editorValue.value = next
            _isModified.value = true
            updateCompletions()
        }
    }

    fun saveCurrentDocument(isSilent: Boolean = false) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            if (doc.fileType == "PDF") {
                // Save serialized annotations
                val json = serializeAnnotations(_annotationsMap.value)
                val updated = doc.copy(annotationsJson = json, updatedAt = System.currentTimeMillis())
                repository.saveDocument(updated)
                _currentDocument.value = updated
                _isModified.value = false
                if (!isSilent) showSnackbar("PDF annotations saved locally")
            } else {
                val updated = doc.copy(
                    content = _editorValue.value.text,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveDocument(updated)
                _currentDocument.value = updated
                _isModified.value = false

                // If this document belongs to the active project workspace, save to project storage
                val proj = _currentProject.value
                val relPath = _currentProjectFilePath.value
                if (proj != null && relPath != null) {
                    projectManager.writeFile(proj, relPath, _editorValue.value.text)
                    refreshProjectTree()
                }

                if (!isSilent) showSnackbar("Saved: ${updated.title}")
            }
        }
    }

    fun createNewDocument(title: String, fileType: String, language: String, starterContent: String = "") {
        viewModelScope.launch {
            val effectiveTitle = if (title.isBlank()) {
                val ext = EditorLanguage.fromId(language).extensions.firstOrNull() ?: "txt"
                "Untitled_${System.currentTimeMillis() % 1000}.$ext"
            } else {
                title
            }

            val doc = DocumentEntity(
                title = effectiveTitle,
                content = starterContent,
                fileType = fileType,
                language = language,
                updatedAt = System.currentTimeMillis()
            )
            val id = repository.saveDocument(doc)
            val created = doc.copy(id = id)
            openDocument(created)
            _isNewFileDialogVisible.value = false
            showSnackbar("Created $effectiveTitle")
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            closeTab(id)
            repository.deleteDocument(id)
            showSnackbar("Document deleted")
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            val doc = _currentDocument.value
            val newFav = if (doc?.id == id) !doc.isFavorite else true
            repository.toggleFavorite(id, newFav)
            if (doc?.id == id) {
                _currentDocument.value = doc.copy(isFavorite = newFav)
            }
        }
    }

    // Search and Replace
    fun toggleFindReplace() {
        _isFindReplaceVisible.value = !_isFindReplaceVisible.value
        if (_isFindReplaceVisible.value) {
            updateSearchMatches()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateSearchMatches()
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun toggleCaseSensitive() {
        _isCaseSensitive.value = !_isCaseSensitive.value
        updateSearchMatches()
    }

    private fun updateSearchMatches() {
        val query = _searchQuery.value
        if (query.isEmpty()) {
            _searchMatches.value = emptyList()
            _currentMatchIndex.value = 0
            return
        }

        val text = _editorValue.value.text
        val matches = mutableListOf<IntRange>()
        val ignoreCase = !_isCaseSensitive.value
        var index = text.indexOf(query, 0, ignoreCase)
        while (index >= 0) {
            matches.add(index until (index + query.length))
            index = text.indexOf(query, index + query.length.coerceAtLeast(1), ignoreCase)
        }

        _searchMatches.value = matches
        _currentMatchIndex.value = if (matches.isNotEmpty()) 0 else 0
        if (matches.isNotEmpty()) {
            highlightMatch(0)
        }
    }

    fun findNext() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val nextIdx = (_currentMatchIndex.value + 1) % matches.size
        _currentMatchIndex.value = nextIdx
        highlightMatch(nextIdx)
    }

    fun findPrevious() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val prevIdx = if (_currentMatchIndex.value - 1 < 0) matches.size - 1 else _currentMatchIndex.value - 1
        _currentMatchIndex.value = prevIdx
        highlightMatch(prevIdx)
    }

    private fun highlightMatch(matchIndex: Int) {
        val matches = _searchMatches.value
        if (matchIndex in matches.indices) {
            val range = matches[matchIndex]
            _editorValue.value = _editorValue.value.copy(
                selection = TextRange(range.first, range.last + 1)
            )
        }
    }

    fun replaceCurrent() {
        val matches = _searchMatches.value
        val idx = _currentMatchIndex.value
        if (matches.isEmpty() || idx !in matches.indices) return

        val range = matches[idx]
        val text = _editorValue.value.text
        val replaceWith = _replaceQuery.value
        val newText = text.substring(0, range.first) + replaceWith + text.substring(range.last + 1)

        _editorValue.value = TextFieldValue(newText, TextRange(range.first + replaceWith.length))
        _isModified.value = true
        updateSearchMatches()
    }

    fun replaceAll() {
        val query = _searchQuery.value
        if (query.isEmpty()) return
        val text = _editorValue.value.text
        val replaceWith = _replaceQuery.value
        val ignoreCase = !_isCaseSensitive.value

        val newText = if (ignoreCase) {
            text.replace(Regex(Regex.escape(query), RegexOption.IGNORE_CASE), replaceWith)
        } else {
            text.replace(query, replaceWith)
        }

        _editorValue.value = TextFieldValue(newText, TextRange(0))
        _isModified.value = true
        updateSearchMatches()
        showSnackbar("Replaced all matches")
    }

    // PDF Controls
    private fun loadPdfDocument(doc: DocumentEntity) {
        closePdfRenderer()
        val path = doc.localFilePath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                val manager = PdfViewerManager(file)
                pdfViewerManager = manager
                _pdfPageCount.value = manager.pageCount
                _currentPdfPage.value = 0

                // Deserialize saved annotations
                _annotationsMap.value = deserializeAnnotations(doc.annotationsJson)

                renderCurrentPdfPage()
            }
        }
    }

    private fun renderCurrentPdfPage() {
        viewModelScope.launch {
            val manager = pdfViewerManager ?: return@launch
            val bitmap = manager.renderPage(_currentPdfPage.value, 1200)
            _pdfPageBitmap.value = bitmap
        }
    }

    fun nextPdfPage() {
        if (_currentPdfPage.value + 1 < _pdfPageCount.value) {
            _currentPdfPage.value++
            renderCurrentPdfPage()
        }
    }

    fun prevPdfPage() {
        if (_currentPdfPage.value > 0) {
            _currentPdfPage.value--
            renderCurrentPdfPage()
        }
    }

    fun setPdfPage(page: Int) {
        if (page in 0 until _pdfPageCount.value) {
            _currentPdfPage.value = page
            renderCurrentPdfPage()
        }
    }

    fun setPdfTool(tool: PdfTool) {
        _activePdfTool.value = if (_activePdfTool.value == tool) PdfTool.NONE else tool
    }

    fun setDrawColor(hex: String) {
        _activeDrawColorHex.value = hex
    }

    fun setStrokeWidth(width: Float) {
        _activeStrokeWidth.value = width
    }

    fun addPointToCurrentDrawing(xNorm: Float, yNorm: Float) {
        val pts = _currentDrawingPoints.value.toMutableList()
        pts.add(AnnotationPoint(xNorm, yNorm))
        _currentDrawingPoints.value = pts
    }

    fun finishCurrentDrawingPath() {
        val pts = _currentDrawingPoints.value
        if (pts.size >= 2) {
            val page = _currentPdfPage.value
            val isHighlighter = _activePdfTool.value == PdfTool.HIGHLIGHTER
            val stroke = if (isHighlighter) 22f else _activeStrokeWidth.value
            val item = DrawPathItem(
                points = pts,
                colorHex = _activeDrawColorHex.value,
                strokeWidth = stroke,
                isHighlighter = isHighlighter
            )

            val currentMap = _annotationsMap.value.toMutableMap()
            val pageAnn = currentMap[page] ?: PageAnnotations(page)
            val updatedPaths = pageAnn.paths + item
            currentMap[page] = pageAnn.copy(paths = updatedPaths)
            _annotationsMap.value = currentMap
            _isModified.value = true
        }
        _currentDrawingPoints.value = emptyList()
    }

    fun addTextAnnotation(text: String, xNorm: Float, yNorm: Float) {
        if (text.isBlank()) return
        val page = _currentPdfPage.value
        val item = TextAnnotationItem(
            id = UUID.randomUUID().toString(),
            text = text,
            x = xNorm,
            y = yNorm,
            colorHex = _activeDrawColorHex.value
        )
        val currentMap = _annotationsMap.value.toMutableMap()
        val pageAnn = currentMap[page] ?: PageAnnotations(page)
        currentMap[page] = pageAnn.copy(textNotes = pageAnn.textNotes + item)
        _annotationsMap.value = currentMap
        _isModified.value = true
        showSnackbar("Added note to page")
    }

    fun clearCurrentPageAnnotations() {
        val page = _currentPdfPage.value
        val currentMap = _annotationsMap.value.toMutableMap()
        currentMap.remove(page)
        _annotationsMap.value = currentMap
        _isModified.value = true
        showSnackbar("Cleared page markups")
    }

    fun exportAnnotatedPdfToStorage() {
        val doc = _currentDocument.value ?: return
        val path = doc.localFilePath ?: return
        viewModelScope.launch {
            val srcFile = File(path)
            val exportDir = getApplication<Application>().filesDir
            val destFile = File(exportDir, "Annotated_${System.currentTimeMillis()}_${doc.title}")
            val success = PdfGenerator.exportAnnotatedPdf(srcFile, destFile, _annotationsMap.value)
            if (success) {
                val newDoc = DocumentEntity(
                    title = destFile.name,
                    content = "Annotated PDF",
                    fileType = "PDF",
                    language = "pdf",
                    updatedAt = System.currentTimeMillis(),
                    localFilePath = destFile.absolutePath,
                    annotationsJson = serializeAnnotations(_annotationsMap.value)
                )
                val id = repository.saveDocument(newDoc)
                openDocument(newDoc.copy(id = id))
                showSnackbar("Exported annotated PDF: ${destFile.name}")
            } else {
                showSnackbar("Failed to export annotated PDF")
            }
        }
    }

    fun exportCurrentTextToPdf() {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            val exportDir = getApplication<Application>().filesDir
            val pdfTitle = doc.title.substringBeforeLast('.') + ".pdf"
            val destFile = File(exportDir, "Export_${System.currentTimeMillis()}_$pdfTitle")
            val isCode = doc.fileType == "CODE"
            val success = PdfGenerator.generatePdfFromText(
                title = doc.title,
                content = _editorValue.value.text,
                outputFile = destFile,
                isCode = isCode
            )
            if (success) {
                val newDoc = DocumentEntity(
                    title = destFile.name,
                    content = "Exported from ${doc.title}",
                    fileType = "PDF",
                    language = "pdf",
                    updatedAt = System.currentTimeMillis(),
                    localFilePath = destFile.absolutePath
                )
                val id = repository.saveDocument(newDoc)
                openDocument(newDoc.copy(id = id))
                showSnackbar("Converted & opened PDF: ${destFile.name}")
            } else {
                showSnackbar("Failed to generate PDF")
            }
        }
    }

    // Document Import / Export via SAF Uri
    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            val imported = repository.importFromUri(uri)
            if (imported != null) {
                openDocument(imported)
                showSnackbar("Imported: ${imported.title}")
            } else {
                showSnackbar("Could not import file")
            }
        }
    }

    fun exportDocumentToUri(uri: Uri) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            val ok = repository.exportToUri(uri, doc)
            if (ok) {
                showSnackbar("Exported: ${doc.title}")
            } else {
                showSnackbar("Export failed")
            }
        }
    }

    // Stats Calculation
    fun getDocumentStats(): UiStats {
        val text = _editorValue.value.text
        val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        val characters = text.length
        val charactersNoSpaces = text.count { !it.isWhitespace() }
        val lines = if (text.isEmpty()) 0 else text.lines().size
        val readingTime = (words / 200).coerceAtLeast(1)
        val bytes = text.toByteArray(Charsets.UTF_8).size
        return UiStats(words, characters, charactersNoSpaces, lines, readingTime, bytes)
    }

    // Settings & Dialog controls
    fun setTheme(theme: AppTheme) {
        _preferences.value = _preferences.value.copy(theme = theme)
    }

    fun updateFontSize(delta: Float) {
        val newSize = (_preferences.value.fontSizeSp + delta).coerceIn(10f, 26f)
        _preferences.value = _preferences.value.copy(fontSizeSp = newSize)
    }

    fun toggleLineNumbers() {
        _preferences.value = _preferences.value.copy(showLineNumbers = !_preferences.value.showLineNumbers)
    }

    fun toggleWordWrap() {
        _preferences.value = _preferences.value.copy(wordWrap = !_preferences.value.wordWrap)
    }

    fun toggleAutoSave() {
        val newState = !_preferences.value.autoSaveEnabled
        _preferences.value = _preferences.value.copy(autoSaveEnabled = newState)
        if (!newState) {
            autoSaveJob?.cancel()
        }
        showSnackbar(if (newState) "Auto-save enabled (every 3s)" else "Auto-save disabled")
    }

    fun setLanguage(languageId: String) {
        val doc = _currentDocument.value ?: return
        val updated = doc.copy(language = languageId)
        _currentDocument.value = updated
        viewModelScope.launch {
            repository.saveDocument(updated)
        }
        updateCompletions()
        showSnackbar("Syntax set to: ${EditorLanguage.fromId(languageId).displayName}")
    }

    fun setStatsVisible(visible: Boolean) { _isStatsVisible.value = visible }
    fun setThemeDialogVisible(visible: Boolean) { _isThemeDialogVisible.value = visible }
    fun setNewFileDialogVisible(visible: Boolean) { _isNewFileDialogVisible.value = visible }
    fun setHtmlPreviewVisible(visible: Boolean) { _isHtmlPreviewVisible.value = visible }
    fun setSearchFilesDialogVisible(visible: Boolean) { _isSearchFilesDialogVisible.value = visible }
    fun setDrawerFilter(filter: String) { _drawerFilter.value = filter }
    fun setDrawerSearch(query: String) { _drawerSearch.value = query }

    fun openDocumentAtLine(doc: DocumentEntity, targetLine: Int?) {
        openDocument(doc)
        if (targetLine != null && targetLine > 0 && doc.fileType != "PDF") {
            val lines = doc.content.lines()
            var charOffset = 0
            for (i in 0 until (targetLine - 1).coerceAtMost(lines.size - 1)) {
                charOffset += lines[i].length + 1
            }
            val lineLen = lines.getOrNull(targetLine - 1)?.length ?: 0
            _editorValue.value = _editorValue.value.copy(
                selection = TextRange(charOffset, (charOffset + lineLen).coerceAtMost(doc.content.length))
            )
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun closePdfRenderer() {
        pdfViewerManager?.close()
        pdfViewerManager = null
        _pdfPageBitmap.value = null
        _pdfPageCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        closePdfRenderer()
        try {
            claudeConnectorServer.stop()
        } catch (_: Exception) {}
    }

    // JSON serialization for annotations
    private fun serializeAnnotations(map: Map<Int, PageAnnotations>): String {
        val array = JSONArray()
        for ((page, ann) in map) {
            val pageObj = JSONObject()
            pageObj.put("page", page)

            val pathsArray = JSONArray()
            for (p in ann.paths) {
                val pObj = JSONObject()
                pObj.put("color", p.colorHex)
                pObj.put("stroke", p.strokeWidth.toDouble())
                pObj.put("highlighter", p.isHighlighter)
                val ptsArray = JSONArray()
                for (pt in p.points) {
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    ptsArray.put(ptObj)
                }
                pObj.put("points", ptsArray)
                pathsArray.put(pObj)
            }
            pageObj.put("paths", pathsArray)

            val notesArray = JSONArray()
            for (n in ann.textNotes) {
                val nObj = JSONObject()
                nObj.put("id", n.id)
                nObj.put("text", n.text)
                nObj.put("x", n.x.toDouble())
                nObj.put("y", n.y.toDouble())
                nObj.put("color", n.colorHex)
                nObj.put("size", n.fontSize.toDouble())
                notesArray.put(nObj)
            }
            pageObj.put("notes", notesArray)

            array.put(pageObj)
        }
        return array.toString()
    }

    private fun deserializeAnnotations(json: String?): Map<Int, PageAnnotations> {
        if (json.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<Int, PageAnnotations>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val pageObj = array.getJSONObject(i)
                val page = pageObj.getInt("page")

                val pathsList = mutableListOf<DrawPathItem>()
                val pathsArray = pageObj.optJSONArray("paths") ?: JSONArray()
                for (j in 0 until pathsArray.length()) {
                    val pObj = pathsArray.getJSONObject(j)
                    val color = pObj.getString("color")
                    val stroke = pObj.getDouble("stroke").toFloat()
                    val highlighter = pObj.optBoolean("highlighter", false)
                    val ptsArray = pObj.getJSONArray("points")
                    val pts = mutableListOf<AnnotationPoint>()
                    for (k in 0 until ptsArray.length()) {
                        val ptObj = ptsArray.getJSONObject(k)
                        pts.add(AnnotationPoint(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                    }
                    pathsList.add(DrawPathItem(pts, color, stroke, highlighter))
                }

                val notesList = mutableListOf<TextAnnotationItem>()
                val notesArray = pageObj.optJSONArray("notes") ?: JSONArray()
                for (j in 0 until notesArray.length()) {
                    val nObj = notesArray.getJSONObject(j)
                    notesList.add(
                        TextAnnotationItem(
                            id = nObj.getString("id"),
                            text = nObj.getString("text"),
                            x = nObj.getDouble("x").toFloat(),
                            y = nObj.getDouble("y").toFloat(),
                            colorHex = nObj.getString("color"),
                            fontSize = nObj.optDouble("size", 14.0).toFloat()
                        )
                    )
                }

                result[page] = PageAnnotations(page, pathsList, notesList)
            }
        } catch (_: Exception) {}
        return result
    }

    fun setExecutionConsoleVisible(visible: Boolean) {
        _isExecutionConsoleVisible.value = visible
    }

    fun clearExecutionResult() {
        _executionResult.value = null
    }

    fun runCurrentCode() {
        val doc = _currentDocument.value ?: return
        val code = _editorValue.value.text
        val ext = doc.title.substringAfterLast('.', "")
        val lang = EditorLanguage.fromId(doc.language).takeIf { it != EditorLanguage.PLAIN_TEXT }
            ?: EditorLanguage.fromExtension(ext)

        _isExecuting.value = true
        _isExecutionConsoleVisible.value = true

        viewModelScope.launch {
            try {
                val result = LanguageExecutor.execute(code, lang)
                _executionResult.value = result
            } catch (e: Exception) {
                _executionResult.value = ExecutionResult(
                    success = false,
                    stdout = "",
                    stderr = "Execution error: ${e.message}\n" + (e.stackTraceToString().take(500)),
                    exitCode = 1,
                    executionTimeMs = 0
                )
            } finally {
                _isExecuting.value = false
            }
        }
    }

    // Projects & Workspaces Operations
    fun setNewProjectDialogVisible(visible: Boolean) {
        _isNewProjectDialogVisible.value = visible
    }

    fun setCloneGitHubDialogVisible(visible: Boolean) {
        _isCloneGitHubDialogVisible.value = visible
    }

    private suspend fun loadProjectsList() {
        var projects = projectManager.getProjects()
        if (projects.isEmpty()) {
            // Seed a starter Web App project
            val defaultProj = projectManager.createProject("MyWebApp", ProjectTemplateType.WEB_APP)
            projects = listOf(defaultProj)
        }
        _allProjects.value = projects
        if (_currentProject.value == null && projects.isNotEmpty()) {
            openProject(projects.first())
        }
    }

    fun openProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            val tree = projectManager.buildFileTree(project)
            val files = projectManager.getAllProjectFiles(project)
            _projectFileTree.value = tree
            _activeProjectFiles.value = files

            // Open primary file if nothing is open or switching project
            val entryFile = files.keys.firstOrNull { it == "index.html" || it == "main.asm" || it == "index.ts" || it == "index.js" || it.endsWith(".md") }
                ?: files.keys.firstOrNull()
            if (entryFile != null) {
                openProjectFile(entryFile)
            }
        }
    }

    fun refreshProjectTree() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val tree = projectManager.buildFileTree(proj)
            val files = projectManager.getAllProjectFiles(proj)
            _projectFileTree.value = tree
            _activeProjectFiles.value = files
        }
    }

    fun openFolder(treeUri: Uri) {
        viewModelScope.launch {
            val project = projectManager.openFolderFromUri(treeUri)
            if (project != null) {
                val projects = projectManager.getProjects()
                _allProjects.value = projects
                openProject(project)
                showSnackbar("Opened workspace: ${project.name}")
            } else {
                showSnackbar("Could not open folder")
            }
        }
    }

    fun createNewProject(name: String, template: ProjectTemplateType) {
        viewModelScope.launch {
            _isNewProjectDialogVisible.value = false
            val project = projectManager.createProject(name, template)
            val projects = projectManager.getProjects()
            _allProjects.value = projects
            openProject(project)
            showSnackbar("Created project: ${project.name}")
        }
    }

    fun cloneGitHub(repoUrlOrSlug: String, token: String?) {
        _isCloning.value = true
        _cloneStatus.value = "Starting clone..."
        viewModelScope.launch {
            val result = gitHubManager.cloneRepository(
                repoUrlOrSlug = repoUrlOrSlug,
                token = token,
                onProgress = { status -> _cloneStatus.value = status }
            )
            _isCloning.value = false
            if (result.isSuccess) {
                val project = result.getOrNull()!!
                _isCloneGitHubDialogVisible.value = false
                val projects = projectManager.getProjects()
                _allProjects.value = projects
                openProject(project)
                showSnackbar("Cloned ${project.name} successfully")
            } else {
                showSnackbar("Clone failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun openProjectFile(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val content = projectManager.readFile(project, relativePath)
            val fileName = relativePath.substringAfterLast('/')
            val ext = fileName.substringAfterLast('.', "")
            val fileType = if (ext.equals("pdf", ignoreCase = true)) "PDF" else "CODE"
            val lang = EditorLanguage.fromExtension(ext).id

            _currentProjectFilePath.value = relativePath

            if (fileType == "PDF") {
                val localPath = if (!project.isExternalFolder) File(project.rootPath, relativePath).absolutePath else null
                val doc = DocumentEntity(
                    id = System.currentTimeMillis(),
                    title = fileName,
                    content = content,
                    fileType = "PDF",
                    language = "pdf",
                    localFilePath = localPath
                )
                openDocument(doc)
            } else {
                val doc = DocumentEntity(
                    id = System.currentTimeMillis(),
                    title = fileName,
                    content = content,
                    fileType = fileType,
                    language = lang
                )
                openDocument(doc)
            }
            checkTypeScriptTranspile()
        }
    }

    fun createProjectFile(parentRelPath: String, fileName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val success = projectManager.createFile(project, parentRelPath, fileName)
            if (success) {
                refreshProjectTree()
                val targetPath = if (parentRelPath.isEmpty()) fileName else "$parentRelPath/$fileName"
                openProjectFile(targetPath)
                showSnackbar("Created $fileName")
            } else {
                showSnackbar("Failed to create file")
            }
        }
    }

    fun createProjectFolder(parentRelPath: String, folderName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val success = projectManager.createFolder(project, parentRelPath, folderName)
            if (success) {
                refreshProjectTree()
                showSnackbar("Created folder: $folderName")
            } else {
                showSnackbar("Failed to create folder")
            }
        }
    }

    fun deleteProjectItem(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val success = projectManager.deleteItem(project, relativePath)
            if (success) {
                if (_currentProjectFilePath.value == relativePath) {
                    _currentProjectFilePath.value = null
                }
                refreshProjectTree()
                showSnackbar("Deleted $relativePath")
            } else {
                showSnackbar("Failed to delete")
            }
        }
    }

    fun exportActiveProjectToZip(uri: Uri) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            try {
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                if (stream != null) {
                    val success = projectManager.exportProjectToZip(project, stream)
                    if (success) showSnackbar("Project exported to ZIP successfully")
                    else showSnackbar("ZIP export failed")
                }
            } catch (e: Exception) {
                showSnackbar("Export error: ${e.message}")
            }
        }
    }

    fun exportActiveProjectToFolder(treeUri: Uri) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val success = projectManager.exportProjectToFolder(project, treeUri)
            if (success) showSnackbar("Project exported to folder successfully")
            else showSnackbar("Folder export failed")
        }
    }

    fun setTerminalVisible(visible: Boolean) {
        _isTerminalVisible.value = visible
    }

    fun setAiAssistantVisible(visible: Boolean) {
        _isAiAssistantVisible.value = visible
    }

    private fun checkTypeScriptTranspile() {
        val doc = _currentDocument.value ?: return
        val ext = doc.title.substringAfterLast('.', "").lowercase()
        if (doc.language == "typescript" || ext == "ts" || ext == "tsx" || ext == "mts" || ext == "cts") {
            val result = TypeScriptCompiler.transpile(_editorValue.value.text)
            _tsTranspileStatus.value = if (result.success) "TypeScript: Transpiled OK" else "TypeScript: ${result.diagnostics.firstOrNull() ?: "Transpile error"}"
        } else {
            _tsTranspileStatus.value = null
        }
    }
}
