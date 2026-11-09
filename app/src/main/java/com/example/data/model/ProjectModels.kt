package com.example.data.model

data class Project(
    val id: String,
    val name: String,
    val rootPath: String,
    val isExternalFolder: Boolean = false,
    val gitBranch: String? = null,
    val gitRemoteUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)

data class ProjectFileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = "",
    val children: MutableList<ProjectFileNode> = mutableListOf(),
    var isExpanded: Boolean = true
)

enum class ProjectTemplateType(
    val title: String,
    val description: String,
    val iconName: String
) {
    WEB_APP(
        title = "HTML5 / CSS / TS Web App",
        description = "Modern single-page web application with auto-compiled TypeScript and live preview",
        iconName = "Language"
    ),
    REACT_TAILWIND(
        title = "React & CSS Studio",
        description = "Interactive component-driven web project with responsive design",
        iconName = "Code"
    ),
    NASM_ASSEMBLY(
        title = "NASM x86-64 Studio",
        description = "x86-64 assembly kernel with virtual CPU emulation and Linux syscalls",
        iconName = "Memory"
    ),
    NODE_JS(
        title = "Node.js / Express Script",
        description = "JavaScript / TypeScript automation and server scripts",
        iconName = "Terminal"
    ),
    MARKDOWN_DOCS(
        title = "Markdown Documentation",
        description = "GitHub-flavored markdown documents with tables, badges, and export",
        iconName = "Description"
    ),
    BLANK(
        title = "Empty Workspace",
        description = "Start with a clean slate for custom development",
        iconName = "Folder"
    )
}
