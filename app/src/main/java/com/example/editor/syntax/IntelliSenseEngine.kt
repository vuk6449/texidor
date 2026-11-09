package com.example.editor.syntax

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.util.regex.Pattern

enum class CompletionKind {
    KEYWORD,
    FUNCTION,
    TYPE,
    SNIPPET,
    VARIABLE
}

data class CompletionItem(
    val label: String,
    val insertText: String,
    val kind: CompletionKind,
    val detail: String? = null,
    val cursorOffsetAfterInsert: Int = insertText.length
)

object IntelliSenseEngine {

    /**
     * Extracts prefix before current cursor position.
     * e.g., for "val myVar = prin|" -> prefix is "prin", wordStart is index of 'p'.
     */
    fun getWordPrefix(text: String, cursorIndex: Int): Pair<String, Int> {
        if (cursorIndex <= 0 || cursorIndex > text.length) return Pair("", cursorIndex)

        var start = cursorIndex - 1
        while (start >= 0 && (text[start].isLetterOrDigit() || text[start] == '_' || text[start] == '$')) {
            start--
        }
        val wordStart = start + 1
        val prefix = text.substring(wordStart, cursorIndex)
        return Pair(prefix, wordStart)
    }

    /**
     * Finds autocomplete candidates based on language keywords, built-ins, snippets, and file identifiers.
     */
    fun getCompletions(
        code: String,
        cursorIndex: Int,
        language: EditorLanguage
    ): List<CompletionItem> {
        val (prefix, _) = getWordPrefix(code, cursorIndex)
        if (prefix.isEmpty()) {
            // If prefix is empty, suggest top language snippets
            return language.snippets.take(4).map {
                CompletionItem(
                    label = it.label,
                    insertText = it.insertText,
                    kind = CompletionKind.SNIPPET,
                    detail = it.description
                )
            }
        }

        val prefixLower = prefix.lowercase()
        val suggestions = mutableListOf<CompletionItem>()

        // 1. Snippets
        language.snippets.filter { it.label.lowercase().startsWith(prefixLower) }.forEach {
            suggestions.add(
                CompletionItem(
                    label = it.label,
                    insertText = it.insertText,
                    kind = CompletionKind.SNIPPET,
                    detail = it.description
                )
            )
        }

        // 2. Keywords
        language.keywords.filter { it.lowercase().startsWith(prefixLower) && it != prefix }.forEach {
            suggestions.add(
                CompletionItem(
                    label = it,
                    insertText = it,
                    kind = CompletionKind.KEYWORD,
                    detail = "keyword"
                )
            )
        }

        // 3. Built-in types
        language.builtInTypes.filter { it.lowercase().startsWith(prefixLower) && it != prefix }.forEach {
            suggestions.add(
                CompletionItem(
                    label = it,
                    insertText = it,
                    kind = CompletionKind.TYPE,
                    detail = "class/type"
                )
            )
        }

        // 4. Built-in functions
        language.builtInFunctions.filter { it.lowercase().startsWith(prefixLower) && it != prefix }.forEach {
            suggestions.add(
                CompletionItem(
                    label = "$it()",
                    insertText = "$it()",
                    kind = CompletionKind.FUNCTION,
                    detail = "standard api",
                    cursorOffsetAfterInsert = it.length + 1 // inside parenthesis
                )
            )
        }

        // 5. User identifiers from file
        val idRegex = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]{2,})\\b")
        val matcher = idRegex.matcher(code)
        val seenIds = mutableSetOf<String>()
        while (matcher.find()) {
            val id = matcher.group(1) ?: continue
            if (id != prefix && id.lowercase().startsWith(prefixLower) && seenIds.add(id)) {
                if (language.keywords.contains(id) || language.builtInTypes.contains(id)) continue
                suggestions.add(
                    CompletionItem(
                        label = id,
                        insertText = id,
                        kind = CompletionKind.VARIABLE,
                        detail = "identifier"
                    )
                )
            }
        }

        return suggestions.distinctBy { it.label }.take(8)
    }

    /**
     * Applies the chosen completion into the TextFieldValue.
     */
    fun applyCompletion(
        currentValue: TextFieldValue,
        completion: CompletionItem
    ): TextFieldValue {
        val cursor = currentValue.selection.end
        val text = currentValue.text
        val (prefix, wordStart) = getWordPrefix(text, cursor)

        val newText = text.substring(0, wordStart) + completion.insertText + text.substring(cursor)
        val newCursor = wordStart + completion.cursorOffsetAfterInsert

        return currentValue.copy(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    /**
     * Smart indentation helper when Enter key is pressed.
     */
    fun handleEnterSmartIndent(
        currentValue: TextFieldValue,
        tabSpaces: Int = 4
    ): TextFieldValue {
        val text = currentValue.text
        val cursor = currentValue.selection.start
        if (cursor < 0 || cursor > text.length) return currentValue

        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val prevLine = text.substring(lineStart, cursor)

        // Count leading whitespace on previous line
        val leadingSpaces = prevLine.takeWhile { it == ' ' || it == '\t' }

        val extraIndent = if (prevLine.trimEnd().endsWith('{') ||
            prevLine.trimEnd().endsWith(':') ||
            prevLine.trimEnd().endsWith('(')
        ) {
            " ".repeat(tabSpaces)
        } else {
            ""
        }

        val indentToInsert = "\n" + leadingSpaces + extraIndent
        val newText = text.substring(0, cursor) + indentToInsert + text.substring(currentValue.selection.end)
        val newCursor = cursor + indentToInsert.length

        return currentValue.copy(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    /**
     * Auto-close pairs like (), [], {}, "", ''
     */
    fun handleAutoClose(
        currentValue: TextFieldValue,
        insertedChar: Char
    ): TextFieldValue? {
        val closer = when (insertedChar) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '"' -> '"'
            '\'' -> '\''
            else -> return null
        }

        val text = currentValue.text
        val cursor = currentValue.selection.start
        val newText = text.substring(0, cursor) + insertedChar + closer + text.substring(currentValue.selection.end)
        return currentValue.copy(
            text = newText,
            selection = TextRange(cursor + 1)
        )
    }
}
