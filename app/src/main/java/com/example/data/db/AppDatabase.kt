package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omniedit_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(getDatabase(context).documentDao())
            }
        }

        private suspend fun populateInitialData(dao: DocumentDao) {
            val now = System.currentTimeMillis()

            val welcomeText = """
# Welcome to Texidor ✦
The free, fast, and powerful multi-format text, code & PDF editor.

## Key Features:
• 📝 Normal TXT & Markdown editing with word & character counts.
• 💻 Code Editor with real-time Syntax Highlighting & IntelliSense auto-complete.
• 📄 Native PDF Viewer & Annotation Suite: draw freehand pen markups, highlight text, and add stamps.
• 🎨 Sleek Minimalist Themes: Obsidian OLED, Monokai Pro, Dracula, Nord Frost, Cyberpunk, and Light modes.
• ⚡ 100% Free Forever: No user accounts, zero subscriptions, no trackers, completely offline-first.
• 📱 Multi-device ready: Optimized for phones, foldables, tablets, and Android TV / keyboard navigation.

## Quick Shortcuts & Navigation:
- Top Bar: Save, Undo/Redo, Search & Replace, Document Stats, Theme Picker.
- Bottom Accessory Bar: Quick coding symbols ({}, (), [], =>), Tab indents, Comment toggles.
- Drawer: Switch between documents, create new files, or open files from device storage.

Enjoy distraction-free creating and editing!
""".trimIndent()

            val pythonDemo = """
# OmniEdit - Python Demo Script
# IntelliSense & Syntax Highlighting Demo

import math
from typing import List, Dict

class NeuralUnit:
    def __init__(self, name: str, learning_rate: float = 0.01):
        self.name = name
        self.learning_rate = learning_rate
        self.weights: List[float] = [0.15, -0.28, 0.64]

    def activate(self, inputs: List[float]) -> float:
        '''Compute weighted sigmoid activation'''
        dot_product = sum(w * x for w, x in zip(self.weights, inputs))
        return 1.0 / (1.0 + math.exp(-dot_product))

def run_simulation():
    unit = NeuralUnit("OmniNode_1")
    sample_inputs = [1.2, 0.8, -0.5]
    output = unit.activate(sample_inputs)
    print(f"Activation Result: {output:.4f}")
    return output

if __name__ == "__main__":
    run_simulation()
""".trimIndent()

            val kotlinDemo = """
package com.omniedit.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

/**
 * OmniEdit Kotlin sample demonstrating clean syntax & IntelliSense
 */
data class CodeSnippet(
    val title: String,
    val language: String,
    val lineCount: Int
)

class SnippetRepository {
    private val snippets = mutableListOf<CodeSnippet>()

    fun getSnippetsFlow(): Flow<List<CodeSnippet>> = flow {
        while (true) {
            emit(snippets.toList())
            delay(1000)
        }
    }

    suspend fun addSnippet(title: String, language: String, lines: Int) {
        snippets.add(CodeSnippet(title, language, lines))
        println("Added snippet: ${'$'}title in ${'$'}language")
    }
}
""".trimIndent()

            val htmlDemo = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>OmniEdit Web Preview</title>
  <style>
    body {
      background: #090D16;
      color: #E2E8F0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      margin: 0;
      padding: 16px;
    }
    .card {
      background: #111827;
      border: 1px solid #1F2937;
      border-radius: 16px;
      padding: 32px;
      max-width: 440px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.5);
    }
    h1 { color: #38BDF8; margin-top: 0; }
    p { line-height: 1.6; color: #94A3B8; }
    .badge {
      display: inline-block;
      background: #0369A1;
      color: #FFFFFF;
      padding: 4px 12px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
    }
  </style>
</head>
<body>
  <div class="card">
    <span class="badge">OmniEdit Live Preview</span>
    <h1>Modern Code & Text</h1>
    <p>Live preview HTML & CSS directly inside OmniEdit. Sleek, responsive, and blazing fast.</p>
  </div>
</body>
</html>
""".trimIndent()

            val markdownNotes = """
# Product Design Spec
*Project: Texidor Editor Suite*

## Core Pillars
1. **Speed & Zero Bloat** - Instant launch, native Compose UI.
2. **Deep Developer Utilities** - Syntax highlighting, IntelliSense, auto-indent, code execution.
3. **Document Versatility** - TXT, MD, Code, SQL, NASM x86-64 assembly, and full PDF markup.
4. **Universal Accessibility** - Mobile, tablet, foldable, and Android TV navigation.

### Keyboard Shortcuts
| Action | Mobile / Tablet | TV / External Keyboard |
|---|---|---|
| Run / Compile | Play button | Ctrl + R |
| Search & Replace | Magnifier icon | Ctrl + F |
| Save | Cloud / Save icon | Ctrl + S |
| Undo | Back arrow | Ctrl + Z |
""".trimIndent()

            val nasmDemo = """; Texidor - NASM x86-64 Assembly Demo
; Emulated 64-bit Virtual CPU with Linux Syscalls

section .data
    msg db "Hello, x86-64 NASM Assembly World!", 10
    len equ $ - msg

section .text
    global _start

_start:
    ; sys_write(stdout=1, buffer=msg, length=len)
    mov rax, 1          ; syscall: sys_write
    mov rdi, 1          ; fd: stdout
    lea rsi, [msg]      ; buffer pointer
    mov rdx, len        ; buffer length
    syscall

    ; 64-bit computation
    mov rax, 40
    add rax, 2          ; rax = 42

    ; sys_exit(status=42)
    mov rdi, rax        ; return code 42
    mov rax, 60         ; syscall: sys_exit
    syscall
""".trimIndent()

            val sqlDemo = """-- Texidor In-Memory SQLite Database & Query Runner
CREATE TABLE engineers (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    favorite_arch TEXT NOT NULL,
    year_started INTEGER NOT NULL
);

INSERT INTO engineers VALUES (1, 'Ada Lovelace', 'Analytical Engine', 1843);
INSERT INTO engineers VALUES (2, 'Linus Torvalds', 'x86-64 / Linux', 1991);
INSERT INTO engineers VALUES (3, 'Grace Hopper', 'A-0 Compiler', 1952);
INSERT INTO engineers VALUES (4, 'Ken Thompson', 'PDP-11 / B', 1969);

SELECT id, name, favorite_arch, year_started FROM engineers ORDER BY year_started ASC;
""".trimIndent()

            dao.insert(
                DocumentEntity(
                    title = "Welcome.txt",
                    content = welcomeText,
                    fileType = "TXT",
                    language = "txt",
                    updatedAt = now,
                    isFavorite = true
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "main.asm",
                    content = nasmDemo,
                    fileType = "CODE",
                    language = "asm",
                    updatedAt = now - 500,
                    isFavorite = true
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "query.sql",
                    content = sqlDemo,
                    fileType = "CODE",
                    language = "sql",
                    updatedAt = now - 800
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "demo_script.py",
                    content = pythonDemo,
                    fileType = "CODE",
                    language = "python",
                    updatedAt = now - 1000
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "OmniCore.kt",
                    content = kotlinDemo,
                    fileType = "CODE",
                    language = "kotlin",
                    updatedAt = now - 2000
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "index.html",
                    content = htmlDemo,
                    fileType = "CODE",
                    language = "html",
                    updatedAt = now - 3000
                )
            )
            dao.insert(
                DocumentEntity(
                    title = "DesignSpec.md",
                    content = markdownNotes,
                    fileType = "TXT",
                    language = "markdown",
                    updatedAt = now - 4000,
                    isFavorite = true
                )
            )
        }
    }
}
