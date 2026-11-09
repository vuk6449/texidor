package com.example.editor.execution

data class DisassemblyLine(
    val address: Long,
    val hexBytes: String,
    val instruction: String
)

data class ExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String = "",
    val exitCode: Int = 0,
    val executionTimeMs: Long = 0,
    val registers: Map<String, Long>? = null,
    val disassembly: List<DisassemblyLine>? = null,
    val sqlColumns: List<String>? = null,
    val sqlRows: List<List<String>>? = null
)
