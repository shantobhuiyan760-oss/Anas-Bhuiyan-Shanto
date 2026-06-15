package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun TransKeyTheme(
    accentColorIndex: Int = 0, // 0=Purple, 1=Blue, 2=Pink, 3=Yellow, 4=Teal
    solidBgIndex: Int = 0,     // 0=Charcoal, 1=Navy, 2=Obsidian
    fontStyleIndex: Int = 0,   // 0=Inter, 1=Roboto, 2=Playfair Display
    content: @Composable () -> Unit
) {
    // Get customized theme colors
    val primaryAccent = when (accentColorIndex) {
        0 -> AccentPurple
        1 -> AccentBlue
        2 -> AccentPink
        3 -> AccentYellow
        4 -> AccentTeal
        else -> AccentPurple
    }

    val backgroundCanvas = when (solidBgIndex) {
        0 -> BgDeepCharcoal
        1 -> BgCosmicNavy
        2 -> BgObsidianSpace
        else -> BgDeepCharcoal
    }

    val selectedFontFamily = when (fontStyleIndex) {
        0 -> AppFonts.Inter
        1 -> AppFonts.Roboto
        2 -> AppFonts.PlayfairDisplay
        else -> AppFonts.Inter
    }

    // High quality mobile dark keyboard color values strictly per our design system guidelines
    val customColorScheme = darkColorScheme(
        primary = primaryAccent,
        onPrimary = Color.Black,
        background = backgroundCanvas,
        onBackground = Color(0xFFF2F2F7),
        surface = Color(0xFF1C1C24),
        onSurface = Color(0xFFF2F2F7),
        surfaceVariant = Color(0xFF121217),
        onSurfaceVariant = Color(0xFF8E8E93),
        outline = Color(0xFF2C2C35),
        outlineVariant = Color(0xFF1A1A23),
        secondaryContainer = Color(0xFF2A2A38),
        onSecondaryContainer = Color(0xFFC5C0FF)
    )

    // Build customized Material 3 Typography set inline
    val customTypography = androidx.compose.material3.Typography(
        displaySmall = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.02).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = selectedFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = customTypography,
        content = content
    )
}

