package com.example.editor.web

import com.example.editor.ts.TypeScriptCompiler
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

data class BundledWebProject(
    val html: String,
    val hasTypeScript: Boolean,
    val compiledScriptCount: Int,
    val inlinedStyleCount: Int
)

object WebProjectBundler {

    /**
     * Bundles HTML with sibling project files (CSS, JS, TS, images)
     * and injects an interactive DevTools console capture script.
     */
    fun bundleHtmlProject(
        mainHtml: String,
        projectFiles: Map<String, String>, // relative path or filename -> content
        currentFileName: String = "index.html"
    ): BundledWebProject {
        var processedHtml = mainHtml
        var hasTypeScript = false
        var compiledScriptCount = 0
        var inlinedStyleCount = 0

        // 1. Resolve and inline CSS <link rel="stylesheet" href="...">
        val linkRegex = Regex("<link\\s+[^>]*rel=[\"']stylesheet[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>|<link\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*rel=[\"']stylesheet[\"'][^>]*>", RegexOption.IGNORE_CASE)
        processedHtml = linkRegex.replace(processedHtml) { match ->
            val href = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val cleanHref = href.removePrefix("./").trim()
            val matchedContent = findContent(cleanHref, projectFiles)

            if (matchedContent != null) {
                inlinedStyleCount++
                "<!-- Inlined $cleanHref -->\n<style>\n$matchedContent\n</style>"
            } else {
                match.value
            }
        }

        // 2. Resolve and inline Scripts <script src="..."></script>
        val scriptRegex = Regex("<script\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>\\s*</script>", RegexOption.IGNORE_CASE)
        processedHtml = scriptRegex.replace(processedHtml) { match ->
            val src = match.groupValues[1].removePrefix("./").trim()
            val ext = src.substringAfterLast('.', "").lowercase()

            val matchedContent = findContent(src, projectFiles)
                ?: if (ext == "js") findContent(src.substringBeforeLast('.') + ".ts", projectFiles) else null

            if (matchedContent != null) {
                val isTs = ext in listOf("ts", "tsx") || (findContent(src, projectFiles) == null && matchedContent != null)
                val executableJs = if (isTs) {
                    hasTypeScript = true
                    TypeScriptCompiler.transpile(matchedContent).javascript
                } else {
                    matchedContent
                }
                compiledScriptCount++
                "<!-- Inlined $src (TS auto-compiled: $isTs) -->\n<script>\n$executableJs\n</script>"
            } else {
                match.value
            }
        }

        // 3. Inject DevTools Console Capture and Viewport meta if missing
        processedHtml = injectDevToolsAndMeta(processedHtml)

        return BundledWebProject(
            html = processedHtml,
            hasTypeScript = hasTypeScript,
            compiledScriptCount = compiledScriptCount,
            inlinedStyleCount = inlinedStyleCount
        )
    }

    /**
     * Bundles markdown content into a rich GitHub-style HTML document
     */
    fun bundleMarkdown(content: String, title: String): String {
        val safeJson = JSONObject.quote(content)
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/github-markdown-css@5.5.0/github-markdown-dark.min.css">
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css">
          <style>
            body {
              box-sizing: border-box;
              min-width: 200px;
              max-width: 980px;
              margin: 0 auto;
              padding: 24px 16px;
              background-color: #0D1117;
              color: #C9D1D9;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans", Helvetica, Arial, sans-serif;
            }
            .markdown-body {
              background-color: transparent !important;
            }
            .task-list-item input[type="checkbox"] {
              margin-right: 6px;
            }
            pre code.hljs {
              padding: 12px;
              border-radius: 8px;
            }
          </style>
        </head>
        <body class="markdown-body">
          <div id="content">Loading preview...</div>

          <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
          <script>
            const raw = $safeJson;
            if (window.marked) {
              marked.setOptions({
                gfm: true,
                breaks: true,
                highlight: function(code, lang) {
                  if (window.hljs && hljs.getLanguage(lang)) {
                    return hljs.highlight(code, { language: lang }).value;
                  }
                  return code;
                }
              });
              document.getElementById('content').innerHTML = marked.parse(raw);
            } else {
              document.getElementById('content').innerText = raw;
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun findContent(relativePath: String, files: Map<String, String>): String? {
        val clean = relativePath.trim().removePrefix("./").lowercase()
        return files.entries.firstOrNull { entry ->
            val key = entry.key.trim().removePrefix("./").lowercase()
            key == clean || key.endsWith("/$clean")
        }?.value
    }

    private fun injectDevToolsAndMeta(html: String): String {
        val consoleCaptureScript = """
        <script>
        (function() {
          function sendLog(level, args) {
            try {
              var msg = Array.prototype.slice.call(args).map(function(arg) {
                if (typeof arg === 'object') {
                  try { return JSON.stringify(arg); } catch(e) { return String(arg); }
                }
                return String(arg);
              }).join(' ');
              if (window.AndroidConsole && window.AndroidConsole.postMessage) {
                window.AndroidConsole.postMessage(level, msg);
              }
            } catch(e) {}
          }
          var origLog = console.log;
          var origWarn = console.warn;
          var origError = console.error;
          var origInfo = console.info;

          console.log = function() { sendLog('LOG', arguments); origLog.apply(console, arguments); };
          console.warn = function() { sendLog('WARN', arguments); origWarn.apply(console, arguments); };
          console.error = function() { sendLog('ERROR', arguments); origError.apply(console, arguments); };
          console.info = function() { sendLog('INFO', arguments); origInfo.apply(console, arguments); };

          window.addEventListener('error', function(e) {
            sendLog('ERROR', [e.message + ' at ' + (e.filename || '') + ':' + (e.lineno || 0)]);
          });
        })();
        </script>
        """.trimIndent()

        var result = html
        if (!result.contains("<meta name=\"viewport\"", ignoreCase = true)) {
            val metaViewport = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            result = if (result.contains("<head>", ignoreCase = true)) {
                result.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>\n  $metaViewport")
            } else {
                "$metaViewport\n$result"
            }
        }

        result = if (result.contains("<head>", ignoreCase = true)) {
            result.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>\n$consoleCaptureScript")
        } else if (result.contains("<html>", ignoreCase = true)) {
            result.replaceFirst(Regex("<html>", RegexOption.IGNORE_CASE), "<html>\n<head>\n$consoleCaptureScript\n</head>")
        } else {
            "$consoleCaptureScript\n$result"
        }

        return result
    }
}
