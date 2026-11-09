package com.example.editor.connector

import android.content.Context
import com.example.data.model.DocumentEntity
import com.example.data.model.Project
import com.example.editor.terminal.UbuntuShellEmulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class ConnectorLogEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val details: String,
    val isSuccess: Boolean = true
) {
    fun formattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}

data class ConnectorServerStatus(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val localUrl: String = "http://localhost:8080",
    val lanUrl: String = "http://127.0.0.1:8080",
    val requestCount: Long = 0,
    val errorMessage: String? = null
)

/**
 * Embedded Claude Custom Connector Server for Texidor IDE.
 * Implements Model Context Protocol (MCP) over HTTP/SSE and REST endpoints,
 * allowing Anthropic Claude (Claude.ai / Claude Desktop / custom connectors)
 * to connect via an input link to read projects, recently edited files, folders,
 * active editor content, and execute terminal commands.
 */
class ClaudeConnectorServer(
    private val context: Context,
    private val terminalEmulator: UbuntuShellEmulator,
    private val getActiveDocument: () -> DocumentEntity?,
    private val getAllDocuments: () -> List<DocumentEntity>,
    private val getRecentDocuments: () -> List<DocumentEntity>,
    private val getActiveProject: () -> Project?,
    private val getAllProjects: () -> List<Project>,
    private val onSaveDocument: ((title: String, content: String) -> Unit)? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val requestCounter = AtomicLong(0)

    private val _status = MutableStateFlow(ConnectorServerStatus())
    val status: StateFlow<ConnectorServerStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<ConnectorLogEvent>>(emptyList())
    val logs: StateFlow<List<ConnectorLogEvent>> = _logs.asStateFlow()

    fun start(port: Int = 8080) {
        if (running.get()) return

        scope.launch {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                running.set(true)

                val ip = getLanIpAddress() ?: "127.0.0.1"
                _status.value = ConnectorServerStatus(
                    isRunning = true,
                    port = port,
                    localUrl = "http://localhost:$port",
                    lanUrl = "http://$ip:$port",
                    requestCount = 0,
                    errorMessage = null
                )

                addLog("SERVER_START", "Claude Connector listening on $ip:$port", true)

                while (running.get() && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        scope.launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (!running.get()) break
                    }
                }
            } catch (e: Exception) {
                _status.value = _status.value.copy(
                    isRunning = false,
                    errorMessage = e.message ?: "Failed to start server"
                )
                addLog("SERVER_ERROR", "Failed to start on port $port: ${e.message}", false)
            }
        }
    }

    fun stop() {
        if (!running.get()) return
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _status.value = _status.value.copy(isRunning = false)
        addLog("SERVER_STOP", "Claude Connector server stopped", true)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(method: String, details: String, isSuccess: Boolean = true) {
        val event = ConnectorLogEvent(
            method = method,
            details = details,
            isSuccess = isSuccess
        )
        _logs.value = (listOf(event) + _logs.value).take(100)
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")
            val queryParams = parseQueryParams(fullPath.substringAfter("?", ""))

            // Read headers
            val headers = mutableMapOf<String, String>()
            var headerLine = reader.readLine()
            var contentLength = 0
            while (!headerLine.isNullOrBlank()) {
                val colon = headerLine.indexOf(':')
                if (colon > 0) {
                    val key = headerLine.substring(0, colon).trim().lowercase()
                    val value = headerLine.substring(colon + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
                headerLine = reader.readLine()
            }

            // Read body if POST
            val body = if (contentLength > 0) {
                val charArray = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(charArray, read, contentLength - read)
                    if (r <= 0) break
                    read += r
                }
                String(charArray, 0, read)
            } else ""

            requestCounter.incrementAndGet()
            _status.value = _status.value.copy(requestCount = requestCounter.get())

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendCorsPreflightResponse(out)
                return@withContext
            }

            // Route request
            when {
                // MCP JSON-RPC endpoint (standard link for Claude custom connector)
                path == "/mcp" || path == "/message" || path == "/rpc" -> {
                    if (method == "POST") {
                        handleMcpJsonRpc(body, out)
                    } else {
                        handleMcpInfo(out)
                    }
                }

                // MCP SSE Streaming transport
                path == "/sse" || path == "/events" -> {
                    handleMcpSse(out)
                }

                // REST API: Workspace Status & Context
                path == "/api/status" || path == "/status" -> {
                    handleApiStatus(out)
                }

                // REST API: Active Document
                path == "/api/active" -> {
                    handleApiActive(out)
                }

                // REST API: Recent Documents
                path == "/api/recent" -> {
                    handleApiRecent(out)
                }

                // REST API: Projects list
                path == "/api/projects" -> {
                    handleApiProjects(out)
                }

                // REST API: Read or Write file
                path == "/api/file" || path == "/api/files" -> {
                    if (method == "POST") {
                        handleApiWriteFile(body, out)
                    } else {
                        handleApiReadFile(queryParams, out)
                    }
                }

                // REST API: Terminal command execution
                path == "/api/terminal" || path == "/api/exec" -> {
                    handleApiTerminal(body, out)
                }

                // Web Landing & Connection Info Page
                path == "/" || path == "/connector" || path == "/index.html" -> {
                    handleWebDashboard(out)
                }

                else -> {
                    sendJsonResponse(out, 404, JSONObject().put("error", "Not Found").put("path", path).toString())
                }
            }
        } catch (e: Exception) {
            // Connection error / timeout
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Handles MCP (Model Context Protocol) JSON-RPC requests from Claude
     */
    private suspend fun handleMcpJsonRpc(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val jsonRpc = json.optString("jsonrpc", "2.0")
            val id = json.opt("id")
            val method = json.optString("method")
            val params = json.optJSONObject("params") ?: JSONObject()

            addLog("MCP:$method", "Claude called $method", true)

            val result = when (method) {
                "initialize" -> {
                    JSONObject().apply {
                        put("protocolVersion", "2024-11-05")
                        put("serverInfo", JSONObject().apply {
                            put("name", "texidor-claude-connector")
                            put("version", "1.0.0")
                            put("description", "Texidor Android IDE Workspace Connector for Anthropic Claude")
                        })
                        put("capabilities", JSONObject().apply {
                            put("tools", JSONObject().put("listChanged", false))
                            put("resources", JSONObject().put("subscribe", false).put("listChanged", false))
                            put("prompts", JSONObject().put("listChanged", false))
                        })
                    }
                }

                "notifications/initialized" -> {
                    // Acknowledgement notification, no result needed
                    JSONObject()
                }

                "tools/list" -> {
                    JSONObject().apply {
                        val toolsArray = JSONArray()

                        // Tool 1: list_projects
                        toolsArray.put(JSONObject().apply {
                            put("name", "list_projects")
                            put("description", "Lists all coding projects inside Texidor IDE with root directory paths, names, and templates.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject())
                            })
                        })

                        // Tool 2: list_recent_files
                        toolsArray.put(JSONObject().apply {
                            put("name", "list_recent_files")
                            put("description", "Lists all recently edited and opened files in Texidor with timestamps, file types, line counts, and titles.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject())
                            })
                        })

                        // Tool 3: get_active_document
                        toolsArray.put(JSONObject().apply {
                            put("name", "get_active_document")
                            put("description", "Returns the currently opened document in the Texidor editor, including filename, language, line count, and source code content.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject())
                            })
                        })

                        // Tool 4: read_file
                        toolsArray.put(JSONObject().apply {
                            put("name", "read_file")
                            put("description", "Reads the source code or text content of a file in the active project or storage.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject().apply {
                                    put("path_or_title", JSONObject().put("type", "string").put("description", "File name or absolute path to read"))
                                })
                                put("required", JSONArray().put("path_or_title"))
                            })
                        })

                        // Tool 5: write_file
                        toolsArray.put(JSONObject().apply {
                            put("name", "write_file")
                            put("description", "Creates or updates a file in the active Texidor workspace or project.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject().apply {
                                    put("title", JSONObject().put("type", "string").put("description", "Filename or path"))
                                    put("content", JSONObject().put("type", "string").put("description", "Full updated file content"))
                                })
                                put("required", JSONArray().put("title").put("content"))
                            })
                        })

                        // Tool 6: get_project_tree
                        toolsArray.put(JSONObject().apply {
                            put("name", "get_project_tree")
                            put("description", "Lists all files, folders, and hierarchy of the active or specified project.")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject().apply {
                                    put("project_name", JSONObject().put("type", "string").put("description", "Optional project name. Defaults to active project."))
                                })
                            })
                        })

                        // Tool 7: execute_terminal_command
                        toolsArray.put(JSONObject().apply {
                            put("name", "execute_terminal_command")
                            put("description", "Executes a shell command in Texidor's Ubuntu 24.04 Termux environment (e.g. ls, apt, make, python3, gcc).")
                            put("inputSchema", JSONObject().apply {
                                put("type", "object")
                                put("properties", JSONObject().apply {
                                    put("command", JSONObject().put("type", "string").put("description", "The shell command to run"))
                                })
                                put("required", JSONArray().put("command"))
                            })
                        })

                        put("tools", toolsArray)
                    }
                }

                "tools/call" -> {
                    val toolName = params.optString("name")
                    val arguments = params.optJSONObject("arguments") ?: JSONObject()
                    executeMcpTool(toolName, arguments)
                }

                "resources/list" -> {
                    JSONObject().apply {
                        val resArray = JSONArray()
                        resArray.put(JSONObject().apply {
                            put("uri", "texidor://workspace/active")
                            put("name", "Active Document")
                            put("description", "Currently open source code file in Texidor IDE")
                            put("mimeType", "text/plain")
                        })
                        resArray.put(JSONObject().apply {
                            put("uri", "texidor://workspace/recent")
                            put("name", "Recent Documents")
                            put("description", "Recently edited documents in Texidor")
                            put("mimeType", "application/json")
                        })
                        resArray.put(JSONObject().apply {
                            put("uri", "texidor://workspace/projects")
                            put("name", "Projects List")
                            put("description", "List of all projects created in Texidor")
                            put("mimeType", "application/json")
                        })
                        put("resources", resArray)
                    }
                }

                "resources/read" -> {
                    val uri = params.optString("uri")
                    readMcpResource(uri)
                }

                "prompts/list" -> {
                    JSONObject().apply {
                        val promptsArray = JSONArray()
                        promptsArray.put(JSONObject().apply {
                            put("name", "review_code")
                            put("description", "Asks Claude to review active editor document for bugs, security, and performance")
                        })
                        promptsArray.put(JSONObject().apply {
                            put("name", "generate_tests")
                            put("description", "Asks Claude to generate comprehensive unit tests for current code")
                        })
                        put("prompts", promptsArray)
                    }
                }

                else -> {
                    JSONObject().put("error", JSONObject().put("code", -32601).put("message", "Method '$method' not found"))
                }
            }

            val responseJson = JSONObject().apply {
                put("jsonrpc", jsonRpc)
                if (id != null) put("id", id)
                if (result.has("error")) {
                    put("error", result.getJSONObject("error"))
                } else {
                    put("result", result)
                }
            }

            sendJsonResponse(out, 200, responseJson.toString())
        } catch (e: Exception) {
            val errJson = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("error", JSONObject().put("code", -32700).put("message", "Parse error: ${e.message}"))
            }
            sendJsonResponse(out, 200, errJson.toString())
        }
    }

    private suspend fun executeMcpTool(name: String, args: JSONObject): JSONObject {
        return try {
            val textContent = when (name) {
                "list_projects" -> {
                    val projects = getAllProjects()
                    if (projects.isEmpty()) {
                        "No projects found in Texidor. (You can create projects using New Project in Texidor IDE)."
                    } else {
                        projects.joinToString("\n") { p ->
                            "• Project: ${p.name} | Root: ${p.rootPath} | External: ${p.isExternalFolder}"
                        }
                    }
                }

                "list_recent_files" -> {
                    val recents = getRecentDocuments()
                    if (recents.isEmpty()) {
                        "No recently edited documents found."
                    } else {
                        recents.joinToString("\n") { doc ->
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(doc.updatedAt))
                            "• ${doc.title} (${doc.language}) - ${doc.content.lines().size} lines | Last updated: $dateStr"
                        }
                    }
                }

                "get_active_document" -> {
                    val doc = getActiveDocument()
                    if (doc == null) {
                        "No document is currently open in Texidor."
                    } else {
                        "File: ${doc.title}\nLanguage: ${doc.language}\nLine Count: ${doc.content.lines().size}\n\n```${doc.language}\n${doc.content}\n```"
                    }
                }

                "read_file" -> {
                    val target = args.optString("path_or_title")
                    val matchedDoc = getAllDocuments().find { it.title.equals(target, ignoreCase = true) }
                    if (matchedDoc != null) {
                        matchedDoc.content
                    } else {
                        val file = File(target)
                        if (file.exists() && file.isFile) {
                            file.readText()
                        } else {
                            "Error: File '$target' not found in Texidor workspace or filesystem."
                        }
                    }
                }

                "write_file" -> {
                    val title = args.optString("title")
                    val content = args.optString("content")
                    onSaveDocument?.invoke(title, content)
                    "Successfully updated '$title' (${content.length} bytes, ${content.lines().size} lines) in Texidor workspace."
                }

                "get_project_tree" -> {
                    val activeProj = getActiveProject()
                    if (activeProj != null) {
                        val dir = File(activeProj.rootPath)
                        if (dir.exists() && dir.isDirectory) {
                            val tree = buildFileTreeString(dir, depth = 0, maxDepth = 4)
                            "Project Tree for '${activeProj.name}':\n$tree"
                        } else {
                            "Project '${activeProj.name}' root directory does not exist on disk."
                        }
                    } else {
                        val docs = getAllDocuments()
                        "Open Workspace Files:\n" + docs.joinToString("\n") { "• ${it.title} (${it.language})" }
                    }
                }

                "execute_terminal_command" -> {
                    val cmd = args.optString("command")
                    val lines = terminalEmulator.executeCommand(cmd)
                    lines.joinToString("\n") { it.text }
                }

                else -> "Unknown tool '$name'"
            }

            JSONObject().apply {
                val contentArray = JSONArray()
                contentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", textContent)
                })
                put("content", contentArray)
            }
        } catch (e: Exception) {
            JSONObject().apply {
                val contentArray = JSONArray()
                contentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", "Error executing tool '$name': ${e.message}")
                })
                put("content", contentArray)
                put("isError", true)
            }
        }
    }

    private fun readMcpResource(uri: String): JSONObject {
        val text = when (uri) {
            "texidor://workspace/active" -> {
                val doc = getActiveDocument()
                doc?.content ?: ""
            }
            "texidor://workspace/recent" -> {
                val array = JSONArray()
                getRecentDocuments().forEach { d ->
                    array.put(JSONObject().apply {
                        put("title", d.title)
                        put("language", d.language)
                        put("lines", d.content.lines().size)
                        put("updatedAt", d.updatedAt)
                    })
                }
                array.toString(2)
            }
            "texidor://workspace/projects" -> {
                val array = JSONArray()
                getAllProjects().forEach { p ->
                    array.put(JSONObject().apply {
                        put("name", p.name)
                        put("rootPath", p.rootPath)
                        put("isExternal", p.isExternalFolder)
                    })
                }
                array.toString(2)
            }
            else -> "Resource not found: $uri"
        }

        return JSONObject().apply {
            val contents = JSONArray()
            contents.put(JSONObject().apply {
                put("uri", uri)
                put("text", text)
            })
            put("contents", contents)
        }
    }

    private fun handleMcpInfo(out: OutputStream) {
        val info = JSONObject().apply {
            put("status", "ok")
            put("server", "Texidor Claude Custom Connector")
            put("version", "1.0.0")
            put("protocol", "Model Context Protocol (MCP) 2024-11-05")
            put("endpoint", "/mcp")
            put("sse_endpoint", "/sse")
            put("active_document", getActiveDocument()?.title ?: "none")
            put("active_project", getActiveProject()?.name ?: "none")
        }
        sendJsonResponse(out, 200, info.toString(2))
    }

    private fun handleMcpSse(out: OutputStream) {
        // Send SSE headers
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/event-stream\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Connection: keep-alive\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        out.write(headers.toByteArray())
        out.flush()

        // Send endpoint event for MCP SSE transport
        val port = _status.value.port
        val event = "event: endpoint\r\ndata: http://localhost:$port/mcp\r\n\r\n"
        out.write(event.toByteArray())
        out.flush()
    }

    private fun handleApiStatus(out: OutputStream) {
        val doc = getActiveDocument()
        val proj = getActiveProject()
        val json = JSONObject().apply {
            put("status", "online")
            put("ide", "Texidor Android IDE")
            put("version", "1.0")
            put("active_document", doc?.title)
            put("active_language", doc?.language)
            put("active_project", proj?.name)
            put("recent_documents_count", getRecentDocuments().size)
            put("all_documents_count", getAllDocuments().size)
            put("terminal_cwd", terminalEmulator.getPromptString())
        }
        sendJsonResponse(out, 200, json.toString(2))
    }

    private fun handleApiActive(out: OutputStream) {
        val doc = getActiveDocument()
        if (doc == null) {
            sendJsonResponse(out, 200, JSONObject().put("has_active_doc", false).toString())
        } else {
            val json = JSONObject().apply {
                put("has_active_doc", true)
                put("title", doc.title)
                put("language", doc.language)
                put("fileType", doc.fileType)
                put("updatedAt", doc.updatedAt)
                put("content", doc.content)
            }
            sendJsonResponse(out, 200, json.toString(2))
        }
    }

    private fun handleApiRecent(out: OutputStream) {
        val recents = getRecentDocuments()
        val array = JSONArray()
        recents.forEach { d ->
            array.put(JSONObject().apply {
                put("title", d.title)
                put("language", d.language)
                put("updatedAt", d.updatedAt)
                put("lineCount", d.content.lines().size)
            })
        }
        sendJsonResponse(out, 200, array.toString(2))
    }

    private fun handleApiProjects(out: OutputStream) {
        val projects = getAllProjects()
        val array = JSONArray()
        projects.forEach { p ->
            array.put(JSONObject().apply {
                put("name", p.name)
                put("rootPath", p.rootPath)
                put("isExternal", p.isExternalFolder)
            })
        }
        sendJsonResponse(out, 200, array.toString(2))
    }

    private fun handleApiReadFile(params: Map<String, String>, out: OutputStream) {
        val pathOrTitle = params["path"] ?: params["title"] ?: ""
        if (pathOrTitle.isBlank()) {
            sendJsonResponse(out, 400, JSONObject().put("error", "Missing 'path' or 'title' parameter").toString())
            return
        }

        val doc = getAllDocuments().find { it.title.equals(pathOrTitle, ignoreCase = true) }
        if (doc != null) {
            sendJsonResponse(out, 200, JSONObject().apply {
                put("title", doc.title)
                put("language", doc.language)
                put("content", doc.content)
            }.toString())
        } else {
            val file = File(pathOrTitle)
            if (file.exists() && file.isFile) {
                sendJsonResponse(out, 200, JSONObject().apply {
                    put("title", file.name)
                    put("path", file.absolutePath)
                    put("content", file.readText())
                }.toString())
            } else {
                sendJsonResponse(out, 404, JSONObject().put("error", "File not found").toString())
            }
        }
    }

    private fun handleApiWriteFile(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val title = json.optString("title")
            val content = json.optString("content")
            if (title.isBlank()) {
                sendJsonResponse(out, 400, JSONObject().put("error", "Title is required").toString())
                return
            }
            onSaveDocument?.invoke(title, content)
            sendJsonResponse(out, 200, JSONObject().put("success", true).put("title", title).toString())
        } catch (e: Exception) {
            sendJsonResponse(out, 500, JSONObject().put("error", e.message).toString())
        }
    }

    private suspend fun handleApiTerminal(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val cmd = json.optString("command")
            val outputLines = terminalEmulator.executeCommand(cmd)
            val result = JSONArray()
            outputLines.forEach { line ->
                result.put(JSONObject().apply {
                    put("type", line.type.name)
                    put("text", line.text)
                })
            }
            sendJsonResponse(out, 200, JSONObject().put("output", result).toString())
        } catch (e: Exception) {
            sendJsonResponse(out, 500, JSONObject().put("error", e.message).toString())
        }
    }

    private fun handleWebDashboard(out: OutputStream) {
        val ip = getLanIpAddress() ?: "127.0.0.1"
        val port = _status.value.port
        val activeDoc = getActiveDocument()?.title ?: "None"
        val activeProj = getActiveProject()?.name ?: "None"
        val recentCount = getRecentDocuments().size

        val mcpUrl = "http://$ip:$port/mcp"
        val sseUrl = "http://$ip:$port/sse"

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Texidor — Claude Custom Connector</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0B0F19; color: #E2E8F0; padding: 24px; line-height: 1.5; }
                    .card { background: #111827; border: 1px solid #1F2937; border-radius: 12px; padding: 20px; max-width: 680px; margin: 0 auto 20px auto; }
                    h1 { color: #38BDF8; margin-top: 0; font-size: 22px; display: flex; align-items: center; gap: 8px; }
                    .badge { background: #059669; color: white; padding: 3px 8px; border-radius: 9999px; font-size: 11px; font-weight: bold; }
                    .url-box { background: #07090E; border: 1px solid #374151; border-radius: 8px; padding: 12px; font-family: monospace; font-size: 14px; color: #38BDF8; word-break: break-all; margin: 12px 0; }
                    pre { background: #07090E; border: 1px solid #374151; border-radius: 8px; padding: 12px; font-size: 12px; overflow-x: auto; color: #A7F3D0; }
                    .step { margin-bottom: 12px; }
                    .step-num { background: #38BDF8; color: #000; font-weight: bold; border-radius: 50%; display: inline-block; width: 22px; height: 22px; text-align: center; line-height: 22px; font-size: 12px; margin-right: 6px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>⚡ Texidor Claude Custom Connector <span class="badge">ONLINE</span></h1>
                    <p>Connect Anthropic Claude directly to your Texidor Android IDE workspace with real-time access to projects, recently edited files, and terminal execution.</p>
                    
                    <h3>📌 Input Link for Claude (MCP Endpoint):</h3>
                    <div class="url-box">$mcpUrl</div>
                    <p><small>Or on local device / ADB reverse: <code>http://localhost:$port/mcp</code></small></p>

                    <h3>⚙️ Claude Desktop Configuration:</h3>
                    <p>Paste this into your <code>claude_desktop_config.json</code>:</p>
                    <pre>{
  "mcpServers": {
    "texidor": {
      "url": "$mcpUrl"
    }
  }
}</pre>

                    <h3>📊 Active Workspace Context:</h3>
                    <ul>
                        <li><strong>Active Document:</strong> $activeDoc</li>
                        <li><strong>Active Project:</strong> $activeProj</li>
                        <li><strong>Recent Documents:</strong> $recentCount</li>
                        <li><strong>SSE Stream:</strong> <code>$sseUrl</code></li>
                    </ul>
                </div>
            </body>
            </html>
        """.trimIndent()

        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${html.toByteArray().size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(headers.toByteArray())
        out.write(html.toByteArray())
        out.flush()
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
        val statusText = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "Error"
        }
        val bytes = json.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization, x-api-key\r\n" +
                "Connection: close\r\n\r\n"
        out.write(headers.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun sendCorsPreflightResponse(out: OutputStream) {
        val response = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization, x-api-key\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray())
        out.flush()
    }

    private fun buildFileTreeString(dir: File, depth: Int, maxDepth: Int): String {
        if (depth > maxDepth) return ""
        val sb = StringBuilder()
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return ""
        val indent = "  ".repeat(depth)
        for (f in files) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                sb.append("$indent📁 ${f.name}/\n")
                sb.append(buildFileTreeString(f, depth + 1, maxDepth))
            } else {
                sb.append("$indent📄 ${f.name} (${f.length()} bytes)\n")
            }
        }
        return sb.toString()
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                map[key] = java.net.URLDecoder.decode(value, "UTF-8")
            }
        }
        return map
    }

    private fun getLanIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
