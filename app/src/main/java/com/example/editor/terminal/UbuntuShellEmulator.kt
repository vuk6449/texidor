package com.example.editor.terminal

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.editor.execution.LanguageExecutor
import com.example.editor.syntax.EditorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TerminalLine(
    val type: TerminalLineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalLineType {
    PROMPT,
    OUTPUT,
    SUCCESS,
    WARNING,
    ERROR,
    SYSTEM
}

class UbuntuShellEmulator(private val context: Context) {

    var currentDirectory: File = getInitialDirectory()
        private set

    private var previousDirectory: File = currentDirectory

    val installedPackages = mutableSetOf(
        "coreutils", "bash", "sed", "grep", "findutils", "git", "curl", "tar", "gzip", "make"
    )

    private val availableAptPackages = mapOf(
        "python3" to "Python 3.12.3 interactive interpreter & standard library",
        "nodejs" to "Node.js 20.12.0 evented I/O for V8 javascript engine",
        "gcc" to "GNU C compiler (Ubuntu 13.2.0-23ubuntu4)",
        "g++" to "GNU C++ compiler (Ubuntu 13.2.0-23ubuntu4)",
        "clang" to "C, C++ and Objective-C compiler (LLVM 18.1.3)",
        "nasm" to "Netwide Assembler (x86 & x86-64 assembly support)",
        "rustc" to "Rust systems programming language compiler 1.77.2",
        "neofetch" to "Fast, highly customizable system information tool",
        "tree" to "Display directory tree structure recursively",
        "htop" to "Interactive processes viewer for Linux",
        "vim" to "Vi IMproved - enhanced text editor",
        "nano" to "Small, friendly text editor inspired by Pico"
    )

    private val commandHistory = mutableListOf<String>()

    fun setWorkingDirectory(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            previousDirectory = currentDirectory
            currentDirectory = dir
        }
    }

    private fun getInitialDirectory(): File {
        val sdcard = File("/sdcard")
        if (sdcard.exists() && sdcard.canRead()) {
            return sdcard
        }
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists() && extStorage.canRead()) {
            return extStorage
        }
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) projectsDir.mkdirs()
        return projectsDir
    }

    suspend fun executeCommand(commandLine: String): List<TerminalLine> = withContext(Dispatchers.IO) {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        commandHistory.add(trimmed)
        val result = mutableListOf<TerminalLine>()
        result.add(TerminalLine(TerminalLineType.PROMPT, getPromptString() + trimmed))

        val parts = trimmed.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        when (cmd) {
            "clear", "cls" -> {
                // Handled in UI, but return empty
                return@withContext listOf(TerminalLine(TerminalLineType.SYSTEM, "__CLEAR__"))
            }

            "pwd" -> {
                result.add(TerminalLine(TerminalLineType.OUTPUT, currentDirectory.absolutePath))
            }

            "whoami" -> {
                result.add(TerminalLine(TerminalLineType.OUTPUT, "ubuntu"))
            }

            "uname" -> {
                if (args.contains("-a") || args.contains("-m") || args.isEmpty()) {
                    val arch = if (Build.SUPPORTED_ABIS.contains("arm64-v8a")) "aarch64" else "x86_64"
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Linux texidor-ubuntu 5.15.0-generic #1 SMP PREEMPT $arch GNU/Linux (Ubuntu 24.04 LTS)"))
                } else {
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Linux"))
                }
            }

            "date" -> {
                val fmt = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
                result.add(TerminalLine(TerminalLineType.OUTPUT, fmt.format(Date())))
            }

            "cd" -> {
                val target = args.firstOrNull() ?: "~"
                handleCd(target, result)
            }

            "ls" -> {
                handleLs(args, result)
            }

            "cat" -> {
                handleCat(args, result)
            }

            "mkdir" -> {
                handleMkdir(args, result)
            }

            "touch" -> {
                handleTouch(args, result)
            }

            "rm" -> {
                handleRm(args, result)
            }

            "echo" -> {
                val text = args.joinToString(" ").replace("\"", "")
                result.add(TerminalLine(TerminalLineType.OUTPUT, text))
            }

            "apt", "apt-get" -> {
                handleApt(args, result)
            }

            "make" -> {
                handleMake(args, result)
            }

            "sh", "bash" -> {
                handleScriptExecution(args, result)
            }

            "tree" -> {
                handleTree(result)
            }

            "neofetch", "texifetch" -> {
                handleNeofetch(result)
            }

            "help" -> {
                handleHelp(result)
            }

            "history" -> {
                commandHistory.forEachIndexed { idx, c ->
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "  ${idx + 1}  $c"))
                }
            }

            else -> {
                // Try executing real shell process first
                val processOutput = runProcess(trimmed)
                if (processOutput != null) {
                    result.addAll(processOutput)
                } else {
                    result.add(TerminalLine(TerminalLineType.ERROR, "bash: $cmd: command not found. Type 'help' or 'apt install $cmd'."))
                }
            }
        }

        result
    }

    private fun handleCd(target: String, result: MutableList<TerminalLine>) {
        val newDir = when (target) {
            "~", "" -> File(context.filesDir, "projects").apply { mkdirs() }
            "-" -> previousDirectory
            ".." -> currentDirectory.parentFile ?: currentDirectory
            else -> {
                if (target.startsWith("/")) {
                    File(target)
                } else {
                    File(currentDirectory, target)
                }
            }
        }

        if (newDir.exists() && newDir.isDirectory) {
            previousDirectory = currentDirectory
            currentDirectory = newDir
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "bash: cd: $target: No such file or directory"))
        }
    }

    private fun handleLs(args: List<String>, result: MutableList<TerminalLine>) {
        val files = currentDirectory.listFiles()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList<File>()

        val isLongFormat = args.contains("-l") || args.contains("-la") || args.contains("-al")
        val showHidden = args.contains("-a") || args.contains("-la") || args.contains("-al")

        if (isLongFormat) {
            result.add(TerminalLine(TerminalLineType.OUTPUT, "total ${files.size * 4}"))
            val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)
            for (f in files) {
                if (!showHidden && f.name.startsWith(".")) continue
                val perms = if (f.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                val size = if (f.isDirectory) "4096".padStart(6) else f.length().toString().padStart(6)
                val dateStr = dateFormat.format(Date(f.lastModified()))
                val colorType = if (f.isDirectory) TerminalLineType.SUCCESS else TerminalLineType.OUTPUT
                result.add(TerminalLine(colorType, "$perms 1 ubuntu ubuntu $size $dateStr ${f.name}"))
            }
        } else {
            val names = files.filter { showHidden || !it.name.startsWith(".") }.map {
                if (it.isDirectory) "${it.name}/" else it.name
            }
            if (names.isNotEmpty()) {
                result.add(TerminalLine(TerminalLineType.OUTPUT, names.joinToString("   ")))
            }
        }
    }

    private fun handleCat(args: List<String>, result: MutableList<TerminalLine>) {
        val fileName = args.firstOrNull()
        if (fileName == null) {
            result.add(TerminalLine(TerminalLineType.ERROR, "cat: missing file operand"))
            return
        }
        val file = File(currentDirectory, fileName)
        if (file.exists() && file.isFile) {
            if (file.length() > 500_000) {
                result.add(TerminalLine(TerminalLineType.WARNING, "[File is larger than 500KB - displaying first 200 lines]"))
                file.useLines { lines ->
                    lines.take(200).forEach { line ->
                        result.add(TerminalLine(TerminalLineType.OUTPUT, line))
                    }
                }
            } else {
                result.add(TerminalLine(TerminalLineType.OUTPUT, file.readText(Charsets.UTF_8)))
            }
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "cat: $fileName: No such file or directory"))
        }
    }

    private fun handleMkdir(args: List<String>, result: MutableList<TerminalLine>) {
        val name = args.firstOrNull()
        if (name == null) {
            result.add(TerminalLine(TerminalLineType.ERROR, "mkdir: missing operand"))
            return
        }
        val folder = File(currentDirectory, name)
        if (folder.mkdirs() || folder.exists()) {
            result.add(TerminalLine(TerminalLineType.SUCCESS, "Created directory: $name"))
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "mkdir: cannot create directory '$name': Permission denied"))
        }
    }

    private fun handleTouch(args: List<String>, result: MutableList<TerminalLine>) {
        val name = args.firstOrNull()
        if (name == null) {
            result.add(TerminalLine(TerminalLineType.ERROR, "touch: missing file operand"))
            return
        }
        val file = File(currentDirectory, name)
        try {
            file.createNewFile()
            result.add(TerminalLine(TerminalLineType.SUCCESS, "Created file: $name"))
        } catch (e: Exception) {
            result.add(TerminalLine(TerminalLineType.ERROR, "touch: cannot touch '$name': ${e.message}"))
        }
    }

    private fun handleRm(args: List<String>, result: MutableList<TerminalLine>) {
        val name = args.lastOrNull { !it.startsWith("-") }
        if (name == null) {
            result.add(TerminalLine(TerminalLineType.ERROR, "rm: missing operand"))
            return
        }
        val file = File(currentDirectory, name)
        if (file.exists()) {
            file.deleteRecursively()
            result.add(TerminalLine(TerminalLineType.SUCCESS, "Removed: $name"))
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "rm: cannot remove '$name': No such file or directory"))
        }
    }

    private fun handleApt(args: List<String>, result: MutableList<TerminalLine>) {
        val subcmd = args.firstOrNull()?.lowercase() ?: "help"
        when (subcmd) {
            "update" -> {
                result.add(TerminalLine(TerminalLineType.OUTPUT, "Hit:1 http://archive.ubuntu.com/ubuntu noble InRelease"))
                result.add(TerminalLine(TerminalLineType.OUTPUT, "Hit:2 http://security.ubuntu.com/ubuntu noble-security InRelease"))
                result.add(TerminalLine(TerminalLineType.OUTPUT, "Reading package lists... Done"))
                result.add(TerminalLine(TerminalLineType.SUCCESS, "Building dependency tree... Done\nAll packages are up to date."))
            }

            "install" -> {
                val pkgName = args.getOrNull(1)?.lowercase()
                if (pkgName == null) {
                    result.add(TerminalLine(TerminalLineType.ERROR, "apt: missing package name. Example: 'apt install nasm'"))
                    return
                }
                if (installedPackages.contains(pkgName)) {
                    result.add(TerminalLine(TerminalLineType.WARNING, "$pkgName is already the newest version."))
                    return
                }
                val desc = availableAptPackages[pkgName]
                if (desc != null) {
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Reading package lists... Done"))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Building dependency tree... Done"))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "The following NEW packages will be installed: $pkgName"))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Get:1 http://archive.ubuntu.com/ubuntu noble/main $pkgName [1,420 kB]"))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Fetched 1,420 kB in 1s (1,230 kB/s)"))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Selecting previously unselected package $pkgName."))
                    result.add(TerminalLine(TerminalLineType.OUTPUT, "Setting up $pkgName ..."))
                    installedPackages.add(pkgName)
                    result.add(TerminalLine(TerminalLineType.SUCCESS, "Setting up $pkgName complete: $desc"))
                } else {
                    result.add(TerminalLine(TerminalLineType.ERROR, "E: Unable to locate package $pkgName. Try: ${availableAptPackages.keys.joinToString(", ")}"))
                }
            }

            "list" -> {
                val showOnlyInstalled = args.contains("--installed")
                result.add(TerminalLine(TerminalLineType.OUTPUT, "Listing packages..."))
                availableAptPackages.forEach { (name, desc) ->
                    val isInst = installedPackages.contains(name)
                    if (!showOnlyInstalled || isInst) {
                        val status = if (isInst) "[installed]" else "[available]"
                        val lineType = if (isInst) TerminalLineType.SUCCESS else TerminalLineType.OUTPUT
                        result.add(TerminalLine(lineType, "$name/noble $status - $desc"))
                    }
                }
            }

            "remove" -> {
                val pkgName = args.getOrNull(1)?.lowercase()
                if (pkgName != null && installedPackages.remove(pkgName)) {
                    result.add(TerminalLine(TerminalLineType.SUCCESS, "Removed $pkgName successfully."))
                } else {
                    result.add(TerminalLine(TerminalLineType.ERROR, "Package is not installed."))
                }
            }

            else -> {
                result.add(TerminalLine(TerminalLineType.OUTPUT, "apt commands: update, install <pkg>, list [--installed], remove <pkg>"))
            }
        }
    }

    private suspend fun handleMake(args: List<String>, result: MutableList<TerminalLine>) {
        val makefile = File(currentDirectory, "Makefile").takeIf { it.exists() }
            ?: File(currentDirectory, "makefile").takeIf { it.exists() }

        if (makefile != null) {
            val content = makefile.readText(Charsets.UTF_8)
            val execResult = LanguageExecutor.execute(content, EditorLanguage.MAKEFILE)
            result.add(TerminalLine(TerminalLineType.OUTPUT, execResult.stdout))
            if (execResult.stderr.isNotEmpty()) {
                result.add(TerminalLine(TerminalLineType.ERROR, execResult.stderr))
            }
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "make: *** No targets specified and no makefile found in ${currentDirectory.name}. Stop."))
        }
    }

    private suspend fun handleScriptExecution(args: List<String>, result: MutableList<TerminalLine>) {
        val scriptName = args.firstOrNull()
        if (scriptName == null) {
            result.add(TerminalLine(TerminalLineType.ERROR, "bash: script name required"))
            return
        }
        val file = File(currentDirectory, scriptName)
        if (file.exists() && file.isFile) {
            val content = file.readText(Charsets.UTF_8)
            val execResult = LanguageExecutor.execute(content, EditorLanguage.SHELL)
            result.add(TerminalLine(TerminalLineType.OUTPUT, execResult.stdout))
            if (execResult.stderr.isNotEmpty()) {
                result.add(TerminalLine(TerminalLineType.ERROR, execResult.stderr))
            }
        } else {
            result.add(TerminalLine(TerminalLineType.ERROR, "bash: $scriptName: No such file or directory"))
        }
    }

    private fun handleTree(result: MutableList<TerminalLine>) {
        result.add(TerminalLine(TerminalLineType.SUCCESS, currentDirectory.name))
        buildTreeLines(currentDirectory, "", result, 0)
    }

    private fun buildTreeLines(dir: File, prefix: String, result: MutableList<TerminalLine>, depth: Int) {
        if (depth > 4) return
        val files = dir.listFiles()?.filter { !it.name.startsWith(".") }?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: return

        for ((index, file) in files.withIndex()) {
            val isLast = index == files.size - 1
            val connector = if (isLast) "└── " else "├── "
            val lineType = if (file.isDirectory) TerminalLineType.SUCCESS else TerminalLineType.OUTPUT
            result.add(TerminalLine(lineType, "$prefix$connector${file.name}${if (file.isDirectory) "/" else ""}"))
            if (file.isDirectory) {
                val newPrefix = prefix + if (isLast) "    " else "│   "
                buildTreeLines(file, newPrefix, result, depth + 1)
            }
        }
    }

    private fun handleNeofetch(result: MutableList<TerminalLine>) {
        val arch = if (Build.SUPPORTED_ABIS.contains("arm64-v8a")) "aarch64" else "x86_64"
        val art = """
            .-.           ubuntu@texidor
           .-'   '-.       --------------
          /         \      OS: Ubuntu 24.04 LTS ($arch)
         |  (o) (o)  |     Host: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
         |     >     |     Kernel: 5.15.0-generic
          \   ---   /      Uptime: 4 days, 12 hours
           '-.....-'       Packages: ${installedPackages.size} (apt)
                           Shell: bash 5.2.21
                           Terminal: Texidor-Integrated-VSC
                           CPU: ${Build.HARDWARE} (${Runtime.getRuntime().availableProcessors()} cores)
                           Memory: ${Runtime.getRuntime().freeMemory() / (1024 * 1024)}MB / ${Runtime.getRuntime().totalMemory() / (1024 * 1024)}MB
        """.trimIndent()
        result.add(TerminalLine(TerminalLineType.SUCCESS, art))
    }

    private fun handleHelp(result: MutableList<TerminalLine>) {
        result.add(TerminalLine(TerminalLineType.SUCCESS, "Texidor Integrated Ubuntu Terminal (Termux Edition)"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "Built-in Commands:"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  ls [-la]       - List directory contents"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  cd <dir>       - Change directory (~, .., -, /sdcard)"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  pwd            - Print working directory"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  cat <file>     - Display file contents"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  mkdir <dir>    - Create new directory"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  touch <file>   - Create new empty file"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  rm <file/dir>  - Remove file or folder"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  apt install    - Install packages (nasm, gcc, nodejs, python3, rustc, etc.)"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  apt list       - List available and installed packages"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  make [target]  - Execute Makefile recipes in current directory"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  sh <file.sh>   - Run shell script"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  tree           - Recursive directory tree"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  neofetch       - Display system specifications & logo"))
        result.add(TerminalLine(TerminalLineType.OUTPUT, "  clear          - Clear terminal screen"))
    }

    private fun runProcess(commandLine: String): List<TerminalLine>? {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", commandLine),
                null,
                currentDirectory
            )
            val output = mutableListOf<TerminalLine>()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lines().limit(100).forEach { line ->
                    output.add(TerminalLine(TerminalLineType.OUTPUT, line))
                }
            }
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.lines().limit(50).forEach { line ->
                    output.add(TerminalLine(TerminalLineType.ERROR, line))
                }
            }
            process.waitFor()
            if (output.isEmpty() && process.exitValue() == 0) {
                listOf(TerminalLine(TerminalLineType.SUCCESS, "[Command exited with status 0]"))
            } else {
                output
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getPromptString(): String {
        val relPath = when {
            currentDirectory.absolutePath.startsWith(context.filesDir.absolutePath) -> {
                "~" + currentDirectory.absolutePath.removePrefix(context.filesDir.absolutePath)
            }
            currentDirectory.absolutePath.startsWith("/sdcard") -> {
                currentDirectory.absolutePath
            }
            else -> currentDirectory.name
        }
        return "ubuntu@texidor:$relPath$ "
    }
}
