package com.example.data.model

enum class AppTheme(val displayName: String) {
    OBSIDIAN("Obsidian OLED"),
    MONOKAI("Monokai Pro"),
    DRACULA("Dracula"),
    NORD("Nord Frost"),
    CYBERPUNK("Cyberpunk"),
    SOLARIZED_LIGHT("Solarized Light"),
    MINIMAL_WHITE("Minimal White")
}

data class EditorPreferences(
    val theme: AppTheme = AppTheme.OBSIDIAN,
    val fontSizeSp: Float = 14f,
    val tabSpaces: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoIndent: Boolean = true,
    val autoCloseBrackets: Boolean = true,
    val isOledBlack: Boolean = true,
    val fontFamilyName: String = "Monospace", // "Monospace", "SansSerif", "Serif"
    val autoSaveEnabled: Boolean = true,
    val hideNavigationBar: Boolean = true
)
