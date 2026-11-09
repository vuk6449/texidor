package com.example

import com.example.editor.execution.NasmEmulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NasmEmulatorTest {

    @Test
    fun testNasmSysWriteAndExit() {
        val asm = """
            section .data
                msg db "Hello, x86-64 NASM!", 10
                len equ $ - msg

            section .text
                global _start

            _start:
                mov rax, 1
                mov rdi, 1
                lea rsi, [msg]
                mov rdx, len
                syscall

                mov rax, 40
                add rax, 2

                mov rdi, rax
                mov rax, 60
                syscall
        """.trimIndent()

        val result = NasmEmulator.assembleAndRun(asm)
        assertTrue("Execution should succeed", result.success)
        assertEquals("Hello, x86-64 NASM!\n", result.stdout)
        assertEquals(42, result.exitCode)
        assertNotNull(result.registers)
        assertEquals(42L, result.registers?.get("RDI"))
        assertEquals(60L, result.registers?.get("RAX"))
        assertNotNull(result.disassembly)
        assertTrue(result.disassembly!!.isNotEmpty())
    }

    @Test
    fun testNasmArithmeticAndJumps() {
        val asm = """
            section .text
                global _start

            _start:
                mov rcx, 5
                mov rax, 0

            loop_start:
                add rax, rcx
                dec rcx
                cmp rcx, 0
                jg loop_start

                mov rdi, rax
                mov rax, 60
                syscall
        """.trimIndent()

        val result = NasmEmulator.assembleAndRun(asm)
        assertTrue(result.success)
        assertEquals(15, result.exitCode) // 5 + 4 + 3 + 2 + 1 = 15
        assertEquals(15L, result.registers?.get("RDI"))
        assertEquals(0L, result.registers?.get("RCX"))
    }
}
