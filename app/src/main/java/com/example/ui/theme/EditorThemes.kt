package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

data class EditorThemeColors(
    val theme: AppTheme,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val accent: Color,
    val editorBackground: Color,
    val gutterBackground: Color,
    val gutterTextColor: Color,
    val activeLineColor: Color,
    val textColor: Color,
    val keywordColor: Color,
    val typeColor: Color,
    val stringColor: Color,
    val commentColor: Color,
    val numberColor: Color,
    val functionColor: Color,
    val operatorColor: Color,
    val isDark: Boolean
)

object ThemeRegistry {
    val Obsidian = EditorThemeColors(
        theme = AppTheme.OBSIDIAN,
        background = Color(0xFF090D14),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF1E293B),
        primary = Color(0xFF38BDF8),
        accent = Color(0xFF818CF8),
        editorBackground = Color(0xFF07090E),
        gutterBackground = Color(0xFF0C101A),
        gutterTextColor = Color(0xFF475569),
        activeLineColor = Color(0xFF131B2E),
        textColor = Color(0xFFE2E8F0),
        keywordColor = Color(0xFF38BDF8),
        typeColor = Color(0xFF2DD4BF),
        stringColor = Color(0xFFA7F3D0),
        commentColor = Color(0xFF64748B),
        numberColor = Color(0xFFFBBF24),
        functionColor = Color(0xFF60A5FA),
        operatorColor = Color(0xFFF472B6),
        isDark = true
    )

    val MonokaiPro = EditorThemeColors(
        theme = AppTheme.MONOKAI,
        background = Color(0xFF222024),
        surface = Color(0xFF2D2A2E),
        surfaceVariant = Color(0xFF3A373B),
        primary = Color(0xFFFFD866),
        accent = Color(0xFFA9DC76),
        editorBackground = Color(0xFF1D1B1E),
        gutterBackground = Color(0xFF222024),
        gutterTextColor = Color(0xFF727072),
        activeLineColor = Color(0xFF2E2B2F),
        textColor = Color(0xFFFCFCFA),
        keywordColor = Color(0xFFFF6188),
        typeColor = Color(0xFF78DCE8),
        stringColor = Color(0xFFFFD866),
        commentColor = Color(0xFF727072),
        numberColor = Color(0xFFAB9DF2),
        functionColor = Color(0xFFA9DC76),
        operatorColor = Color(0xFFFC9867),
        isDark = true
    )

    val Dracula = EditorThemeColors(
        theme = AppTheme.DRACULA,
        background = Color(0xFF1E1F29),
        surface = Color(0xFF282A36),
        surfaceVariant = Color(0xFF44475A),
        primary = Color(0xFFBD93F9),
        accent = Color(0xFFFF79C6),
        editorBackground = Color(0xFF191A21),
        gutterBackground = Color(0xFF21222C),
        gutterTextColor = Color(0xFF6272A4),
        activeLineColor = Color(0xFF343746),
        textColor = Color(0xFFF8F8F2),
        keywordColor = Color(0xFFFF79C6),
        typeColor = Color(0xFF8BE9FD),
        stringColor = Color(0xFFF1FA8C),
        commentColor = Color(0xFF6272A4),
        numberColor = Color(0xFFBD93F9),
        functionColor = Color(0xFF50FA7B),
        operatorColor = Color(0xFFFFB86C),
        isDark = true
    )

    val NordFrost = EditorThemeColors(
        theme = AppTheme.NORD,
        background = Color(0xFF242933),
        surface = Color(0xFF2E3440),
        surfaceVariant = Color(0xFF3B4252),
        primary = Color(0xFF88C0D0),
        accent = Color(0xFF81A1C1),
        editorBackground = Color(0xFF20242D),
        gutterBackground = Color(0xFF262C38),
        gutterTextColor = Color(0xFF616E88),
        activeLineColor = Color(0xFF303746),
        textColor = Color(0xFFECEFF4),
        keywordColor = Color(0xFF81A1C1),
        typeColor = Color(0xFF8FBCBB),
        stringColor = Color(0xFFA3BE8C),
        commentColor = Color(0xFF616E88),
        numberColor = Color(0xFFB48EAD),
        functionColor = Color(0xFF88C0D0),
        operatorColor = Color(0xFFEBCB8B),
        isDark = true
    )

    val Cyberpunk = EditorThemeColors(
        theme = AppTheme.CYBERPUNK,
        background = Color(0xFF0B0517),
        surface = Color(0xFF160E2A),
        surfaceVariant = Color(0xFF241544),
        primary = Color(0xFF00F0FF),
        accent = Color(0xFFFF007F),
        editorBackground = Color(0xFF070310),
        gutterBackground = Color(0xFF0E071F),
        gutterTextColor = Color(0xFF6E5696),
        activeLineColor = Color(0xFF1F123C),
        textColor = Color(0xFFE8E5F5),
        keywordColor = Color(0xFFFF007F),
        typeColor = Color(0xFF00F0FF),
        stringColor = Color(0xFF05FFA1),
        commentColor = Color(0xFF755B9E),
        numberColor = Color(0xFFFFE600),
        functionColor = Color(0xFF7928CA),
        operatorColor = Color(0xFFFF70A6),
        isDark = true
    )

    val SolarizedLight = EditorThemeColors(
        theme = AppTheme.SOLARIZED_LIGHT,
        background = Color(0xFFFDF6E3),
        surface = Color(0xFFEEE8D5),
        surfaceVariant = Color(0xFFE0D8C0),
        primary = Color(0xFF268BD2),
        accent = Color(0xFF859900),
        editorBackground = Color(0xFFFAF2DC),
        gutterBackground = Color(0xFFECE5CF),
        gutterTextColor = Color(0xFF93A1A1),
        activeLineColor = Color(0xFFE7DFCA),
        textColor = Color(0xFF073642),
        keywordColor = Color(0xFF268BD2),
        typeColor = Color(0xFFB58900),
        stringColor = Color(0xFF2AA198),
        commentColor = Color(0xFF93A1A1),
        numberColor = Color(0xFFD33682),
        functionColor = Color(0xFF859900),
        operatorColor = Color(0xFFCB4B16),
        isDark = false
    )

    val MinimalWhite = EditorThemeColors(
        theme = AppTheme.MINIMAL_WHITE,
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        primary = Color(0xFF0284C7),
        accent = Color(0xFF475569),
        editorBackground = Color(0xFFFFFFFF),
        gutterBackground = Color(0xFFF8FAFC),
        gutterTextColor = Color(0xFF94A3B8),
        activeLineColor = Color(0xFFF1F5F9),
        textColor = Color(0xFF0F172A),
        keywordColor = Color(0xFF0284C7),
        typeColor = Color(0xFF0D9488),
        stringColor = Color(0xFF16A34A),
        commentColor = Color(0xFF94A3B8),
        numberColor = Color(0xFFEA580C),
        functionColor = Color(0xFF7C3AED),
        operatorColor = Color(0xFFE11D48),
        isDark = false
    )

    fun getTheme(theme: AppTheme): EditorThemeColors {
        return when (theme) {
            AppTheme.OBSIDIAN -> Obsidian
            AppTheme.MONOKAI -> MonokaiPro
            AppTheme.DRACULA -> Dracula
            AppTheme.NORD -> NordFrost
            AppTheme.CYBERPUNK -> Cyberpunk
            AppTheme.SOLARIZED_LIGHT -> SolarizedLight
            AppTheme.MINIMAL_WHITE -> MinimalWhite
        }
    }
}
