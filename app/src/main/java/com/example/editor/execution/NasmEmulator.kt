package com.example.editor.execution

import java.util.Locale

object NasmEmulator {

    private const val MAX_CYCLES = 100_000
    private const val MEMORY_SIZE = 65536
    private const val STACK_TOP = 0xFFF0L

    data class ParsedInstruction(
        val opcode: String,
        val operands: List<String>,
        val originalText: String,
        val address: Long,
        val hexBytes: String
    )

    fun assembleAndRun(code: String): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        try {
            val memory = ByteArray(MEMORY_SIZE)
            val registers = mutableMapOf<String, Long>().apply {
                listOf(
                    "RAX", "RBX", "RCX", "RDX", "RSI", "RDI", "RBP", "RSP",
                    "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15", "RIP"
                ).forEach { put(it, 0L) }
                put("RSP", STACK_TOP)
                put("RBP", STACK_TOP)
            }

            var zf = false
            var sf = false

            // Symbol table: label -> address/value
            val symbols = mutableMapOf<String, Long>()
            val disassembly = mutableListOf<DisassemblyLine>()
            val instructions = mutableListOf<ParsedInstruction>()

            var dataOffset = 0x2000L
            var textOffset = 0x4000L

            var currentSection = ".text"
            val lines = code.lines()

            // Pass 1: Parse sections, data directives, and record labels
            var i = 0
            while (i < lines.size) {
                val rawLine = lines[i].trim()
                val lineWithoutComment = rawLine.substringBefore(';').trim()
                i++
                if (lineWithoutComment.isEmpty()) continue

                val lower = lineWithoutComment.lowercase(Locale.ROOT)
                if (lower.startsWith("section .data") || lower.startsWith("segment .data")) {
                    currentSection = ".data"
                    continue
                } else if (lower.startsWith("section .bss") || lower.startsWith("segment .bss")) {
                    currentSection = ".bss"
                    continue
                } else if (lower.startsWith("section .text") || lower.startsWith("segment .text")) {
                    currentSection = ".text"
                    continue
                } else if (lower.startsWith("global ") || lower.startsWith("extern ") || lower.startsWith("default ")) {
                    continue
                }

                if (currentSection == ".data" || currentSection == ".bss") {
                    // Check for equ: label equ $ - otherLabel
                    if (lower.contains(" equ ")) {
                        val parts = lineWithoutComment.split(Regex("\\s+equ\\s+", RegexOption.IGNORE_CASE), 2)
                        val equLabel = parts[0].trim()
                        val expr = parts[1].trim()
                        if (expr.contains("$ -")) {
                            val targetLabel = expr.substringAfter("$ -").trim()
                            val targetAddr = symbols[targetLabel] ?: 0L
                            val len = (dataOffset - targetAddr).coerceAtLeast(0)
                            symbols[equLabel] = len
                        } else {
                            val parsedVal = parseImmediate(expr, symbols)
                            symbols[equLabel] = parsedVal
                        }
                        continue
                    }

                    // Check for label: or label db/dw/dd/dq/resb
                    var working = lineWithoutComment
                    if (working.contains(':')) {
                        val lbl = working.substringBefore(':').trim()
                        symbols[lbl] = dataOffset
                        working = working.substringAfter(':').trim()
                    }

                    if (working.isNotEmpty()) {
                        val tokens = working.split(Regex("\\s+"), 2)
                        var labelName: String? = null
                        var directive = tokens[0].lowercase(Locale.ROOT)
                        var valuesStr = tokens.getOrNull(1) ?: ""

                        if (!listOf("db", "dw", "dd", "dq", "resb", "resw", "resd", "resq").contains(directive) && valuesStr.isNotEmpty()) {
                            labelName = directive
                            val rest = valuesStr.split(Regex("\\s+"), 2)
                            directive = rest[0].lowercase(Locale.ROOT)
                            valuesStr = rest.getOrNull(1) ?: ""
                            symbols[labelName] = dataOffset
                        }

                        when (directive) {
                            "db" -> {
                                val bytes = parseByteList(valuesStr, symbols)
                                for (b in bytes) {
                                    if (dataOffset.toInt() < MEMORY_SIZE) {
                                        memory[dataOffset.toInt()] = b
                                    }
                                    dataOffset++
                                }
                            }
                            "dw" -> {
                                val v = parseImmediate(valuesStr, symbols)
                                writeMemory16(memory, dataOffset.toInt(), v)
                                dataOffset += 2
                            }
                            "dd" -> {
                                val v = parseImmediate(valuesStr, symbols)
                                writeMemory32(memory, dataOffset.toInt(), v)
                                dataOffset += 4
                            }
                            "dq" -> {
                                val v = parseImmediate(valuesStr, symbols)
                                writeMemory64(memory, dataOffset.toInt(), v)
                                dataOffset += 8
                            }
                            "resb" -> dataOffset += parseImmediate(valuesStr, symbols).coerceAtLeast(1)
                            "resw" -> dataOffset += parseImmediate(valuesStr, symbols).coerceAtLeast(1) * 2
                            "resd" -> dataOffset += parseImmediate(valuesStr, symbols).coerceAtLeast(1) * 4
                            "resq" -> dataOffset += parseImmediate(valuesStr, symbols).coerceAtLeast(1) * 8
                        }
                    }
                } else if (currentSection == ".text") {
                    var working = lineWithoutComment
                    // Check if line contains label
                    if (working.contains(':')) {
                        val lbl = working.substringBefore(':').trim()
                        symbols[lbl] = textOffset
                        working = working.substringAfter(':').trim()
                    }

                    if (working.isNotEmpty()) {
                        val parts = working.split(Regex("\\s+"), 2)
                        val op = parts[0].lowercase(Locale.ROOT)
                        val operands = if (parts.size > 1) {
                            splitOperands(parts[1])
                        } else {
                            emptyList()
                        }

                        val hex = generateSyntheticHex(op, operands)
                        val parsed = ParsedInstruction(op, operands, working, textOffset, hex)
                        instructions.add(parsed)
                        disassembly.add(DisassemblyLine(textOffset, hex, working))
                        textOffset += 4L
                    }
                }
            }

            // Pass 2: Execution
            val startAddr = symbols["_start"] ?: symbols["main"] ?: instructions.firstOrNull()?.address ?: 0x4000L
            var pc = startAddr
            var cycles = 0
            var exitCode = 0
            var terminated = false

            // Map address -> instruction index
            val addrToIndex = instructions.indices.associateBy { instructions[it].address }

            while (!terminated && cycles < MAX_CYCLES) {
                cycles++
                val idx = addrToIndex[pc]
                if (idx == null) {
                    // Program reached beyond code section without explicit exit
                    break
                }

                val inst = instructions[idx]
                registers["RIP"] = pc
                pc += 4L

                when (inst.opcode) {
                    "mov" -> {
                        val dst = inst.operands[0]
                        val src = inst.operands[1]
                        val srcVal = evaluateOperand(src, registers, memory, symbols)
                        writeRegisterOrMemory(dst, srcVal, registers, memory, symbols)
                    }
                    "lea" -> {
                        val dst = inst.operands[0]
                        val src = inst.operands[1].trim('[', ']')
                        val addr = evaluateAddress(src, registers, symbols)
                        setReg64(dst, addr, registers)
                    }
                    "add" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a + b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "sub" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a - b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "inc" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers) + 1
                        setReg64(dst, a, registers)
                        zf = (a == 0L)
                        sf = (a < 0L)
                    }
                    "dec" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers) - 1
                        setReg64(dst, a, registers)
                        zf = (a == 0L)
                        sf = (a < 0L)
                    }
                    "xor" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a xor b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "and" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a and b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "or" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a or b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "imul", "mul" -> {
                        val dst = inst.operands[0]
                        val a = getReg64(dst, registers)
                        val b = evaluateOperand(inst.operands.getOrElse(1) { dst }, registers, memory, symbols)
                        val res = a * b
                        setReg64(dst, res, registers)
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "cmp" -> {
                        val a = evaluateOperand(inst.operands[0], registers, memory, symbols)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a - b
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "test" -> {
                        val a = evaluateOperand(inst.operands[0], registers, memory, symbols)
                        val b = evaluateOperand(inst.operands[1], registers, memory, symbols)
                        val res = a and b
                        zf = (res == 0L)
                        sf = (res < 0L)
                    }
                    "jmp" -> {
                        val target = symbols[inst.operands[0]] ?: evaluateOperand(inst.operands[0], registers, memory, symbols)
                        pc = target
                    }
                    "je", "jz" -> {
                        if (zf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "jne", "jnz" -> {
                        if (!zf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "jg" -> {
                        if (!zf && !sf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "jge" -> {
                        if (!sf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "jl" -> {
                        if (sf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "jle" -> {
                        if (sf || zf) pc = symbols[inst.operands[0]] ?: pc
                    }
                    "push" -> {
                        val v = evaluateOperand(inst.operands[0], registers, memory, symbols)
                        var rsp = registers["RSP"] ?: STACK_TOP
                        rsp -= 8
                        writeMemory64(memory, rsp.toInt(), v)
                        registers["RSP"] = rsp
                    }
                    "pop" -> {
                        var rsp = registers["RSP"] ?: STACK_TOP
                        val v = readMemory64(memory, rsp.toInt())
                        rsp += 8
                        registers["RSP"] = rsp
                        setReg64(inst.operands[0], v, registers)
                    }
                    "call" -> {
                        var rsp = registers["RSP"] ?: STACK_TOP
                        rsp -= 8
                        writeMemory64(memory, rsp.toInt(), pc)
                        registers["RSP"] = rsp
                        pc = symbols[inst.operands[0]] ?: pc
                    }
                    "ret" -> {
                        var rsp = registers["RSP"] ?: STACK_TOP
                        val returnAddr = readMemory64(memory, rsp.toInt())
                        rsp += 8
                        registers["RSP"] = rsp
                        pc = returnAddr
                    }
                    "syscall" -> {
                        val rax = registers["RAX"] ?: 0L
                        when (rax) {
                            1L -> { // sys_write(rdi, rsi, rdx)
                                val fd = registers["RDI"] ?: 1L
                                val bufPtr = (registers["RSI"] ?: 0L).toInt()
                                val count = (registers["RDX"] ?: 0L).toInt().coerceIn(0, 4096)
                                if (bufPtr in 0 until MEMORY_SIZE) {
                                    val str = String(memory, bufPtr, count.coerceAtMost(MEMORY_SIZE - bufPtr), Charsets.UTF_8)
                                    if (fd == 2L) stderrBuilder.append(str) else stdoutBuilder.append(str)
                                }
                                registers["RAX"] = count.toLong()
                            }
                            60L -> { // sys_exit(rdi)
                                exitCode = (registers["RDI"] ?: 0L).toInt()
                                terminated = true
                            }
                            else -> {
                                stderrBuilder.append("[syscall: unknown rax=$rax]\n")
                            }
                        }
                    }
                    "int" -> {
                        val intNum = inst.operands.firstOrNull()?.removePrefix("0x")?.toIntOrNull(16) ?: 0x80
                        if (intNum == 0x80) { // 32-bit Linux ABI
                            val eax = getReg64("EAX", registers)
                            when (eax) {
                                4L -> { // sys_write(ebx, ecx, edx)
                                    val fd = getReg64("EBX", registers)
                                    val bufPtr = getReg64("ECX", registers).toInt()
                                    val count = getReg64("EDX", registers).toInt().coerceIn(0, 4096)
                                    if (bufPtr in 0 until MEMORY_SIZE) {
                                        val str = String(memory, bufPtr, count.coerceAtMost(MEMORY_SIZE - bufPtr), Charsets.UTF_8)
                                        if (fd == 2L) stderrBuilder.append(str) else stdoutBuilder.append(str)
                                    }
                                    setReg64("EAX", count.toLong(), registers)
                                }
                                1L -> { // sys_exit(ebx)
                                    exitCode = getReg64("EBX", registers).toInt()
                                    terminated = true
                                }
                            }
                        }
                    }
                    "nop" -> {}
                }
            }

            if (cycles >= MAX_CYCLES) {
                stderrBuilder.append("\n[Warning: Execution reached safety cycle limit (100,000 cycles)]\n")
            }

            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = stderrBuilder.isEmpty(),
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                exitCode = exitCode,
                executionTimeMs = elapsed,
                registers = registers,
                disassembly = disassembly
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return ExecutionResult(
                success = false,
                stdout = stdoutBuilder.toString(),
                stderr = (stderrBuilder.toString() + "\nAssembler Error: " + (e.message ?: "Unknown error")).trim(),
                exitCode = -1,
                executionTimeMs = elapsed
            )
        }
    }

    private fun splitOperands(str: String): List<String> {
        val result = mutableListOf<String>()
        var inQuote = false
        var quoteChar = ' '
        val current = StringBuilder()

        for (ch in str) {
            if (ch == '\'' || ch == '"') {
                if (!inQuote) {
                    inQuote = true
                    quoteChar = ch
                } else if (ch == quoteChar) {
                    inQuote = false
                }
                current.append(ch)
            } else if (ch == ',' && !inQuote) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

    private fun parseByteList(valuesStr: String, symbols: Map<String, Long>): List<Byte> {
        val bytes = mutableListOf<Byte>()
        val parts = splitOperands(valuesStr)
        for (part in parts) {
            val p = part.trim()
            if ((p.startsWith('"') && p.endsWith('"')) || (p.startsWith('\'') && p.endsWith('\''))) {
                val s = p.substring(1, p.length - 1)
                bytes.addAll(s.toByteArray(Charsets.UTF_8).toList())
            } else {
                val v = parseImmediate(p, symbols)
                bytes.add((v and 0xFF).toByte())
            }
        }
        return bytes
    }

    private fun parseImmediate(expr: String, symbols: Map<String, Long>): Long {
        val clean = expr.trim()
        symbols[clean]?.let { return it }

        if (clean.startsWith("0x", ignoreCase = true)) {
            return clean.substring(2).toLongOrNull(16) ?: 0L
        }
        if (clean.endsWith("h", ignoreCase = true) && clean.length > 1) {
            return clean.substring(0, clean.length - 1).toLongOrNull(16) ?: 0L
        }
        return clean.toLongOrNull() ?: 0L
    }

    private fun evaluateAddress(expr: String, registers: Map<String, Long>, symbols: Map<String, Long>): Long {
        symbols[expr.trim()]?.let { return it }
        val regVal = getReg64(expr.trim(), registers)
        return regVal
    }

    private fun evaluateOperand(op: String, registers: Map<String, Long>, memory: ByteArray, symbols: Map<String, Long>): Long {
        val trimmed = op.trim()
        if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            val addr = evaluateAddress(inner, registers, symbols).toInt()
            return if (addr in 0 until (MEMORY_SIZE - 7)) readMemory64(memory, addr) else 0L
        }
        symbols[trimmed]?.let { return it }
        if (isRegister(trimmed)) {
            return getReg64(trimmed, registers)
        }
        return parseImmediate(trimmed, symbols)
    }

    private fun writeRegisterOrMemory(dst: String, value: Long, registers: MutableMap<String, Long>, memory: ByteArray, symbols: Map<String, Long>) {
        val trimmed = dst.trim()
        if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            val addr = evaluateAddress(inner, registers, symbols).toInt()
            if (addr in 0 until (MEMORY_SIZE - 7)) {
                writeMemory64(memory, addr, value)
            }
            return
        }
        setReg64(trimmed, value, registers)
    }

    private fun isRegister(name: String): Boolean {
        val upper = name.uppercase(Locale.ROOT)
        return listOf(
            "RAX", "RBX", "RCX", "RDX", "RSI", "RDI", "RBP", "RSP",
            "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15",
            "EAX", "EBX", "ECX", "EDX", "ESI", "EDI", "EBP", "ESP",
            "AX", "BX", "CX", "DX", "SI", "DI", "BP", "SP",
            "AL", "BL", "CL", "DL", "AH", "BH", "CH", "DH"
        ).contains(upper)
    }

    private fun getReg64(reg: String, registers: Map<String, Long>): Long {
        val upper = reg.uppercase(Locale.ROOT)
        return when (upper) {
            "EAX" -> (registers["RAX"] ?: 0L) and 0xFFFFFFFFL
            "EBX" -> (registers["RBX"] ?: 0L) and 0xFFFFFFFFL
            "ECX" -> (registers["RCX"] ?: 0L) and 0xFFFFFFFFL
            "EDX" -> (registers["RDX"] ?: 0L) and 0xFFFFFFFFL
            "ESI" -> (registers["RSI"] ?: 0L) and 0xFFFFFFFFL
            "EDI" -> (registers["RDI"] ?: 0L) and 0xFFFFFFFFL
            "EBP" -> (registers["RBP"] ?: 0L) and 0xFFFFFFFFL
            "ESP" -> (registers["RSP"] ?: 0L) and 0xFFFFFFFFL
            "AX" -> (registers["RAX"] ?: 0L) and 0xFFFFL
            "AL" -> (registers["RAX"] ?: 0L) and 0xFFL
            "AH" -> ((registers["RAX"] ?: 0L) shr 8) and 0xFFL
            "BL" -> (registers["RBX"] ?: 0L) and 0xFFL
            "CL" -> (registers["RCX"] ?: 0L) and 0xFFL
            "DL" -> (registers["RDX"] ?: 0L) and 0xFFL
            else -> registers[upper] ?: 0L
        }
    }

    private fun setReg64(reg: String, value: Long, registers: MutableMap<String, Long>) {
        val upper = reg.uppercase(Locale.ROOT)
        when (upper) {
            "EAX" -> registers["RAX"] = value and 0xFFFFFFFFL
            "EBX" -> registers["RBX"] = value and 0xFFFFFFFFL
            "ECX" -> registers["RCX"] = value and 0xFFFFFFFFL
            "EDX" -> registers["RDX"] = value and 0xFFFFFFFFL
            "ESI" -> registers["RSI"] = value and 0xFFFFFFFFL
            "EDI" -> registers["RDI"] = value and 0xFFFFFFFFL
            "EBP" -> registers["RBP"] = value and 0xFFFFFFFFL
            "ESP" -> registers["RSP"] = value and 0xFFFFFFFFL
            "AX" -> registers["RAX"] = ((registers["RAX"] ?: 0L) and 0xFFFF.inv()) or (value and 0xFFFFL)
            "AL" -> registers["RAX"] = ((registers["RAX"] ?: 0L) and 0xFF.inv()) or (value and 0xFFL)
            "AH" -> registers["RAX"] = ((registers["RAX"] ?: 0L) and 0xFF00.inv()) or ((value and 0xFFL) shl 8)
            "BL" -> registers["RBX"] = ((registers["RBX"] ?: 0L) and 0xFF.inv()) or (value and 0xFFL)
            "CL" -> registers["RCX"] = ((registers["RCX"] ?: 0L) and 0xFF.inv()) or (value and 0xFFL)
            "DL" -> registers["RDX"] = ((registers["RDX"] ?: 0L) and 0xFF.inv()) or (value and 0xFFL)
            else -> registers[upper] = value
        }
    }

    private fun readMemory64(memory: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + 7 >= memory.size) return 0L
        var v = 0L
        for (i in 0..7) {
            v = v or ((memory[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return v
    }

    private fun writeMemory64(memory: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + 7 >= memory.size) return
        for (i in 0..7) {
            memory[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
    }

    private fun writeMemory32(memory: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + 3 >= memory.size) return
        for (i in 0..3) {
            memory[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
    }

    private fun writeMemory16(memory: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + 1 >= memory.size) return
        memory[offset] = (value and 0xFF).toByte()
        memory[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun generateSyntheticHex(opcode: String, operands: List<String>): String {
        return when (opcode) {
            "syscall" -> "0F 05"
            "int" -> "CD 80"
            "nop" -> "90"
            "ret" -> "C3"
            "mov" -> "48 C7 C0 " + (operands.lastOrNull()?.take(2)?.padStart(2, '0') ?: "00")
            "add" -> "48 01 D8"
            "sub" -> "48 29 D8"
            "xor" -> "48 31 F6"
            "inc" -> "48 FF C0"
            "dec" -> "48 FF C8"
            "cmp" -> "48 39 D0"
            "jmp" -> "E9 00 00 00 00"
            "je", "jz" -> "74 05"
            "jne", "jnz" -> "75 05"
            "push" -> "50"
            "pop" -> "58"
            else -> "48 89 C0"
        }
    }
}
