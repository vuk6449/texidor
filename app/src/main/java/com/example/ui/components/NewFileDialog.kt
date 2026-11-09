package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.syntax.EditorLanguage
import com.example.ui.theme.EditorThemeColors

data class FileTemplate(
    val title: String,
    val defaultName: String,
    val language: EditorLanguage,
    val starterContent: String,
    val fileType: String
)

@Composable
fun NewFileDialog(
    themeColors: EditorThemeColors,
    onCreateFile: (title: String, fileType: String, language: String, starterContent: String) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = listOf(
        FileTemplate(
            title = "NASM x86-64",
            defaultName = "main.asm",
            language = EditorLanguage.NASM,
            starterContent = """; Texidor - NASM x86-64 Assembly
section .data
    msg db "Hello, x86-64 NASM!", 10
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

    ; 64-bit computation
    mov rax, 40
    add rax, 2

    ; sys_exit(42)
    mov rdi, rax
    mov rax, 60
    syscall
""",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "SQL Database",
            defaultName = "query.sql",
            language = EditorLanguage.SQL,
            starterContent = """-- Texidor In-Memory SQLite Runner
CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, role TEXT);
INSERT INTO users VALUES (1, 'Alice', 'Engineer');
INSERT INTO users VALUES (2, 'Bob', 'Architect');
SELECT * FROM users;
""",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Python",
            defaultName = "script.py",
            language = EditorLanguage.PYTHON,
            starterContent = "def main():\n    print(\"Hello from Texidor!\")\n    for i in range(1, 6):\n        print(f\"Step {i}: count={i*10}\")\n\nif __name__ == '__main__':\n    main()\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "JavaScript",
            defaultName = "app.js",
            language = EditorLanguage.JAVASCRIPT,
            starterContent = "function calculate() {\n  console.log(\"Texidor JS Runtime\");\n  let sum = 0;\n  for (let i = 1; i <= 10; i++) sum += i;\n  console.log(\"Sum 1..10 =\", sum);\n}\ncalculate();\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Bash / Shell",
            defaultName = "script.sh",
            language = EditorLanguage.SHELL,
            starterContent = "#!/bin/bash\necho \"Texidor Shell Runner\"\necho \"Current kernel: $(uname -s -m)\"\necho \"Listing files:\"\nls -la\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Kotlin",
            defaultName = "Main.kt",
            language = EditorLanguage.KOTLIN,
            starterContent = "fun main() {\n    println(\"Hello from Texidor!\")\n    val numbers = listOf(1, 2, 3, 4, 5)\n    println(\"Doubled: \" + numbers.map { it * 2 })\n}\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Java",
            defaultName = "Main.java",
            language = EditorLanguage.JAVA,
            starterContent = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Texidor Java!\");\n    }\n}\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "C / C++",
            defaultName = "main.c",
            language = EditorLanguage.C_CPP,
            starterContent = "#include <stdio.h>\n\nint main() {\n    printf(\"Hello from Texidor C/C++!\\n\");\n    return 0;\n}\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Rust",
            defaultName = "main.rs",
            language = EditorLanguage.RUST,
            starterContent = "fn main() {\n    println!(\"Hello from Texidor Rust!\");\n}\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Go",
            defaultName = "main.go",
            language = EditorLanguage.GO,
            starterContent = "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello from Texidor Go!\")\n}\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "HTML Web",
            defaultName = "index.html",
            language = EditorLanguage.HTML,
            starterContent = "<!DOCTYPE html>\n<html>\n<head>\n  <title>Texidor</title>\n</head>\n<body>\n  <h1>Hello from Texidor</h1>\n</body>\n</html>\n",
            fileType = "CODE"
        ),
        FileTemplate(
            title = "Markdown",
            defaultName = "Document.md",
            language = EditorLanguage.MARKDOWN,
            starterContent = "# Notes\n\n- [ ] Task 1\n- [x] Task 2\n",
            fileType = "TXT"
        ),
        FileTemplate(
            title = "Text Note",
            defaultName = "Notes.txt",
            language = EditorLanguage.PLAIN_TEXT,
            starterContent = "",
            fileType = "TXT"
        )
    )

    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var fileName by remember { mutableStateOf(selectedTemplate.defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surface,
        title = {
            Text(
                text = "Create New Document",
                color = themeColors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CHOOSE TEMPLATE",
                    color = themeColors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        val isSelected = template.title == selectedTemplate.title
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeColors.primary.copy(alpha = 0.2f) else themeColors.surfaceVariant)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) themeColors.primary else themeColors.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedTemplate = template
                                    fileName = template.defaultName
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = template.title,
                                color = if (isSelected) themeColors.primary else themeColors.textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        unfocusedBorderColor = themeColors.surfaceVariant,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateFile(
                        fileName.trim(),
                        selectedTemplate.fileType,
                        selectedTemplate.language.id,
                        selectedTemplate.starterContent
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                modifier = Modifier.testTag("confirm_create_file")
            ) {
                Text("Create", color = themeColors.surface, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = themeColors.textColor)
            ) {
                Text("Cancel")
            }
        }
    )
}
