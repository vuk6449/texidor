package com.example.editor.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.EditorThemeColors
import java.util.regex.Pattern

class SyntaxHighlighter(
    private val language: EditorLanguage,
    private val themeColors: EditorThemeColors,
    private val searchMatches: List<IntRange> = emptyList(),
    private val activeMatchIndex: Int = -1
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        if (rawText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(rawText)

        try {
            if (language == EditorLanguage.MARKDOWN) {
                highlightMarkdown(rawText, builder)
            } else if (language != EditorLanguage.PLAIN_TEXT) {
                highlightCodeSyntax(rawText, builder)
            }

            // In-text search matches highlight
            if (searchMatches.isNotEmpty()) {
                highlightSearchMatches(rawText, builder)
            }
        } catch (_: Exception) {
            // Safe fallback
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightSearchMatches(code: String, builder: AnnotatedString.Builder) {
        for (i in searchMatches.indices) {
            val range = searchMatches[i]
            val start = range.first.coerceIn(0, code.length)
            val end = (range.last + 1).coerceIn(0, code.length)
            if (start >= end) continue

            val isActive = i == activeMatchIndex
            val bg = if (isActive) Color(0xFFFF5722) else Color(0xFFFACC15)
            val fg = Color(0xFF000000)

            builder.addStyle(
                SpanStyle(
                    background = bg,
                    color = fg,
                    fontWeight = FontWeight.Bold
                ),
                start,
                end
            )
        }
    }

    private fun highlightMarkdown(code: String, builder: AnnotatedString.Builder) {
        // Headers (# ...)
        val headerMatcher = Pattern.compile("^(#{1,6}\\s.*)$", Pattern.MULTILINE).matcher(code)
        while (headerMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.primary, fontWeight = FontWeight.Bold),
                headerMatcher.start(),
                headerMatcher.end()
            )
        }

        // Bold (**text**)
        val boldMatcher = Pattern.compile("(\\*\\*[^*]+\\*\\*)").matcher(code)
        while (boldMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold),
                boldMatcher.start(),
                boldMatcher.end()
            )
        }

        // Italic (*text*)
        val italicMatcher = Pattern.compile("(?<!\\*)(\\*[^*]+\\*)(?!\\*)").matcher(code)
        while (italicMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.accent, fontStyle = FontStyle.Italic),
                italicMatcher.start(),
                italicMatcher.end()
            )
        }

        // Inline code (`...`)
        val codeMatcher = Pattern.compile("(`[^`]+`)").matcher(code)
        while (codeMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.stringColor, fontWeight = FontWeight.Medium),
                codeMatcher.start(),
                codeMatcher.end()
            )
        }

        // Blockquotes (> ...)
        val quoteMatcher = Pattern.compile("^(>.*)$", Pattern.MULTILINE).matcher(code)
        while (quoteMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.commentColor, fontStyle = FontStyle.Italic),
                quoteMatcher.start(),
                quoteMatcher.end()
            )
        }

        // Links [text](url)
        val linkMatcher = Pattern.compile("(\\[.*?\\]\\(.*?\\))").matcher(code)
        while (linkMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = themeColors.functionColor, fontWeight = FontWeight.Medium),
                linkMatcher.start(),
                linkMatcher.end()
            )
        }
    }

    private fun highlightCodeSyntax(code: String, builder: AnnotatedString.Builder) {
        // 1. Strings
        val stringRegex = Pattern.compile("(\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'|`([^`\\\\])*`)")
        val stringMatcher = stringRegex.matcher(code)
        val stringSpans = mutableListOf<IntRange>()
        while (stringMatcher.find()) {
            val start = stringMatcher.start()
            val end = stringMatcher.end()
            builder.addStyle(
                SpanStyle(color = themeColors.stringColor),
                start,
                end
            )
            stringSpans.add(start until end)
        }

        // 2. Comments
        val commentPatternStr = when (language) {
            EditorLanguage.PYTHON, EditorLanguage.SHELL -> "#.*$"
            EditorLanguage.SQL -> "(--.*$|/\\*.*?\\*/)"
            EditorLanguage.HTML -> "<!--.*?-->"
            EditorLanguage.CSS -> "/\\*.*?\\*/"
            EditorLanguage.NASM -> ";.*$"
            else -> "(//.*$|/\\*.*?\\*/)"
        }
        val commentRegex = Pattern.compile(commentPatternStr, Pattern.MULTILINE or Pattern.DOTALL)
        val commentMatcher = commentRegex.matcher(code)
        val commentSpans = mutableListOf<IntRange>()
        while (commentMatcher.find()) {
            val start = commentMatcher.start()
            val end = commentMatcher.end()
            if (stringSpans.none { it.contains(start) }) {
                builder.addStyle(
                    SpanStyle(
                        color = themeColors.commentColor,
                        fontStyle = FontStyle.Italic
                    ),
                    start,
                    end
                )
                commentSpans.add(start until end)
            }
        }

        val coveredSpans = stringSpans + commentSpans

        fun isCovered(start: Int): Boolean {
            return coveredSpans.any { it.contains(start) }
        }

        // 3. Annotations & Decorators (@Composable, @decorator, @Override)
        val annotRegex = Pattern.compile("(@[a-zA-Z_][a-zA-Z0-9_.]*)")
        val annotMatcher = annotRegex.matcher(code)
        while (annotMatcher.find()) {
            val start = annotMatcher.start()
            val end = annotMatcher.end()
            if (!isCovered(start)) {
                builder.addStyle(
                    SpanStyle(color = themeColors.accent, fontWeight = FontWeight.SemiBold),
                    start,
                    end
                )
            }
        }

        // 4. Numbers (supports hex 0x... and decimal)
        val numberRegex = Pattern.compile("\\b(\\d+(\\.\\d+)?([eE][+-]?\\d+)?([fFdDlL])?|0x[0-9a-fA-F]+|[0-9a-fA-F]+h)\\b")
        val numMatcher = numberRegex.matcher(code)
        while (numMatcher.find()) {
            val start = numMatcher.start()
            val end = numMatcher.end()
            if (!isCovered(start)) {
                builder.addStyle(
                    SpanStyle(color = themeColors.numberColor, fontWeight = FontWeight.Medium),
                    start,
                    end
                )
            }
        }

        // 5. Keywords
        if (language.keywords.isNotEmpty()) {
            val kwPattern = "\\b(" + language.keywords.joinToString("|") { Pattern.quote(it) } + ")\\b"
            val flags = if (language == EditorLanguage.SQL || language == EditorLanguage.NASM) Pattern.CASE_INSENSITIVE else 0
            val kwRegex = Pattern.compile(kwPattern, flags)
            val kwMatcher = kwRegex.matcher(code)
            while (kwMatcher.find()) {
                val start = kwMatcher.start()
                val end = kwMatcher.end()
                if (!isCovered(start)) {
                    builder.addStyle(
                        SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold),
                        start,
                        end
                    )
                }
            }
        }

        // 6. Built-in types / Registers
        if (language.builtInTypes.isNotEmpty()) {
            val typePattern = "\\b(" + language.builtInTypes.joinToString("|") { Pattern.quote(it) } + ")\\b"
            val flags = if (language == EditorLanguage.NASM) Pattern.CASE_INSENSITIVE else 0
            val typeRegex = Pattern.compile(typePattern, flags)
            val typeMatcher = typeRegex.matcher(code)
            while (typeMatcher.find()) {
                val start = typeMatcher.start()
                val end = typeMatcher.end()
                if (!isCovered(start)) {
                    builder.addStyle(
                        SpanStyle(color = themeColors.typeColor, fontWeight = FontWeight.SemiBold),
                        start,
                        end
                    )
                }
            }
        }

        // 7. Functions (identifier followed by '(')
        val funcRegex = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()")
        val funcMatcher = funcRegex.matcher(code)
        while (funcMatcher.find()) {
            val start = funcMatcher.start(1)
            val end = funcMatcher.end(1)
            if (!isCovered(start) && !language.keywords.contains(funcMatcher.group(1))) {
                builder.addStyle(
                    SpanStyle(color = themeColors.functionColor),
                    start,
                    end
                )
            }
        }

        // 8. HTML/XML tags
        if (language == EditorLanguage.HTML) {
            val tagRegex = Pattern.compile("</?([a-zA-Z0-9_-]+)")
            val tagMatcher = tagRegex.matcher(code)
            while (tagMatcher.find()) {
                val start = tagMatcher.start(1)
                val end = tagMatcher.end(1)
                if (!isCovered(start)) {
                    builder.addStyle(
                        SpanStyle(color = themeColors.keywordColor, fontWeight = FontWeight.Bold),
                        start,
                        end
                    )
                }
            }
        }

        // 9. Operators & Brackets
        val opRegex = Pattern.compile("[{}()\\[\\]=+\\-*/%&|^!<>?:;.,]")
        val opMatcher = opRegex.matcher(code)
        while (opMatcher.find()) {
            val start = opMatcher.start()
            val end = opMatcher.end()
            if (!isCovered(start)) {
                builder.addStyle(
                    SpanStyle(color = themeColors.operatorColor),
                    start,
                    end
                )
            }
        }
    }
}
