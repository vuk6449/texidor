package com.example.editor.ts

data class TranspileResult(
    val javascript: String,
    val success: Boolean,
    val compilationTimeMs: Long,
    val diagnostics: List<String> = emptyList()
)

object TypeScriptCompiler {

    /**
     * Fast, robust on-device TypeScript to ECMAScript transpiler.
     * Removes interfaces, type aliases, access modifiers, type casts,
     * type annotations, generics, and transpiles enums into standard JS objects.
     */
    fun transpile(tsCode: String): TranspileResult {
        val startTime = System.currentTimeMillis()
        val diagnostics = mutableListOf<String>()

        try {
            var code = tsCode

            // 1. Transpile enums
            code = transpileEnums(code)

            // 2. Remove interface declarations
            code = removeInterfaces(code)

            // 3. Remove type alias declarations: type X<T> = ...;
            code = removeTypeAliases(code)

            // 4. Remove declare statements
            code = code.replace(Regex("(?m)^\\s*declare\\s+.*?;"), "")

            // 5. Remove 'implements Interface' from class definitions
            code = code.replace(Regex("\\bclass\\s+([A-Za-z0-9_$]+)(?:<[^>]+>)?(?:\\s+extends\\s+[A-Za-z0-9_$.]+(?:<[^>]+>)?)?\\s+implements\\s+[^{]+"), "class $1")

            // 6. Remove TypeScript access modifiers and readonly from class fields/methods
            code = code.replace(Regex("\\b(public|private|protected|readonly|override|abstract)\\s+"), "")

            // 7. Remove type casts: (x as Type), (x as const), <Type>x
            code = code.replace(Regex("\\s+as\\s+(?:const|[A-Za-z0-9_$.\\[\\]<>,\\s|&]+)"), "")
            code = code.replace(Regex("<[A-Za-z0-9_$.\\[\\]\\s|&]+>(?=[A-Za-z0-9_$(])"), "")

            // 8. Remove non-null assertion operators: expr!.prop -> expr.prop
            code = code.replace(Regex("([A-Za-z0-9_$])!(?=[.\\[(])"), "$1")

            // 9. Remove function generic parameters: function foo<T, U>(... -> function foo(...
            code = code.replace(Regex("(\\bfunction\\s+[A-Za-z0-9_$]+)\\s*<[A-Za-z0-9_$,\\s\\textends]+>"), "$1")
            code = code.replace(Regex("(\\b[A-Za-z0-9_$]+)\\s*<[A-Za-z0-9_$,\\s\\textends]+>\\s*(?=\\()"), "$1")

            // 10. Remove function/arrow return type annotations: ): ReturnType { or ): ReturnType =>
            code = code.replace(Regex("\\)\\s*:\\s*(?:Promise<[^>]+>|[A-Za-z0-9_$.\\[\\]|&\\s]+?)(?=\\s*[{=])"), ") ")

            // 11. Remove parameter type annotations: (a: string, b?: number = 1) -> (a, b = 1)
            code = cleanParameterTypes(code)

            // 12. Remove variable type annotations: const x: string = "a" -> const x = "a"
            code = code.replace(Regex("\\b(const|let|var)\\s+([A-Za-z0-9_$]+)\\s*:\\s*[A-Za-z0-9_$.\\[\\]<>,\\s|&]+?\\s*="), "$1 $2 =")

            // 13. Remove class property type annotations: prop: string = val; or prop: string;
            code = code.replace(Regex("(?m)^(\\s*)([A-Za-z0-9_$]+)\\s*:\\s*[A-Za-z0-9_$.\\[\\]<>,\\s|&]+?\\s*="), "$1$2 =")
            code = code.replace(Regex("(?m)^(\\s*)([A-Za-z0-9_$]+)\\s*:\\s*[A-Za-z0-9_$.\\[\\]<>,\\s|&]+?\\s*;"), "$1$2 = undefined;")

            val elapsed = System.currentTimeMillis() - startTime
            return TranspileResult(
                javascript = code.trim(),
                success = true,
                compilationTimeMs = elapsed,
                diagnostics = diagnostics
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return TranspileResult(
                javascript = tsCode,
                success = false,
                compilationTimeMs = elapsed,
                diagnostics = listOf("Transpilation warning: ${e.message}")
            )
        }
    }

    private fun transpileEnums(source: String): String {
        val enumRegex = Regex("\\benum\\s+([A-Za-z0-9_$]+)\\s*\\{([^}]+)\\}")
        return enumRegex.replace(source) { match ->
            val enumName = match.groupValues[1]
            val body = match.groupValues[2]

            val members = body.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val jsAssignments = StringBuilder()
            jsAssignments.append("const $enumName = {};\n")

            var autoIndex = 0
            for (member in members) {
                if (member.contains('=')) {
                    val parts = member.split('=').map { it.trim() }
                    val key = parts[0]
                    val value = parts[1]
                    jsAssignments.append("$enumName[\"$key\"] = $value;\n")
                    val parsedInt = value.toIntOrNull()
                    if (parsedInt != null) {
                        jsAssignments.append("$enumName[$value] = \"$key\";\n")
                        autoIndex = parsedInt + 1
                    }
                } else {
                    val key = member
                    jsAssignments.append("$enumName[\"$key\"] = $autoIndex;\n")
                    jsAssignments.append("$enumName[$autoIndex] = \"$key\";\n")
                    autoIndex++
                }
            }
            jsAssignments.append("Object.freeze($enumName);")
            jsAssignments.toString()
        }
    }

    private fun removeInterfaces(source: String): String {
        // Matches interface Name<T> { ... } with balanced braces
        val sb = StringBuilder()
        var i = 0
        val len = source.length

        while (i < len) {
            if (source.startsWith("interface ", i) || source.startsWith("interface\t", i) || source.startsWith("interface\n", i)) {
                // Find opening brace '{'
                val openBrace = source.indexOf('{', i)
                if (openBrace != -1) {
                    var depth = 1
                    var j = openBrace + 1
                    while (j < len && depth > 0) {
                        if (source[j] == '{') depth++
                        else if (source[j] == '}') depth--
                        j++
                    }
                    i = j
                    // Skip optional trailing semicolon or newline
                    while (i < len && (source[i] == ';' || source[i] == ' ' || source[i] == '\t')) i++
                    continue
                }
            }
            sb.append(source[i])
            i++
        }
        return sb.toString()
    }

    private fun removeTypeAliases(source: String): String {
        // Matches: type Name<T> = ...;
        return source.replace(Regex("(?m)^\\s*type\\s+[A-Za-z0-9_$]+(?:<[^>]+>)?\\s*=[^;]+;\\s*"), "")
    }

    private fun cleanParameterTypes(source: String): String {
        // Matches parameter lists like (a: number, b?: string, c = 2)
        val paramListRegex = Regex("\\(([^)]*)\\)")
        return paramListRegex.replace(source) { match ->
            val inside = match.groupValues[1]
            if (inside.isBlank()) return@replace match.value

            val params = inside.split(',').map { param ->
                var p = param.trim()
                // optional marker: a?: string -> a: string
                p = p.replace(Regex("([A-Za-z0-9_$]+)\\?\\s*:"), "$1:")
                // strip type: a: number = default -> a = default
                if (p.contains(':')) {
                    val colonIdx = p.indexOf(':')
                    val name = p.substring(0, colonIdx).trim()
                    val rest = p.substring(colonIdx + 1).trim()
                    if (rest.contains('=')) {
                        val eqIdx = rest.indexOf('=')
                        val defaultVal = rest.substring(eqIdx)
                        "$name $defaultVal"
                    } else {
                        name
                    }
                } else {
                    p
                }
            }
            "(" + params.joinToString(", ") + ")"
        }
    }
}
