package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.execution.ExecutionResult
import com.example.editor.syntax.EditorLanguage
import com.example.ui.theme.EditorThemeColors

@Composable
fun ExecutionConsoleSheet(
    result: ExecutionResult?,
    isRunning: Boolean,
    language: EditorLanguage,
    themeColors: EditorThemeColors,
    onRerun: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("OUTPUT") } // "OUTPUT", "CPU", "GRID"

    val hasCpu = result?.registers != null || result?.disassembly != null || language == EditorLanguage.NASM
    val hasGrid = result?.sqlColumns != null && result.sqlColumns.isNotEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 380.dp),
        color = themeColors.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Terminal: ${language.displayName}",
                        color = themeColors.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isRunning) {
                        CircularProgressIndicator(
                            color = themeColors.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Compiling & Running...",
                            color = themeColors.primary,
                            fontSize = 11.sp
                        )
                    } else if (result != null) {
                        val statusColor = if (result.exitCode == 0 && result.success) Color(0xFF10B981) else Color(0xFFEF4444)
                        Text(
                            text = if (result.exitCode == 0 && result.success) "Exit 0 (${result.executionTimeMs}ms)" else "Exit ${result.exitCode}",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRerun, modifier = Modifier.size(28.dp).testTag("rerun_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rerun", tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp).testTag("clear_console_button")) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = themeColors.gutterTextColor, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp).testTag("close_console_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Tab Bar if NASM CPU or SQL Grid available
            if (hasCpu || hasGrid) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    TabPill(
                        label = "Console Output",
                        icon = Icons.Default.Terminal,
                        isSelected = activeTab == "OUTPUT",
                        themeColors = themeColors,
                        onClick = { activeTab = "OUTPUT" }
                    )

                    if (hasCpu) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TabPill(
                            label = "x86-64 CPU & Opcodes",
                            icon = Icons.Default.Memory,
                            isSelected = activeTab == "CPU",
                            themeColors = themeColors,
                            onClick = { activeTab = "CPU" }
                        )
                    }

                    if (hasGrid) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TabPill(
                            label = "Table Grid",
                            icon = Icons.Default.TableChart,
                            isSelected = activeTab == "GRID",
                            themeColors = themeColors,
                            onClick = { activeTab = "GRID" }
                        )
                    }
                }
            }

            Divider(color = themeColors.surfaceVariant)

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07090E))
            ) {
                when {
                    activeTab == "CPU" && hasCpu -> {
                        CpuRegistersAndDisassemblyView(result = result, themeColors = themeColors)
                    }
                    activeTab == "GRID" && hasGrid -> {
                        SqlResultTableView(result = result, themeColors = themeColors)
                    }
                    else -> {
                        ConsoleOutputView(result = result, isRunning = isRunning, themeColors = themeColors)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleOutputView(
    result: ExecutionResult?,
    isRunning: Boolean,
    themeColors: EditorThemeColors
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        if (isRunning) {
            Text(
                text = "> Compiling and executing binary...\n",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else if (result == null) {
            Text(
                text = "Press 'Run' (Play icon) to compile and execute current code.\n",
                color = themeColors.gutterTextColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            if (result.stdout.isNotEmpty()) {
                Text(
                    text = result.stdout,
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (result.stderr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.stderr,
                    color = Color(0xFFF87171),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\n[Process finished with exit code ${result.exitCode} in ${result.executionTimeMs} ms]",
                color = if (result.exitCode == 0) Color(0xFF10B981) else Color(0xFFEF4444),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CpuRegistersAndDisassemblyView(
    result: ExecutionResult?,
    themeColors: EditorThemeColors
) {
    val regs = result?.registers ?: emptyMap()
    val disasm = result?.disassembly ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Register Grid
        item {
            Text(
                text = "x86-64 REGISTERS",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            val regNames = listOf(
                "RAX", "RBX", "RCX", "RDX",
                "RSI", "RDI", "RBP", "RSP",
                "R8", "R9", "R10", "R11", "RIP"
            )

            // Render in 2 columns
            for (chunk in regNames.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (reg in chunk) {
                        val v = regs[reg] ?: 0L
                        val hexStr = "0x" + "%016X".format(v)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF131B2E))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = reg, color = Color(0xFFA7F3D0), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(text = "$hexStr ($v)", color = Color(0xFFE2E8F0), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MACHINE CODE DISASSEMBLY",
                color = Color(0xFFFBBF24),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(disasm) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val addrStr = "0x%04X".format(item.address)
                Text(
                    text = addrStr,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(55.dp)
                )
                Text(
                    text = item.hexBytes.padEnd(16),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(110.dp)
                )
                Text(
                    text = item.instruction,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SqlResultTableView(
    result: ExecutionResult?,
    themeColors: EditorThemeColors
) {
    val cols = result?.sqlColumns ?: emptyList()
    val rows = result?.sqlRows ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .background(Color(0xFF1E293B))
                .padding(vertical = 6.dp)
        ) {
            for (col in cols) {
                Text(
                    text = col,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(120.dp).padding(horizontal = 6.dp)
                )
            }
        }

        // Table Rows
        for ((idx, row) in rows.withIndex()) {
            val bg = if (idx % 2 == 0) Color(0xFF0F172A) else Color(0xFF131B2E)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 6.dp)
            ) {
                for (cell in row) {
                    Text(
                        text = cell,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(120.dp).padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    themeColors: EditorThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) themeColors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) themeColors.surface else themeColors.textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) themeColors.surface else themeColors.textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
