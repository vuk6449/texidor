package com.example.editor.syntax

data class CodeSnippet(
    val label: String,
    val insertText: String,
    val description: String
)

enum class EditorLanguage(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val commentPrefix: String,
    val keywords: Set<String>,
    val builtInTypes: Set<String>,
    val builtInFunctions: Set<String>,
    val snippets: List<CodeSnippet>
) {
    KOTLIN(
        id = "kotlin",
        displayName = "Kotlin",
        extensions = listOf("kt", "kts"),
        commentPrefix = "//",
        keywords = setOf(
            "package", "import", "class", "interface", "object", "val", "var", "fun",
            "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
            "throw", "is", "as", "in", "override", "suspend", "private", "protected",
            "public", "internal", "data", "sealed", "enum", "companion", "by", "lazy",
            "true", "false", "null", "this", "super", "constructor", "init"
        ),
        builtInTypes = setOf(
            "String", "Int", "Long", "Float", "Double", "Boolean", "Char", "Byte", "Short",
            "Any", "Unit", "Nothing", "List", "MutableList", "Map", "MutableMap", "Set",
            "Flow", "StateFlow", "CoroutineScope", "Job", "Deferred", "Modifier", "Composable"
        ),
        builtInFunctions = setOf(
            "println", "print", "listOf", "mutableListOf", "mapOf", "mutableMapOf", "setOf",
            "filter", "map", "forEach", "firstOrNull", "take", "drop", "let", "also", "apply",
            "run", "with", "launch", "async", "delay", "remember", "mutableStateOf"
        ),
        snippets = listOf(
            CodeSnippet("fun", "fun name() {\n    \n}", "Function definition"),
            CodeSnippet("data class", "data class Name(\n    val id: Long,\n    val title: String\n)", "Data class declaration"),
            CodeSnippet("when", "when (val) {\n    case1 -> {}\n    else -> {}\n}", "When expression"),
            CodeSnippet("launch", "CoroutineScope(Dispatchers.IO).launch {\n    \n}", "Coroutine launch"),
            CodeSnippet("composable", "@Composable\nfun Screen() {\n    \n}", "Compose UI function")
        )
    ),

    PYTHON(
        id = "python",
        displayName = "Python",
        extensions = listOf("py", "pyw"),
        commentPrefix = "#",
        keywords = setOf(
            "def", "class", "import", "from", "as", "return", "if", "elif", "else",
            "for", "while", "break", "continue", "try", "except", "finally", "raise",
            "with", "yield", "lambda", "pass", "None", "True", "False", "and", "or",
            "not", "in", "is", "async", "await", "global", "nonlocal", "assert"
        ),
        builtInTypes = setOf(
            "str", "int", "float", "bool", "list", "dict", "set", "tuple", "bytes",
            "List", "Dict", "Set", "Tuple", "Optional", "Any", "Union"
        ),
        builtInFunctions = setOf(
            "print", "len", "range", "enumerate", "zip", "sum", "min", "max", "sorted",
            "map", "filter", "isinstance", "type", "open", "input", "abs", "round"
        ),
        snippets = listOf(
            CodeSnippet("def", "def function_name(param):\n    \"\"\"Docstring\"\"\"\n    return None", "Function definition"),
            CodeSnippet("class", "class ClassName:\n    def __init__(self):\n        pass", "Class definition"),
            CodeSnippet("if __main__", "if __name__ == '__main__':\n    main()", "Main guard"),
            CodeSnippet("try-except", "try:\n    pass\nexcept Exception as e:\n    print(e)", "Try/Except block"),
            CodeSnippet("for in", "for item in collection:\n    print(item)", "For loop")
        )
    ),

    JAVASCRIPT(
        id = "javascript",
        displayName = "JavaScript",
        extensions = listOf("js", "jsx", "mjs", "cjs"),
        commentPrefix = "//",
        keywords = setOf(
            "const", "let", "var", "function", "return", "if", "else", "switch", "case",
            "default", "for", "while", "do", "break", "continue", "try", "catch", "finally",
            "throw", "new", "class", "extends", "import", "export", "from", "async", "await",
            "yield", "typeof", "instanceof", "this", "super", "null", "undefined", "true", "false",
            "debugger", "in", "of", "delete", "void"
        ),
        builtInTypes = setOf(
            "String", "Number", "Boolean", "Object", "Array", "Function", "Promise",
            "Map", "Set", "Date", "RegExp", "Error", "JSON", "Math", "Symbol", "BigInt"
        ),
        builtInFunctions = setOf(
            "console.log", "console.error", "setTimeout", "setInterval", "fetch",
            "document.getElementById", "document.querySelector", "JSON.stringify",
            "JSON.parse", "parseInt", "parseFloat", "addEventListener"
        ),
        snippets = listOf(
            CodeSnippet("func", "function name(param) {\n  \n}", "Function declaration"),
            CodeSnippet("arrow", "const func = (param) => {\n  \n};", "Arrow function"),
            CodeSnippet("async", "async function fetchData() {\n  const res = await fetch(url);\n  return res.json();\n}", "Async fetch function"),
            CodeSnippet("clg", "console.log();", "Console log")
        )
    ),

    TYPESCRIPT(
        id = "typescript",
        displayName = "TypeScript",
        extensions = listOf("ts", "tsx", "mts", "cts"),
        commentPrefix = "//",
        keywords = setOf(
            "interface", "type", "enum", "implements", "declare", "namespace", "module",
            "abstract", "as", "is", "keyof", "readonly", "infer", "never", "unknown", "any",
            "override", "satisfies", "asserts", "const", "let", "var", "function", "return",
            "if", "else", "switch", "case", "default", "for", "while", "do", "break", "continue",
            "try", "catch", "finally", "throw", "new", "class", "extends", "import", "export",
            "from", "async", "await", "yield", "typeof", "instanceof", "this", "super", "public",
            "private", "protected", "static", "null", "undefined", "true", "false"
        ),
        builtInTypes = setOf(
            "string", "number", "boolean", "void", "any", "unknown", "never", "object",
            "Array", "Record", "Partial", "Required", "Readonly", "Pick", "Omit", "Exclude",
            "Extract", "NonNullable", "Parameters", "ReturnType", "InstanceType", "Promise"
        ),
        builtInFunctions = setOf(
            "console.log", "console.error", "setTimeout", "setInterval", "fetch",
            "document.getElementById", "document.querySelector", "JSON.stringify", "JSON.parse"
        ),
        snippets = listOf(
            CodeSnippet("interface", "interface InterfaceName {\n  id: string;\n  name: string;\n  isActive?: boolean;\n}", "Interface declaration"),
            CodeSnippet("type", "type CustomType = {\n  id: string;\n  value: number;\n};", "Type alias"),
            CodeSnippet("enum", "enum Status {\n  Active = 'ACTIVE',\n  Inactive = 'INACTIVE',\n}", "Enum declaration"),
            CodeSnippet("func:typed", "function calculate(a: number, b: number): number {\n  return a + b;\n}", "Typed function"),
            CodeSnippet("generic", "function identity<T>(arg: T): T {\n  return arg;\n}", "Generic function")
        )
    ),

    JAVA(
        id = "java",
        displayName = "Java",
        extensions = listOf("java"),
        commentPrefix = "//",
        keywords = setOf(
            "public", "private", "protected", "class", "interface", "extends", "implements",
            "static", "final", "void", "return", "new", "this", "super", "if", "else",
            "for", "while", "do", "switch", "case", "default", "break", "continue",
            "try", "catch", "finally", "throw", "throws", "import", "package", "true", "false", "null"
        ),
        builtInTypes = setOf(
            "String", "int", "long", "float", "double", "boolean", "char", "byte", "short",
            "Integer", "Long", "Float", "Double", "Boolean", "List", "ArrayList", "Map", "HashMap", "Set"
        ),
        builtInFunctions = setOf("System.out.println", "System.err.println", "equals", "toString", "hashCode"),
        snippets = listOf(
            CodeSnippet("psvm", "public static void main(String[] args) {\n    \n}", "Main method"),
            CodeSnippet("sout", "System.out.println();", "Print line")
        )
    ),

    C_CPP(
        id = "cpp",
        displayName = "C / C++",
        extensions = listOf("c", "cpp", "h", "hpp", "cc"),
        commentPrefix = "//",
        keywords = setOf(
            "#include", "#define", "#ifdef", "#ifndef", "#endif", "using", "namespace",
            "class", "struct", "template", "typename", "public", "private", "protected",
            "int", "long", "float", "double", "char", "bool", "void", "auto", "const",
            "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
            "new", "delete", "nullptr", "true", "false"
        ),
        builtInTypes = setOf("std::string", "std::vector", "std::map", "std::cout", "std::cin", "std::endl"),
        builtInFunctions = setOf("printf", "scanf", "malloc", "free", "cout", "cin"),
        snippets = listOf(
            CodeSnippet("main", "int main(int argc, char* argv[]) {\n    \n    return 0;\n}", "Main function"),
            CodeSnippet("cout", "std::cout << \"text\" << std::endl;", "Print stream")
        )
    ),

    RUST(
        id = "rust",
        displayName = "Rust",
        extensions = listOf("rs"),
        commentPrefix = "//",
        keywords = setOf(
            "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "match", "if", "else",
            "for", "while", "loop", "return", "use", "mod", "crate", "self", "Self", "true", "false",
            "async", "await", "where", "type", "const", "ref"
        ),
        builtInTypes = setOf("String", "str", "i32", "i64", "u32", "u64", "f32", "f64", "bool", "Option", "Result", "Vec"),
        builtInFunctions = setOf("println!", "print!", "format!", "panic!", "vec!"),
        snippets = listOf(
            CodeSnippet("fn main", "fn main() {\n    println!(\"Hello\");\n}", "Main function"),
            CodeSnippet("struct", "struct Name {\n    field: String,\n}", "Struct definition")
        )
    ),

    GO(
        id = "go",
        displayName = "Go",
        extensions = listOf("go"),
        commentPrefix = "//",
        keywords = setOf(
            "package", "import", "func", "return", "var", "const", "type", "struct", "interface",
            "if", "else", "for", "range", "switch", "case", "default", "go", "defer", "chan",
            "select", "make", "new", "nil", "true", "false"
        ),
        builtInTypes = setOf("string", "int", "int64", "float64", "bool", "byte", "error"),
        builtInFunctions = setOf("fmt.Println", "fmt.Printf", "len", "cap", "append"),
        snippets = listOf(
            CodeSnippet("main", "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello\")\n}", "Main package"),
            CodeSnippet("func", "func name(param string) error {\n    return nil\n}", "Function")
        )
    ),

    SQL(
        id = "sql",
        displayName = "SQL",
        extensions = listOf("sql"),
        commentPrefix = "--",
        keywords = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "CREATE", "TABLE", "DROP", "ALTER", "ADD", "JOIN", "INNER", "LEFT", "RIGHT", "ON",
            "GROUP", "BY", "ORDER", "ASC", "DESC", "HAVING", "LIMIT", "OFFSET", "DISTINCT",
            "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL", "AS", "PRIMARY", "KEY"
        ),
        builtInTypes = setOf("VARCHAR", "INT", "BIGINT", "TEXT", "BOOLEAN", "TIMESTAMP", "DATE", "FLOAT"),
        builtInFunctions = setOf("COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "NOW"),
        snippets = listOf(
            CodeSnippet("select", "SELECT * FROM table_name WHERE id = 1;", "Select statement"),
            CodeSnippet("create", "CREATE TABLE users (\n    id BIGINT PRIMARY KEY,\n    name VARCHAR(255)\n);", "Create table")
        )
    ),

    SHELL(
        id = "sh",
        displayName = "Shell / Bash",
        extensions = listOf("sh", "bash", "zsh"),
        commentPrefix = "#",
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "until", "do", "done",
            "in", "function", "select", "time", "return", "exit", "echo", "export", "source", "local"
        ),
        builtInTypes = emptySet(),
        builtInFunctions = setOf("echo", "cat", "grep", "sed", "awk", "curl", "chmod", "mkdir", "rm", "cp", "mv"),
        snippets = listOf(
            CodeSnippet("#!", "#!/bin/bash\nset -euo pipefail\n\n", "Bash shebang"),
            CodeSnippet("if", "if [ -f \"file\" ]; then\n    echo \"found\"\nfi", "If condition")
        )
    ),

    NASM(
        id = "asm",
        displayName = "NASM x86-64",
        extensions = listOf("asm", "nasm", "s"),
        commentPrefix = ";",
        keywords = setOf(
            "section", "segment", "global", "extern", "default", "rel", "abs",
            "mov", "lea", "add", "sub", "inc", "dec", "imul", "mul", "idiv", "div",
            "and", "or", "xor", "not", "shl", "shr", "cmp", "test",
            "jmp", "je", "jne", "jg", "jge", "jl", "jle", "jz", "jnz", "js", "jns",
            "call", "ret", "push", "pop", "syscall", "int", "nop", "equ"
        ),
        builtInTypes = setOf(
            "rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rbp", "rsp",
            "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15",
            "eax", "ebx", "ecx", "edx", "esi", "edi", "ebp", "esp",
            "al", "ah", "bl", "bh", "cl", "ch", "dl", "dh",
            "db", "dw", "dd", "dq", "resb", "resw", "resd", "resq",
            "byte", "word", "dword", "qword"
        ),
        builtInFunctions = setOf("sys_write", "sys_read", "sys_exit"),
        snippets = listOf(
            CodeSnippet("_start", "section .text\n    global _start\n_start:\n    mov rax, 60\n    mov rdi, 0\n    syscall", "Basic x86-64 program exit"),
            CodeSnippet("hello", "section .data\n    msg db \"Hello, World!\", 10\n    len equ $ - msg\n\nsection .text\n    global _start\n_start:\n    mov rax, 1\n    mov rdi, 1\n    mov rsi, msg\n    mov rdx, len\n    syscall\n\n    mov rax, 60\n    xor rdi, rdi\n    syscall", "Hello World x86-64 NASM"),
            CodeSnippet("mov", "mov rax, 1", "Move instruction"),
            CodeSnippet("loop", "    mov rcx, 5\nloop_label:\n    dec rcx\n    jnz loop_label", "Count loop")
        )
    ),

    HTML(
        id = "html",
        displayName = "HTML",
        extensions = listOf("html", "htm"),
        commentPrefix = "<!--",
        keywords = setOf(
            "html", "head", "body", "title", "meta", "link", "script", "style",
            "div", "span", "p", "a", "img", "button", "input", "form", "label",
            "table", "tr", "td", "th", "thead", "tbody", "ul", "ol", "li",
            "h1", "h2", "h3", "h4", "h5", "h6", "header", "footer", "nav", "section"
        ),
        builtInTypes = setOf(
            "class", "id", "src", "href", "alt", "style", "type", "value", "placeholder",
            "target", "rel", "name", "action", "method", "rows", "cols"
        ),
        builtInFunctions = emptySet(),
        snippets = listOf(
            CodeSnippet("html5", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Title</title>\n</head>\n<body>\n  \n</body>\n</html>", "HTML5 Boilerplate"),
            CodeSnippet("div.card", "<div class=\"card\">\n  \n</div>", "Card container"),
            CodeSnippet("button", "<button type=\"button\" class=\"btn\">\n  Click\n</button>", "Button tag")
        )
    ),

    CSS(
        id = "css",
        displayName = "CSS",
        extensions = listOf("css", "scss"),
        commentPrefix = "/*",
        keywords = setOf(
            "body", "div", "display", "flex", "grid", "position", "margin", "padding",
            "width", "height", "color", "background", "border", "border-radius", "font-family",
            "font-size", "font-weight", "justify-content", "align-items", "box-shadow",
            "transition", "transform", "cursor", "overflow", "z-index", "opacity"
        ),
        builtInTypes = setOf("px", "rem", "em", "%", "vh", "vw", "auto", "none", "solid", "rgba"),
        builtInFunctions = setOf("rgb", "rgba", "hsl", "calc", "var", "url", "linear-gradient"),
        snippets = listOf(
            CodeSnippet("center", "display: flex;\njustify-content: center;\nalign-items: center;", "Flexbox centering"),
            CodeSnippet("card-style", "background: #1E293B;\nborder-radius: 12px;\npadding: 20px;", "Card styling")
        )
    ),

    JSON_LANG(
        id = "json",
        displayName = "JSON",
        extensions = listOf("json"),
        commentPrefix = "//",
        keywords = setOf("true", "false", "null"),
        builtInTypes = emptySet(),
        builtInFunctions = emptySet(),
        snippets = listOf(
            CodeSnippet("object", "{\n  \"key\": \"value\"\n}", "JSON Object"),
            CodeSnippet("array", "[\n  \"item1\",\n  \"item2\"\n]", "JSON Array")
        )
    ),

    MARKDOWN(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf("md", "markdown"),
        commentPrefix = "<!--",
        keywords = setOf("#", "##", "###", "####", "**", "*", "`", "```", ">", "- [ ]", "- [x]"),
        builtInTypes = emptySet(),
        builtInFunctions = emptySet(),
        snippets = listOf(
            CodeSnippet("h1", "# Heading 1\n", "Top level heading"),
            CodeSnippet("table", "| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |", "Markdown Table"),
            CodeSnippet("codeblock", "```kotlin\n// code here\n```", "Code block")
        )
    ),

    BATCH(
        id = "bat",
        displayName = "Batch / CMD",
        extensions = listOf("bat", "cmd"),
        commentPrefix = "REM ",
        keywords = setOf(
            "@echo", "echo", "set", "goto", "if", "else", "call", "pause", "rem",
            "exit", "for", "in", "do", "shift", "start", "cls", "title", "color",
            "copy", "del", "move", "mkdir", "rmdir", "choice", "defined", "not",
            "exist", "errorlevel"
        ),
        builtInTypes = setOf(
            "%~dp0", "%1", "%2", "%3", "%4", "%*", "%ERRORLEVEL%", "%CD%",
            "%PATH%", "%DATE%", "%TIME%", "%RANDOM%"
        ),
        builtInFunctions = setOf("echo", "set", "cd", "md", "rd", "dir", "type", "ping"),
        snippets = listOf(
            CodeSnippet("@echo off", "@echo off\nsetlocal enabledelayedexpansion\n\nREM Script starts here\n", "Batch script template"),
            CodeSnippet("if exist", "if exist \"file.txt\" (\n    echo File exists\n) else (\n    echo Not found\n)", "Check file exists"),
            CodeSnippet("for /f", "for /f \"tokens=*\" %%a in (file.txt) do (\n    echo %%a\n)", "Loop lines")
        )
    ),

    MAKEFILE(
        id = "make",
        displayName = "Makefile",
        extensions = listOf("mk", "mak", "makefile"),
        commentPrefix = "#",
        keywords = setOf(
            "all", "clean", "build", "install", "test", "run", "dist",
            ".PHONY", "include", "define", "endef", "ifeq", "ifneq",
            "else", "endif", "export", "unexport", "override", "vpath"
        ),
        builtInTypes = setOf(
            "CC", "CXX", "CFLAGS", "CXXFLAGS", "LDFLAGS", "LDLIBS",
            "RM", "AR", "AS", "$@", "$<", "$^", "$*", "$?", "$%"
        ),
        builtInFunctions = setOf("wildcard", "patsubst", "subst", "shell", "dir", "notdir", "strip", "filter"),
        snippets = listOf(
            CodeSnippet("c_makefile", "CC = gcc\nCFLAGS = -Wall -O2\nTARGET = app\n\nall: $(TARGET)\n\n$(TARGET): main.o\n\t$(CC) $(CFLAGS) -o $@ $^\n\nclean:\n\trm -f *.o $(TARGET)\n\n.PHONY: all clean\n", "Standard C Makefile"),
            CodeSnippet("rule", "target: dependencies\n\tcommand -flags\n", "Makefile rule"),
            CodeSnippet(".PHONY", ".PHONY: all clean test\n", "Phony targets")
        )
    ),

    PLAIN_TEXT(
        id = "txt",
        displayName = "Plain Text",
        extensions = listOf("txt", "log", "conf", "env"),
        commentPrefix = "#",
        keywords = emptySet(),
        builtInTypes = emptySet(),
        builtInFunctions = emptySet(),
        snippets = emptyList()
    );

    companion object {
        fun fromExtension(extension: String): EditorLanguage {
            val ext = extension.lowercase().removePrefix(".")
            return entries.firstOrNull { it.extensions.contains(ext) } ?: PLAIN_TEXT
        }

        fun fromFileName(fileName: String): EditorLanguage {
            val name = fileName.lowercase().trim()
            if (name == "makefile" || name == "gnumakefile" || name.endsWith(".mk") || name.endsWith(".mak")) {
                return MAKEFILE
            }
            if (name.endsWith(".bat") || name.endsWith(".cmd")) {
                return BATCH
            }
            if (name.endsWith(".sh") || name.endsWith(".bash") || name.endsWith(".zsh")) {
                return SHELL
            }
            val ext = name.substringAfterLast('.', "")
            return fromExtension(ext)
        }

        fun fromId(id: String): EditorLanguage {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PLAIN_TEXT
        }
    }
}
