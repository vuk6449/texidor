package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.Project
import com.example.data.model.ProjectFileNode
import com.example.data.model.ProjectTemplateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectManager(private val context: Context) {

    private val projectsDir: File = File(context.filesDir, "projects").apply { mkdirs() }
    private val metadataFile: File = File(context.filesDir, "projects_metadata.json")

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        loadProjectsMetadata()
    }

    suspend fun createProject(name: String, template: ProjectTemplateType): Project = withContext(Dispatchers.IO) {
        val safeName = name.trim().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val id = UUID.randomUUID().toString().take(8)
        val projectFolder = File(projectsDir, "${safeName}_$id").apply { mkdirs() }

        val templateFiles = getTemplateFiles(template, safeName)
        for ((relPath, content) in templateFiles) {
            val file = File(projectFolder, relPath)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        }

        val project = Project(
            id = id,
            name = name.trim(),
            rootPath = projectFolder.absolutePath,
            isExternalFolder = false,
            gitBranch = "main",
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis()
        )

        saveProjectMetadata(project)
        project
    }

    suspend fun openFolderFromUri(treeUri: Uri): Project? = withContext(Dispatchers.IO) {
        try {
            // Take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val docFile = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
            val folderName = docFile.name ?: "Opened Workspace"
            val id = UUID.randomUUID().toString().take(8)

            val project = Project(
                id = id,
                name = folderName,
                rootPath = treeUri.toString(),
                isExternalFolder = true,
                createdAt = System.currentTimeMillis(),
                lastOpenedAt = System.currentTimeMillis()
            )

            saveProjectMetadata(project)
            project
        } catch (e: Exception) {
            null
        }
    }

    suspend fun buildFileTree(project: Project): ProjectFileNode = withContext(Dispatchers.IO) {
        if (project.isExternalFolder) {
            val treeUri = Uri.parse(project.rootPath)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            val rootNode = ProjectFileNode(
                name = project.name,
                path = "",
                isDirectory = true,
                extension = "",
                isExpanded = true
            )
            if (rootDoc != null && rootDoc.isDirectory) {
                populateDocumentFileTree(rootDoc, "", rootNode)
            }
            rootNode
        } else {
            val rootDir = File(project.rootPath)
            val rootNode = ProjectFileNode(
                name = project.name,
                path = "",
                isDirectory = true,
                extension = "",
                isExpanded = true
            )
            if (rootDir.exists() && rootDir.isDirectory) {
                populateLocalFileTree(rootDir, "", rootNode)
            }
            rootNode
        }
    }

    private fun populateLocalFileTree(dir: File, currentRelPath: String, parentNode: ProjectFileNode) {
        val files = dir.listFiles()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: return

        for (file in files) {
            if (file.name.startsWith(".git") && file.isDirectory) continue
            val relPath = if (currentRelPath.isEmpty()) file.name else "$currentRelPath/${file.name}"
            val ext = if (file.isDirectory) "" else file.extension.lowercase()
            val node = ProjectFileNode(
                name = file.name,
                path = relPath,
                isDirectory = file.isDirectory,
                extension = ext,
                isExpanded = true
            )
            if (file.isDirectory) {
                populateLocalFileTree(file, relPath, node)
            }
            parentNode.children.add(node)
        }
    }

    private fun populateDocumentFileTree(doc: DocumentFile, currentRelPath: String, parentNode: ProjectFileNode) {
        val children = doc.listFiles().sortedWith(
            compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase() ?: "" }
        )
        for (child in children) {
            val name = child.name ?: continue
            val relPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
            val isDir = child.isDirectory
            val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
            val node = ProjectFileNode(
                name = name,
                path = relPath,
                isDirectory = isDir,
                extension = ext,
                isExpanded = true
            )
            if (isDir) {
                populateDocumentFileTree(child, relPath, node)
            }
            parentNode.children.add(node)
        }
    }

    suspend fun readFile(project: Project, relativePath: String): String = withContext(Dispatchers.IO) {
        if (project.isExternalFolder) {
            val treeUri = Uri.parse(project.rootPath)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext ""
            val doc = findDocumentByPath(rootDoc, relativePath) ?: return@withContext ""
            context.contentResolver.openInputStream(doc.uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""
        } else {
            val file = File(project.rootPath, relativePath)
            if (file.exists()) file.readText(Charsets.UTF_8) else ""
        }
    }

    suspend fun writeFile(project: Project, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (project.isExternalFolder) {
                val treeUri = Uri.parse(project.rootPath)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                val doc = findDocumentByPath(rootDoc, relativePath) ?: return@withContext false
                context.contentResolver.openOutputStream(doc.uri, "wt")?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } else {
                val file = File(project.rootPath, relativePath)
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createFile(project: Project, parentRelPath: String, fileName: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetRelPath = if (parentRelPath.isEmpty()) fileName else "$parentRelPath/$fileName"
            if (project.isExternalFolder) {
                val treeUri = Uri.parse(project.rootPath)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                val parentDoc = if (parentRelPath.isEmpty()) rootDoc else findDocumentByPath(rootDoc, parentRelPath) ?: return@withContext false
                val mime = getMimeType(fileName)
                val newDoc = parentDoc.createFile(mime, fileName) ?: return@withContext false
                context.contentResolver.openOutputStream(newDoc.uri, "wt")?.use {
                    it.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } else {
                val file = File(project.rootPath, targetRelPath)
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createFolder(project: Project, parentRelPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetRelPath = if (parentRelPath.isEmpty()) folderName else "$parentRelPath/$folderName"
            if (project.isExternalFolder) {
                val treeUri = Uri.parse(project.rootPath)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                val parentDoc = if (parentRelPath.isEmpty()) rootDoc else findDocumentByPath(rootDoc, parentRelPath) ?: return@withContext false
                parentDoc.createDirectory(folderName) != null
            } else {
                val folder = File(project.rootPath, targetRelPath)
                folder.mkdirs()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteItem(project: Project, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (project.isExternalFolder) {
                val treeUri = Uri.parse(project.rootPath)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                val doc = findDocumentByPath(rootDoc, relativePath) ?: return@withContext false
                doc.delete()
            } else {
                val file = File(project.rootPath, relativePath)
                file.deleteRecursively()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getAllProjectFiles(project: Project): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        if (project.isExternalFolder) {
            val treeUri = Uri.parse(project.rootPath)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null) {
                collectDocumentFiles(rootDoc, "", result)
            }
        } else {
            val rootDir = File(project.rootPath)
            if (rootDir.exists()) {
                collectLocalFiles(rootDir, "", result)
            }
        }
        result
    }

    private fun collectLocalFiles(dir: File, currentRelPath: String, acc: MutableMap<String, String>) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith(".git") && f.isDirectory) continue
            val rel = if (currentRelPath.isEmpty()) f.name else "$currentRelPath/${f.name}"
            if (f.isDirectory) {
                collectLocalFiles(f, rel, acc)
            } else if (f.isFile && f.length() < 2_000_000) {
                try {
                    acc[rel] = f.readText(Charsets.UTF_8)
                } catch (_: Exception) {}
            }
        }
    }

    private fun collectDocumentFiles(doc: DocumentFile, currentRelPath: String, acc: MutableMap<String, String>) {
        val children = doc.listFiles()
        for (child in children) {
            val name = child.name ?: continue
            val rel = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
            if (child.isDirectory) {
                collectDocumentFiles(child, rel, acc)
            } else if (child.isFile && child.length() < 2_000_000) {
                try {
                    context.contentResolver.openInputStream(child.uri)?.use { stream ->
                        acc[rel] = stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun exportProjectToZip(project: Project, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                val allFiles = getAllProjectFiles(project)
                for ((relPath, content) in allFiles) {
                    val entry = ZipEntry(relPath)
                    zipOut.putNextEntry(entry)
                    zipOut.write(content.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportProjectToFolder(project: Project, destinationTreeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val destRoot = DocumentFile.fromTreeUri(context, destinationTreeUri) ?: return@withContext false
            val allFiles = getAllProjectFiles(project)
            for ((relPath, content) in allFiles) {
                val parts = relPath.split('/')
                var currentDir = destRoot
                for (i in 0 until parts.size - 1) {
                    val dirName = parts[i]
                    val existing = currentDir.findFile(dirName)
                    currentDir = if (existing != null && existing.isDirectory) {
                        existing
                    } else {
                        currentDir.createDirectory(dirName) ?: currentDir
                    }
                }
                val fileName = parts.last()
                val mime = getMimeType(fileName)
                val existingFile = currentDir.findFile(fileName)
                val targetDoc = existingFile ?: currentDir.createFile(mime, fileName)
                if (targetDoc != null) {
                    context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importProjectFromZip(name: String, zipInputStream: ZipInputStream): Project = withContext(Dispatchers.IO) {
        val safeName = name.trim().replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val id = UUID.randomUUID().toString().take(8)
        val projectFolder = File(projectsDir, "${safeName}_$id").apply { mkdirs() }

        // Find root folder prefix if zip wrapped everything in a top-level folder
        var entry = zipInputStream.nextEntry
        val entries = mutableListOf<Pair<String, ByteArray>>()

        while (entry != null) {
            if (!entry.isDirectory) {
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
                entries.add(Pair(entry.name, out.toByteArray()))
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }

        // Determine if there is a common single root folder (like GitHub repo zipballs: "repo-main/...")
        val firstSlash = entries.firstOrNull()?.first?.indexOf('/') ?: -1
        val hasCommonRoot = firstSlash != -1 && entries.all { it.first.indexOf('/') == firstSlash }
        val rootPrefix = if (hasCommonRoot) entries.first().first.substring(0, firstSlash + 1) else ""

        for ((entryPath, bytes) in entries) {
            val cleanPath = entryPath.removePrefix(rootPrefix)
            if (cleanPath.isEmpty()) continue
            val destFile = File(projectFolder, cleanPath)
            destFile.parentFile?.mkdirs()
            destFile.writeBytes(bytes)
        }

        val project = Project(
            id = id,
            name = name.trim(),
            rootPath = projectFolder.absolutePath,
            isExternalFolder = false,
            gitBranch = "main",
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis()
        )

        saveProjectMetadata(project)
        project
    }

    private fun findDocumentByPath(root: DocumentFile, relativePath: String): DocumentFile? {
        val parts = relativePath.split('/').filter { it.isNotEmpty() }
        var current: DocumentFile = root
        for (part in parts) {
            val child = current.findFile(part) ?: return null
            current = child
        }
        return current
    }

    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "mjs" -> "application/javascript"
            "ts", "tsx" -> "text/plain"
            "json" -> "application/json"
            "md" -> "text/markdown"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            else -> "text/plain"
        }
    }

    private fun getTemplateFiles(template: ProjectTemplateType, projectName: String): Map<String, String> {
        return when (template) {
            ProjectTemplateType.WEB_APP -> mapOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$projectName</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div class="container">
    <div class="card">
      <span class="badge">VSC Web Project</span>
      <h1 id="title">$projectName</h1>
      <p id="counter-text">Counter initialized...</p>
      <div class="actions">
        <button id="btn-increment" class="btn btn-primary">Increment Count</button>
        <button id="btn-reset" class="btn btn-secondary">Reset</button>
      </div>
    </div>
  </div>
  <script src="app.ts"></script>
</body>
</html>
""".trimIndent(),
                "styles.css" to """body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: #090D16;
  color: #E2E8F0;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

.container {
  padding: 20px;
  max-width: 480px;
  width: 100%;
}

.card {
  background: #111827;
  border: 1px solid #1F2937;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
}

.badge {
  background: rgba(56, 189, 248, 0.15);
  color: #38BDF8;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 9999px;
  text-transform: uppercase;
}

h1 {
  font-size: 26px;
  margin-top: 12px;
  color: #F8FAFC;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn {
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary { background: #38BDF8; color: #0F172A; }
.btn-secondary { background: #334155; color: #F8FAFC; }
""".trimIndent(),
                "app.ts" to """// TypeScript Component - Auto-compiled in Texidor
interface CounterState {
  count: number;
  lastUpdated: string;
}

class CounterApp {
  private state: CounterState = {
    count: 0,
    lastUpdated: new Date().toLocaleTimeString()
  };

  constructor() {
    this.render();
    this.bindEvents();
    console.log("Texidor TypeScript Web App Initialized!");
  }

  private bindEvents(): void {
    const incBtn = document.getElementById("btn-increment");
    const resetBtn = document.getElementById("btn-reset");

    incBtn?.addEventListener("click", () => this.increment());
    resetBtn?.addEventListener("click", () => this.reset());
  }

  public increment(): void {
    this.state.count++;
    this.state.lastUpdated = new Date().toLocaleTimeString();
    this.render();
    console.log(`Counter value: ${'$'}{this.state.count}`);
  }

  public reset(): void {
    this.state.count = 0;
    this.state.lastUpdated = new Date().toLocaleTimeString();
    this.render();
    console.warn("Counter reset to 0");
  }

  private render(): void {
    const textEl = document.getElementById("counter-text");
    if (textEl) {
      textEl.innerHTML = `Current Count: <strong>${'$'}{this.state.count}</strong> (Updated: ${'$'}{this.state.lastUpdated})`;
    }
  }
}

window.addEventListener("DOMContentLoaded", () => {
  new CounterApp();
});
""".trimIndent(),
                "README.md" to """# $projectName
Built with **Texidor VSC-like Studio**.
- `index.html` - Semantic HTML markup
- `styles.css` - Modern dark responsive styling
- `app.ts` - TypeScript component with auto-compile
""".trimIndent()
            )

            ProjectTemplateType.NASM_ASSEMBLY -> mapOf(
                "main.asm" to """; Texidor x86-64 NASM Assembly Project
section .data
    msg db "Hello from x86-64 Assembly Project!", 10
    len equ $ - msg

section .text
    global _start

_start:
    ; sys_write(1, msg, len)
    mov rax, 1
    mov rdi, 1
    lea rsi, [msg]
    mov rdx, len
    syscall

    ; Computation
    mov rax, 10
    mov rbx, 32
    add rax, rbx

    ; sys_exit(42)
    mov rdi, rax
    mov rax, 60
    syscall
""".trimIndent(),
                "README.md" to "# $projectName\nNASM x86-64 project with virtual CPU emulation and disassembly inspection.\n".trimIndent()
            )

            ProjectTemplateType.MARKDOWN_DOCS -> mapOf(
                "index.md" to """# $projectName Documentation

Welcome to the project documentation workspace.

## Features
- [x] Full Markdown syntax highlighting
- [x] Live GitHub-flavored rendering
- [ ] Export to PDF & HTML

### Quick Reference
| Command | Action |
|---|---|
| Ctrl + R | Run / Preview |
| Ctrl + S | Save File |
| Ctrl + F | Search & Replace |
""".trimIndent(),
                "architecture.md" to "# System Architecture\nDetailed specification and design notes.\n".trimIndent()
            )

            ProjectTemplateType.NODE_JS -> mapOf(
                "package.json" to """{
  "name": "${projectName.lowercase()}",
  "version": "1.0.0",
  "description": "Texidor Node.js script",
  "main": "index.js",
  "scripts": {
    "start": "node index.js"
  }
}""".trimIndent(),
                "index.js" to """console.log("Texidor Node / JS runtime");
function computeFibonacci(n) {
  let a = 0, b = 1;
  for (let i = 0; i < n; i++) {
    const temp = a + b;
    a = b;
    b = temp;
  }
  return a;
}
console.log("Fib(10) =", computeFibonacci(10));
""".trimIndent()
            )

            ProjectTemplateType.REACT_TAILWIND -> mapOf(
                "index.html" to """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$projectName</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-slate-900 text-slate-100 min-h-screen flex items-center justify-center p-4">
  <div class="max-w-md w-full bg-slate-800 rounded-2xl p-6 border border-slate-700 shadow-2xl">
    <div class="text-xs font-bold text-sky-400 uppercase tracking-wider mb-2">React Component</div>
    <h1 class="text-2xl font-bold mb-4">$projectName</h1>
    <div id="root"></div>
  </div>
  <script src="app.js"></script>
</body>
</html>
""".trimIndent(),
                "app.js" to """document.getElementById('root').innerHTML = `
  <p class="text-slate-400 mb-4">Tailwind and React ready workspace in Texidor.</p>
  <button onclick="alert('Hello from Texidor!')" class="w-full bg-sky-500 hover:bg-sky-400 text-slate-900 font-bold py-2 px-4 rounded-lg transition">
    Interactive Action
  </button>
`;
""".trimIndent()
            )

            ProjectTemplateType.BLANK -> mapOf(
                "README.md" to "# $projectName\n".trimIndent()
            )
        }
    }

    private fun loadProjectsMetadata(): List<Project> {
        if (!metadataFile.exists()) return emptyList()
        return try {
            val json = metadataFile.readText(Charsets.UTF_8)
            val array = JSONArray(json)
            val list = mutableListOf<Project>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Project(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        rootPath = obj.getString("rootPath"),
                        isExternalFolder = obj.optBoolean("isExternalFolder", false),
                        gitBranch = obj.optString("gitBranch", "main").takeIf { it.isNotEmpty() },
                        gitRemoteUrl = obj.optString("gitRemoteUrl", "").takeIf { it.isNotEmpty() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastOpenedAt = obj.optLong("lastOpenedAt", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.lastOpenedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveProjectMetadata(project: Project) {
        val current = loadProjectsMetadata().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == project.id || it.rootPath == project.rootPath }
        if (existingIndex != -1) {
            current[existingIndex] = project.copy(lastOpenedAt = System.currentTimeMillis())
        } else {
            current.add(0, project)
        }

        try {
            val array = JSONArray()
            for (p in current) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("rootPath", p.rootPath)
                    put("isExternalFolder", p.isExternalFolder)
                    put("gitBranch", p.gitBranch ?: "")
                    put("gitRemoteUrl", p.gitRemoteUrl ?: "")
                    put("createdAt", p.createdAt)
                    put("lastOpenedAt", p.lastOpenedAt)
                }
                array.put(obj)
            }
            metadataFile.writeText(array.toString(), Charsets.UTF_8)
        } catch (_: Exception) {}
    }
}
