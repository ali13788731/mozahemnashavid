package com.khalvat.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Brand {
    val Indigo = Color(0xFF4F46E5)
    val Violet = Color(0xFF7C3AED)
    val Cyan = Color(0xFF06B6D4)
    val Pink = Color(0xFFEC4899)

    val gradient = Brush.linearGradient(listOf(Indigo, Cyan))
    val gradientWarm = Brush.linearGradient(listOf(Violet, Pink))
}

/** ۰ = مطابق سیستم، ۱ = روشن، ۲ = تاریک */
object ThemeState {
    var mode by mutableIntStateOf(0)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF14124A),
    secondary = Color(0xFF0891B2),
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF4F5FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Color(0xFF50546B),
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9A9BFF),
    onPrimary = Color(0xFF14124A),
    primaryContainer = Color(0xFF2E2D7A),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFF4DD0E1),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF0D0F1F),
    surface = Color(0xFF171A2F),
    surfaceVariant = Color(0xFF252849),
    onSurfaceVariant = Color(0xFFB7BAD6),
    error = Color(0xFFF87171)
)

@Composable
fun KhalvatTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
