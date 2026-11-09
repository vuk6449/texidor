package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.editor.web.WebProjectBundler
import com.example.ui.theme.EditorThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleLogEntry(
    val level: String, // "LOG", "INFO", "WARN", "ERROR"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ViewportMode(val title: String, val widthDp: Int?) {
    RESPONSIVE("Fluid", null),
    MOBILE("Mobile (375px)", 375),
    TABLET("Tablet (640px)", 640)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlPreviewScreen(
    title: String,
    content: String,
    isMarkdown: Boolean,
    themeColors: EditorThemeColors,
    projectFiles: Map<String, String> = emptyMap(),
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var reloadKey by remember { mutableStateOf(0) }
    var activeTab by remember { mutableStateOf("PREVIEW") } // "PREVIEW", "CONSOLE"
    var viewportMode by remember { mutableStateOf(ViewportMode.RESPONSIVE) }
    val consoleLogs = remember { mutableStateListOf<ConsoleLogEntry>() }

    val bundledResult = remember(content, isMarkdown, projectFiles, reloadKey) {
        if (isMarkdown) {
            WebProjectBundler.bundleMarkdown(content, title)
        } else {
            val bundle = WebProjectBundler.bundleHtmlProject(content, projectFiles, title)
            bundle.html
        }
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.surface)
    ) {
        // TOP CONTROL STRIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title & Tabs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMarkdown) "Markdown Preview" else "Live Web Preview",
                    color = themeColors.textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )

                // Tab Pills
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColors.surface)
                        .padding(2.dp)
                ) {
                    // Preview Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeTab == "PREVIEW") themeColors.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { activeTab = "PREVIEW" }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Web, contentDescription = null, tint = if (activeTab == "PREVIEW") themeColors.primary else themeColors.gutterTextColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Page", color = if (activeTab == "PREVIEW") themeColors.primary else themeColors.textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Console Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeTab == "CONSOLE") themeColors.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { activeTab = "CONSOLE" }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = if (activeTab == "CONSOLE") themeColors.primary else themeColors.gutterTextColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Console (${consoleLogs.size})", color = if (activeTab == "CONSOLE") themeColors.primary else themeColors.textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Right: Viewport Toggles & Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeTab == "PREVIEW" && !isMarkdown) {
                    IconButton(
                        onClick = {
                            viewportMode = when (viewportMode) {
                                ViewportMode.RESPONSIVE -> ViewportMode.MOBILE
                                ViewportMode.MOBILE -> ViewportMode.TABLET
                                ViewportMode.TABLET -> ViewportMode.RESPONSIVE
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        val icon = when (viewportMode) {
                            ViewportMode.RESPONSIVE -> Icons.Default.Devices
                            ViewportMode.MOBILE -> Icons.Default.PhoneAndroid
                            ViewportMode.TABLET -> Icons.Default.Tablet
                        }
                        Icon(icon, contentDescription = viewportMode.title, tint = themeColors.primary, modifier = Modifier.size(16.dp))
                    }
                }

                if (activeTab == "CONSOLE") {
                    IconButton(
                        onClick = { consoleLogs.clear() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Console", tint = themeColors.gutterTextColor, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = {
                        consoleLogs.clear()
                        reloadKey++
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        // BODY
        if (activeTab == "PREVIEW") {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117)),
                contentAlignment = Alignment.TopCenter
            ) {
                val boxModifier = if (viewportMode.widthDp != null) {
                    Modifier
                        .width(viewportMode.widthDp!!.dp)
                        .fillMaxHeight()
                        .border(1.dp, themeColors.surfaceVariant, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                } else {
                    Modifier.fillMaxSize()
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.databaseEnabled = true

                            // Inject JS bridge for console capture
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun postMessage(level: String, msg: String) {
                                    mainHandler.post {
                                        consoleLogs.add(ConsoleLogEntry(level.uppercase(), msg))
                                    }
                                }
                            }, "AndroidConsole")

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    if (consoleMessage != null) {
                                        val lvl = when (consoleMessage.messageLevel()) {
                                            ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                                            ConsoleMessage.MessageLevel.WARNING -> "WARN"
                                            ConsoleMessage.MessageLevel.LOG -> "LOG"
                                            ConsoleMessage.MessageLevel.TIP -> "INFO"
                                            else -> "DEBUG"
                                        }
                                        mainHandler.post {
                                            consoleLogs.add(ConsoleLogEntry(lvl, consoleMessage.message()))
                                        }
                                    }
                                    return super.onConsoleMessage(consoleMessage)
                                }
                            }

                            webViewClient = WebViewClient()
                            loadDataWithBaseURL("https://texidor.local/", bundledResult, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL("https://texidor.local/", bundledResult, "text/html", "UTF-8", null)
                    },
                    modifier = boxModifier
                )
            }
        } else {
            // CONSOLE TAB
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0F1D))
                    .padding(8.dp)
            ) {
                if (consoleLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("DevTools Console is empty. Run page scripts to inspect logs.", color = themeColors.gutterTextColor, fontSize = 12.sp)
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(consoleLogs) { entry ->
                            val color = when (entry.level) {
                                "ERROR" -> Color(0xFFEF4444)
                                "WARN" -> Color(0xFFF59E0B)
                                "INFO" -> Color(0xFF38BDF8)
                                else -> Color(0xFFE2E8F0)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(90.dp)
                                )
                                Text(
                                    text = "[${entry.level}]",
                                    color = color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(65.dp)
                                )
                                Text(
                                    text = entry.message,
                                    color = color,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
