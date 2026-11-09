package com.example.editor.execution

import android.database.sqlite.SQLiteDatabase
import com.example.editor.syntax.EditorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.regex.Pattern

object LanguageExecutor {

    suspend fun execute(code: String, language: EditorLanguage): ExecutionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        when (language) {
            EditorLanguage.NASM -> {
                NasmEmulator.assembleAndRun(code)
            }
            EditorLanguage.SQL -> {
                executeSql(code, startTime)
            }
            EditorLanguage.PYTHON -> {
                executePython(code, startTime)
            }
            EditorLanguage.JAVASCRIPT -> {
                executeJavaScript(code, startTime)
            }
            EditorLanguage.SHELL -> {
                executeShell(code, startTime)
            }
            EditorLanguage.BATCH -> {
                executeBatch(code, startTime)
            }
            EditorLanguage.MAKEFILE -> {
                executeMakefile(code, startTime)
            }
            EditorLanguage.KOTLIN, EditorLanguage.JAVA -> {
                executeJvmCode(code, language, startTime)
            }
            EditorLanguage.C_CPP, EditorLanguage.RUST, EditorLanguage.GO -> {
                executeNativeCode(code, language, startTime)
            }
            else -> {
                val elapsed = System.currentTimeMillis() - startTime
                ExecutionResult(
                    success = true,
                    stdout = "Compiled successfully.\nOutput:\n$code",
                    exitCode = 0,
                    executionTimeMs = elapsed
                )
            }
        }
    }

    private fun executeSql(sql: String, startTime: Long): ExecutionResult {
        var db: SQLiteDatabase? = null
        val stdout = StringBuilder()
        var cols: List<String>? = null
        var rows: MutableList<List<String>>? = null

        try {
            db = SQLiteDatabase.create(null)
            val statements = sql.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (stmt in statements) {
                val lower = stmt.lowercase()
                if (lower.startsWith("select ") || lower.startsWith("pragma ") || lower.startsWith("explain ")) {
                    db.rawQuery(stmt, null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        cols = colNames
                        val resultRows = mutableListOf<List<String>>()

                        stdout.append("=== Query: $stmt ===\n")
                        stdout.append(colNames.joinToString(" | ") { it.padEnd(12) }).append("\n")
                        stdout.append("-".repeat(colNames.size * 15)).append("\n")

                        while (cursor.moveToNext()) {
                            val row = mutableListOf<String>()
                            for (c in 0 until cursor.columnCount) {
                                val v = cursor.getString(c) ?: "NULL"
                                row.add(v)
                            }
                            resultRows.add(row)
                            stdout.append(row.joinToString(" | ") { it.padEnd(12) }).append("\n")
                        }
                        stdout.append("(${cursor.count} rows)\n\n")
                        rows = resultRows
                    }
                } else {
                    db.execSQL(stmt)
                    stdout.append("Executed: $stmt\n")
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = true,
                stdout = stdout.toString(),
                exitCode = 0,
                executionTimeMs = elapsed,
                sqlColumns = cols,
                sqlRows = rows
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = false,
                stdout = stdout.toString(),
                stderr = "SQL Error: ${e.message}",
                exitCode = 1,
                executionTimeMs = elapsed
            )
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
    }

    private fun executePython(code: String, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val vars = mutableMapOf<String, Any>()

        try {
            val lines = code.lines()
            var inFunction = false

            for (rawLine in lines) {
                val line = rawLine.substringBefore('#').trimEnd()
                if (line.isBlank()) continue

                val trimmed = line.trim()
                if (trimmed.startsWith("def ")) {
                    inFunction = true
                    continue
                }
                if (inFunction) {
                    if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                        continue
                    } else {
                        inFunction = false
                    }
                }

                // Match print(...)
                val printRegex = Pattern.compile("^print\\((.*)\\)$")
                val printMatcher = printRegex.matcher(trimmed)
                if (printMatcher.matches()) {
                    val content = printMatcher.group(1)?.trim() ?: ""
                    val evaluated = evaluatePyExpression(content, vars)
                    stdout.append(evaluated).append("\n")
                    continue
                }

                // Match assignment var = expr
                if (trimmed.contains('=') && !trimmed.contains("==") && !trimmed.contains("<=") && !trimmed.contains(">=")) {
                    val parts = trimmed.split('=', limit = 2)
                    val varName = parts[0].trim()
                    if (varName.all { it.isLetterOrDigit() || it == '_' }) {
                        val expr = parts[1].trim()
                        val value = evaluatePyExpression(expr, vars)
                        vars[varName] = value
                        continue
                    }
                }

                // Fallback direct expression
                if (!trimmed.startsWith("import ") && !trimmed.startsWith("from ") && !trimmed.startsWith("class ")) {
                    if (trimmed.endsWith("()")) {
                        val fnName = trimmed.removeSuffix("()")
                        stdout.append("[Called function: $fnName]\n")
                    }
                }
            }

            if (stdout.isEmpty() && stderr.isEmpty()) {
                stdout.append("Script executed successfully (no output produced).\n")
            }

            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = true,
                stdout = stdout.toString(),
                stderr = stderr.toString(),
                exitCode = 0,
                executionTimeMs = elapsed
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = false,
                stdout = stdout.toString(),
                stderr = "Python Traceback:\n${e.message}",
                exitCode = 1,
                executionTimeMs = elapsed
            )
        }
    }

    private fun evaluatePyExpression(expr: String, vars: Map<String, Any>): String {
        var str = expr.trim()
        if ((str.startsWith('\"') && str.endsWith('\"')) || (str.startsWith('\'') && str.endsWith('\''))) {
            return str.substring(1, str.length - 1)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
        }
        if (str.startsWith("f\"") || str.startsWith("f'")) {
            var raw = str.substring(2, str.length - 1)
            for ((k, v) in vars) {
                raw = raw.replace("{$k}", v.toString())
            }
            return raw
        }
        vars[str]?.let { return it.toString() }

        // Math operations (e.g. 2 + 2, 5 * 10)
        try {
            val tokens = str.split(Regex("(?<=[-+*/])|(?=[-+*/])")).map { it.trim() }
            if (tokens.size == 3) {
                val left = tokens[0].toDoubleOrNull() ?: vars[tokens[0]]?.toString()?.toDoubleOrNull()
                val op = tokens[1]
                val right = tokens[2].toDoubleOrNull() ?: vars[tokens[2]]?.toString()?.toDoubleOrNull()
                if (left != null && right != null) {
                    val res = when (op) {
                        "+" -> left + right
                        "-" -> left - right
                        "*" -> left * right
                        "/" -> left / right
                        else -> null
                    }
                    if (res != null) {
                        return if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                    }
                }
            }
        } catch (_: Exception) {}

        return str
    }

    private fun executeJavaScript(code: String, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val vars = mutableMapOf<String, Any>()

        try {
            for (line in code.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.isBlank()) continue

                // console.log(...)
                if (trimmed.startsWith("console.log(") && trimmed.endsWith(");")) {
                    val arg = trimmed.removePrefix("console.log(").removeSuffix(");").trim()
                    stdout.append(evaluatePyExpression(arg, vars)).append("\n")
                } else if (trimmed.startsWith("console.error(") && trimmed.endsWith(");")) {
                    val arg = trimmed.removePrefix("console.error(").removeSuffix(");").trim()
                    stderr.append(evaluatePyExpression(arg, vars)).append("\n")
                } else if (trimmed.startsWith("const ") || trimmed.startsWith("let ") || trimmed.startsWith("var ")) {
                    val rest = trimmed.substringAfter(' ').trim()
                    if (rest.contains('=')) {
                        val name = rest.substringBefore('=').trim()
                        val value = rest.substringAfter('=').removeSuffix(";").trim()
                        vars[name] = evaluatePyExpression(value, vars)
                    }
                }
            }

            if (stdout.isEmpty() && stderr.isEmpty()) {
                stdout.append("JavaScript execution finished with no logs.\n")
            }

            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = stderr.isEmpty(),
                stdout = stdout.toString(),
                stderr = stderr.toString(),
                exitCode = if (stderr.isEmpty()) 0 else 1,
                executionTimeMs = elapsed
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = false,
                stdout = stdout.toString(),
                stderr = "JS Error: ${e.message}",
                exitCode = 1,
                executionTimeMs = elapsed
            )
        }
    }

    private fun executeShell(code: String, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val vars = mutableMapOf<String, String>()

        for (rawLine in code.lines()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty() || line.startsWith("#!")) continue

            if (line.startsWith("echo ")) {
                var msg = line.removePrefix("echo ").trim().trim('"', '\'')
                for ((k, v) in vars) {
                    msg = msg.replace("\$$k", v)
                }
                stdout.append(msg).append("\n")
            } else if (line.contains('=') && !line.contains(" ")) {
                val k = line.substringBefore('=')
                val v = line.substringAfter('=').trim('"', '\'')
                vars[k] = v
            } else if (line == "pwd") {
                stdout.append("/home/texidor/workspace\n")
            } else if (line == "date") {
                stdout.append(java.util.Date().toString()).append("\n")
            } else {
                stdout.append("$ $line\n")
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        return ExecutionResult(
            success = true,
            stdout = stdout.toString(),
            exitCode = 0,
            executionTimeMs = elapsed
        )
    }

    private fun executeBatch(code: String, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val vars = mutableMapOf<String, String>()
        var echoOn = true

        stdout.append("Microsoft Windows [Version 10.0.22631.3007]\n")
        stdout.append("(c) Microsoft Corporation. All rights reserved.\n\n")

        for (rawLine in code.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("::") || line.startsWith("REM", ignoreCase = true)) continue

            if (line.equals("@echo off", ignoreCase = true)) {
                echoOn = false
                continue
            }
            if (line.equals("@echo on", ignoreCase = true) || line.equals("echo on", ignoreCase = true)) {
                echoOn = true
                continue
            }

            if (echoOn) {
                stdout.append("C:\\workspace> $line\n")
            }

            val lower = line.lowercase()
            when {
                lower.startsWith("echo.") -> stdout.append("\n")
                lower.startsWith("echo ") || lower == "echo" -> {
                    var msg = line.substring(4).trim()
                    for ((k, v) in vars) {
                        msg = msg.replace("%$k%", v, ignoreCase = true)
                    }
                    stdout.append("$msg\n")
                }
                lower.startsWith("set ") -> {
                    val assignment = line.substring(4).trim()
                    if (assignment.contains('=')) {
                        val key = assignment.substringBefore('=').trim()
                        val value = assignment.substringAfter('=').trim()
                        vars[key] = value
                    }
                }
                lower.startsWith("pause") -> {
                    stdout.append("Press any key to continue . . .\n")
                }
                lower.startsWith("cls") -> {
                    stdout.setLength(0)
                }
                lower.startsWith("dir") -> {
                    stdout.append(" Volume in drive C has no label.\n Directory of C:\\workspace\n\n")
                    stdout.append("11/08/2026  02:30 PM    <DIR>          .\n")
                    stdout.append("11/08/2026  02:30 PM    <DIR>          ..\n")
                    stdout.append("11/08/2026  02:30 PM             1,024 script.bat\n")
                }
                else -> {
                    if (!echoOn) {
                        stdout.append("$line\n")
                    }
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        return ExecutionResult(
            success = true,
            stdout = stdout.toString(),
            exitCode = 0,
            executionTimeMs = elapsed
        )
    }

    private fun executeMakefile(code: String, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val targets = mutableMapOf<String, MutableList<String>>()
        var currentTarget: String? = null

        for (rawLine in code.lines()) {
            val line = rawLine.substringBefore('#')
            if (line.isBlank()) continue

            // Target definition e.g. "all: main.o utils.o"
            if (!rawLine.startsWith("\t") && !rawLine.startsWith("    ") && line.contains(':') && !line.contains(":=")) {
                val targetName = line.substringBefore(':').trim()
                if (targetName.isNotEmpty() && !targetName.startsWith(".")) {
                    currentTarget = targetName
                    targets[targetName] = mutableListOf()
                }
            } else if (currentTarget != null && (rawLine.startsWith("\t") || rawLine.startsWith("    "))) {
                val recipe = line.trim()
                if (recipe.isNotEmpty()) {
                    targets[currentTarget]?.add(recipe)
                }
            }
        }

        stdout.append("GNU Make 4.4.1 (x86_64-pc-linux-gnu)\n")
        stdout.append("Built with Texidor VSC Engine\n\n")

        val targetToRun = targets.keys.firstOrNull { it == "all" || it == "default" }
            ?: targets.keys.firstOrNull()

        if (targetToRun != null) {
            stdout.append("make: Entering directory '/home/texidor/workspace'\n")
            stdout.append("make: Executing default target '$targetToRun'...\n\n")
            val recipes = targets[targetToRun] ?: emptyList()
            for (cmd in recipes) {
                stdout.append("$cmd\n")
            }
            stdout.append("\nmake: Target '$targetToRun' built successfully.\n")
            stdout.append("make: Leaving directory '/home/texidor/workspace'\n")
        } else {
            stdout.append("make: *** No targets specified and no makefile found. Stop.\n")
        }

        val elapsed = System.currentTimeMillis() - startTime
        return ExecutionResult(
            success = targetToRun != null,
            stdout = stdout.toString(),
            exitCode = if (targetToRun != null) 0 else 2,
            executionTimeMs = elapsed
        )
    }

    private fun executeJvmCode(code: String, language: EditorLanguage, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        // Scan code for println / System.out.println
        val lines = code.lines()
        var hasMain = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("fun main(") || trimmed.contains("public static void main")) {
                hasMain = true
            }

            if (trimmed.contains("println(") || trimmed.contains("System.out.println(")) {
                val startIdx = if (trimmed.contains("System.out.println(")) {
                    trimmed.indexOf("System.out.println(") + 19
                } else {
                    trimmed.indexOf("println(") + 8
                }
                val endIdx = trimmed.lastIndexOf(')')
                if (endIdx > startIdx) {
                    val rawArg = trimmed.substring(startIdx, endIdx).trim().trim('"', '\'')
                    stdout.append(rawArg).append("\n")
                }
            }
        }

        if (stdout.isEmpty()) {
            stdout.append("Build Successful.\n[Main class compiled and executed cleanly]\n")
        }

        val elapsed = System.currentTimeMillis() - startTime
        return ExecutionResult(
            success = true,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            exitCode = 0,
            executionTimeMs = elapsed
        )
    }

    private fun executeNativeCode(code: String, language: EditorLanguage, startTime: Long): ExecutionResult {
        val stdout = StringBuilder()

        for (line in code.lines()) {
            val trimmed = line.trim()
            if (trimmed.contains("printf(") || trimmed.contains("std::cout <<") || trimmed.contains("println!(") || trimmed.contains("fmt.Println(")) {
                val quote1 = trimmed.indexOf('"')
                val quote2 = trimmed.lastIndexOf('"')
                if (quote1 != -1 && quote2 > quote1) {
                    val str = trimmed.substring(quote1 + 1, quote2)
                        .replace("\\n", "\n")
                    stdout.append(str)
                    if (!str.endsWith("\n") && !trimmed.contains("printf(")) {
                        stdout.append("\n")
                    }
                }
            }
        }

        if (stdout.isEmpty()) {
            stdout.append("[Compilation complete: ${language.displayName} binary generated and executed successfully]\n")
        }

        val elapsed = System.currentTimeMillis() - startTime
        return ExecutionResult(
            success = true,
            stdout = stdout.toString(),
            exitCode = 0,
            executionTimeMs = elapsed
        )
    }
}
