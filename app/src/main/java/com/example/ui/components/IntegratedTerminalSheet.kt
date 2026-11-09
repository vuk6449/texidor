package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.terminal.TerminalLine
import com.example.editor.terminal.TerminalLineType
import com.example.editor.terminal.UbuntuShellEmulator
import com.example.ui.theme.EditorThemeColors
import kotlinx.coroutines.launch

@Composable
fun IntegratedTerminalSheet(
    terminal: UbuntuShellEmulator,
    themeColors: EditorThemeColors,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val terminalLines = remember { mutableStateListOf<TerminalLine>() }
    var inputCommand by remember { mutableStateOf("") }
    var cwdLabel by remember { mutableStateOf(terminal.getPromptString()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (terminalLines.isEmpty()) {
            terminalLines.add(
                TerminalLine(
                    TerminalLineType.SUCCESS,
                    "Welcome to Ubuntu 24.04 LTS (GNU/Linux aarch64)\nTexidor Integrated Termux Environment with apt, make & coreutils.\nType 'help' or 'neofetch' to begin.\n"
                )
            )
        }
    }

    fun submitCommand(cmd: String) {
        if (cmd.isBlank()) return
        val command = cmd.trim()
        inputCommand = ""

        coroutineScope.launch {
            val lines = terminal.executeCommand(command)
            if (lines.any { it.text == "__CLEAR__" }) {
                terminalLines.clear()
            } else {
                terminalLines.addAll(lines)
            }
            cwdLabel = terminal.getPromptString()
            if (terminalLines.isNotEmpty()) {
                listState.animateScrollToItem(terminalLines.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 450.dp)
            .background(Color(0xFF090D16))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
    ) {
        // TERMINAL HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TERMINAL",
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ubuntu 24.04",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { terminalLines.clear() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Terminal", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                }
            }
        }

        // QUICK COMMAND CHIPS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1322))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickChips = listOf(
                "ls -la",
                "pwd",
                "cd ..",
                "apt list --installed",
                "apt install nasm",
                "make",
                "neofetch",
                "tree",
                "clear"
            )
            for (chip in quickChips) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { submitCommand(chip) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chip,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // TERMINAL OUTPUT STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            items(terminalLines) { line ->
                val color = when (line.type) {
                    TerminalLineType.PROMPT -> Color(0xFF4ADE80)
                    TerminalLineType.SUCCESS -> Color(0xFF38BDF8)
                    TerminalLineType.WARNING -> Color(0xFFFBBF24)
                    TerminalLineType.ERROR -> Color(0xFFF87171)
                    TerminalLineType.SYSTEM -> Color(0xFF94A3B8)
                    TerminalLineType.OUTPUT -> Color(0xFFE2E8F0)
                }

                Text(
                    text = line.text,
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // COMMAND INPUT LINE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B101E))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "❯",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 6.dp)
            )

            TextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = {
                    Text(
                        text = "Enter terminal command (e.g. ls, apt install, make, neofetch)...",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCommand(inputCommand) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field")
            )

            IconButton(
                onClick = { submitCommand(inputCommand) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Execute",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
