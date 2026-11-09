package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.Project
import com.example.editor.ai.AiChatMessage
import com.example.editor.ai.AiCopilotManager
import com.example.editor.ai.AiProvider
import com.example.editor.connector.ClaudeConnectorServer
import com.example.ui.theme.EditorThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AssistantMode {
    CLAUDE_CHAT,
    CLAUDE_CONNECTOR,
    COPILOT
}

@Composable
fun AiAssistantSheet(
    aiManager: AiCopilotManager,
    currentDoc: DocumentEntity?,
    editorText: String,
    currentProject: Project?,
    recentDocs: List<DocumentEntity>,
    themeColors: EditorThemeColors,
    onInsertCode: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    connectorServer: ClaudeConnectorServer? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var activeMode by remember { mutableStateOf(AssistantMode.CLAUDE_CONNECTOR) }
    var userPrompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var apiKeyText by remember { mutableStateOf(aiManager.userClaudeApiKey ?: "") }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    val connectorStatus by (connectorServer?.status?.collectAsState() ?: remember { mutableStateOf<com.example.editor.connector.ConnectorServerStatus?>(null) })
    val connectorLogs by (connectorServer?.logs?.collectAsState() ?: remember { mutableStateOf<List<com.example.editor.connector.ConnectorLogEvent>>(emptyList()) })

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                role = "assistant",
                content = "Hello! I am connected to your Texidor IDE workspace. I have context on **${currentProject?.name ?: "your active project"}** and `${currentDoc?.title ?: "current document"}`.\n\nAsk me anything about your architecture, request code refactoring, or generate tests."
            )
        )
    }
    val listState = rememberLazyListState()

    fun showCopiedNotice(text: String) {
        copyNotice = text
        coroutineScope.launch {
            delay(2500)
            copyNotice = null
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading) return
        val query = text.trim()
        userPrompt = ""
        messages.add(AiChatMessage(role = "user", content = query))
        isLoading = true

        coroutineScope.launch {
            if (activeMode == AssistantMode.CLAUDE_CHAT) {
                val res = aiManager.queryClaude(
                    prompt = query,
                    currentDoc = currentDoc,
                    editorText = editorText,
                    currentProject = currentProject,
                    recentDocs = recentDocs,
                    chatHistory = messages
                )
                isLoading = false
                if (res.isSuccess) {
                    messages.add(AiChatMessage(role = "assistant", content = res.getOrNull()!!))
                } else {
                    messages.add(AiChatMessage(role = "assistant", content = "Error connecting to Claude: ${res.exceptionOrNull()?.message}"))
                }
            } else {
                val res = aiManager.queryCopilot(
                    prompt = query,
                    currentDoc = currentDoc,
                    editorText = editorText,
                    cursorOffset = 0
                )
                isLoading = false
                if (res.isSuccess) {
                    messages.add(AiChatMessage(role = "assistant", content = res.getOrNull()!!))
                } else {
                    messages.add(AiChatMessage(role = "assistant", content = "Copilot Error: ${res.exceptionOrNull()?.message}"))
                }
            }
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp, max = 560.dp)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        // TOP CONTROL BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = themeColors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Mode Toggle Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(2.dp)
                ) {
                    // Claude Connector (Link)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeMode == AssistantMode.CLAUDE_CONNECTOR) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { activeMode = AssistantMode.CLAUDE_CONNECTOR }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Claude Link",
                                color = if (activeMode == AssistantMode.CLAUDE_CONNECTOR) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (connectorStatus?.isRunning == true) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                        }
                    }

                    // Claude Chat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeMode == AssistantMode.CLAUDE_CHAT) themeColors.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { activeMode = AssistantMode.CLAUDE_CHAT }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Claude Chat",
                            color = if (activeMode == AssistantMode.CLAUDE_CHAT) themeColors.primary else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // GitHub Copilot
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeMode == AssistantMode.COPILOT) themeColors.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { activeMode = AssistantMode.COPILOT }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Copilot",
                            color = if (activeMode == AssistantMode.COPILOT) themeColors.primary else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeMode == AssistantMode.CLAUDE_CHAT) {
                    IconButton(
                        onClick = { showApiKeyInput = !showApiKeyInput },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Keys",
                            tint = if (aiManager.userClaudeApiKey != null) themeColors.primary else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }

        // COPIED TOAST BANNER
        if (copyNotice != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF059669))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓ $copyNotice",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // CONTEXT BADGE STRIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131D33))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${currentProject?.name ?: "Workspace"} • File: ${currentDoc?.title ?: "None"} • ${recentDocs.size} recent files",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        if (activeMode == AssistantMode.CLAUDE_CONNECTOR) {
            // =========================================================================
            // CLAUDE CUSTOM CONNECTOR HUB (MCP & INPUT LINK FOR CLAUDE)
            // =========================================================================
            val status = connectorStatus
            val isRunning = status?.isRunning == true
            val mcpUrl = "${status?.lanUrl ?: "http://127.0.0.1:8080"}/mcp"
            val localMcpUrl = "${status?.localUrl ?: "http://localhost:8080"}/mcp"
            val sseUrl = "${status?.lanUrl ?: "http://127.0.0.1:8080"}/sse"

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SERVER STATUS & TOGGLE
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isRunning) "Claude Connector Active (Port ${status?.port ?: 8080})" else "Connector Server Stopped",
                                    color = if (isRunning) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isRunning) "${status?.requestCount ?: 0} requests handled" else "Tap Start to listen for Claude",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isRunning) {
                                    connectorServer?.stop()
                                } else {
                                    connectorServer?.start(8080)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFF334155) else Color(0xFF059669)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isRunning) "Restart" else "Start",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // PRIMARY INPUT LINK FOR CLAUDE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Input Link for Claude (MCP Endpoint)", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(mcpUrl))
                                    showCopiedNotice("Copied Claude Connector Link!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Link", fontSize = 10.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // URL Display Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D16))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(mcpUrl))
                                    showCopiedNotice("Copied Claude Connector Link!")
                                }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = mcpUrl,
                                color = Color(0xFF7DD3FC),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Local device loopback: $localMcpUrl",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // INSTRUCTIONS CARD
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A2338))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 How to Connect Claude to Texidor:",
                            color = Color(0xFFF1F5F9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. In Claude (Claude.ai or Claude Desktop), open Settings → Integrations / Connectors.\n2. Paste the Connector Link above into the connector input box.\n3. Claude now has real-time access to read & edit your files, inspect project trees, and run shell commands in Texidor!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Claude Desktop Config JSON snippet
                        val desktopJson = "{\n  \"mcpServers\": {\n    \"texidor\": {\n      \"url\": \"$mcpUrl\"\n    }\n  }\n}"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Claude Desktop (claude_desktop_config.json):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(desktopJson))
                                    showCopiedNotice("Copied Claude Desktop Config JSON!")
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Copy JSON", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D16))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = desktopJson,
                                color = Color(0xFFA7F3D0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // CAPABILITIES GRANTED TO CLAUDE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Claude Permissions via this Connector:",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val perms = listOf(
                            "📂 Projects" to (currentProject?.name ?: "All Workspace Files"),
                            "🕒 Recent Files" to "${recentDocs.size} files tracked",
                            "📝 Active Buffer" to (currentDoc?.title ?: "None (Read / Write)"),
                            "💻 Ubuntu Terminal" to "Command execution (`apt`, `make`, `python3`)"
                        )

                        for ((label, detail) in perms) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(detail, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ACTIVITY LOG
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connector Activity Log", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { connectorServer?.clearLogs() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                        }

                        if (connectorLogs.isEmpty()) {
                            Text(
                                text = "Waiting for Claude connection requests...",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            for (log in connectorLogs.take(10)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.formattedTime(),
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.method,
                                        color = if (log.isSuccess) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.details,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // CHAT MODE (CLAUDE CHAT / GITHUB COPILOT)
            // =========================================================================

            // OPTIONAL API KEY EXPANDER
            if (showApiKeyInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2338))
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            aiManager.userClaudeApiKey = it.trim().ifEmpty { null }
                        },
                        label = { Text("Anthropic / Claude API Key", fontSize = 11.sp) },
                        placeholder = { Text("sk-ant-api03-...", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // QUICK ACTION CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chips = listOf(
                    "Explain this code",
                    "Generate unit tests",
                    "Suggest refactoring",
                    "Add TypeScript types",
                    "Find potential bugs",
                    "Summarize project"
                )
                for (chip in chips) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { sendMessage(chip) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(chip, color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // MESSAGES STREAM
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.role == "user"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isUser) themeColors.primary.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    if (isUser) themeColors.primary.copy(alpha = 0.5f) else Color(0xFF334155),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.content,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                if (!isUser && msg.content.contains("```")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val codeBlock = extractCodeBlock(msg.content)
                                    if (codeBlock.isNotEmpty()) {
                                        Button(
                                            onClick = { onInsertCode(codeBlock) },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = themeColors.surface, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert into Editor", fontSize = 10.sp, color = themeColors.surface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColors.primary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking with workspace context...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }

            // INPUT BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = {
                        Text(
                            text = if (activeMode == AssistantMode.CLAUDE_CHAT) "Ask Claude about your project..." else "Ask Copilot for completions...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage(userPrompt) })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage(userPrompt) },
                    enabled = userPrompt.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (userPrompt.isNotBlank() && !isLoading) themeColors.primary else Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (userPrompt.isNotBlank() && !isLoading) themeColors.surface else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun extractCodeBlock(content: String): String {
    val start = content.indexOf("```")
    if (start == -1) return ""
    val lineBreak = content.indexOf('\n', start)
    if (lineBreak == -1) return ""
    val end = content.indexOf("```", lineBreak)
    if (end == -1) return content.substring(lineBreak + 1).trim()
    return content.substring(lineBreak + 1, end).trim()
}
