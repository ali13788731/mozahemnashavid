package com.khalvat.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.RuleKind

object Brand {
    val Indigo = Color(0xFF6366F1)
    val Violet = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF22D3EE)
    val Pink = Color(0xFFEC4899)
    val Mint = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)

    val hero = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF7C3AED), Color(0xFF0EA5E9)))
    val heroPaused = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF334155)))
    val call = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))
    val sms = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
    val focus = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))

    fun kindBrush(kind: Int): Brush = if (kind == RuleKind.CALL) call else sms
    fun kindColor(kind: Int): Color = if (kind == RuleKind.CALL) Mint else Violet
}

/** 0 = follow system, 1 = light, 2 = dark */
object ThemeState {
    var mode by mutableIntStateOf(0)
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9395FF),
    onPrimary = Color(0xFF0B0C2E),
    primaryContainer = Color(0xFF2A2B6B),
    onPrimaryContainer = Color(0xFFE2E2FF),
    secondary = Color(0xFF22D3EE),
    tertiary = Color(0xFFF472B6),
    background = Color(0xFF0A0D1C),
    onBackground = Color(0xFFE9EAF7),
    surface = Color(0xFF131730),
    onSurface = Color(0xFFE9EAF7),
    surfaceVariant = Color(0xFF1E2342),
    onSurfaceVariant = Color(0xFFA9AECF),
    outline = Color(0xFF3A4070),
    outlineVariant = Color(0xFF282D55),
    error = Color(0xFFFF6B7A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E4FF),
    onPrimaryContainer = Color(0xFF14124A),
    secondary = Color(0xFF0891B2),
    tertiary = Color(0xFFDB2777),
    background = Color(0xFFF3F4FC),
    onBackground = Color(0xFF14162B),
    surface = Color.White,
    onSurface = Color(0xFF14162B),
    surfaceVariant = Color(0xFFE9EBF8),
    onSurfaceVariant = Color(0xFF50546B),
    outline = Color(0xFFB4B8D6),
    outlineVariant = Color(0xFFDDE0F2),
    error = Color(0xFFDC2626)
)

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun isDarkTheme(): Boolean = when (ThemeState.mode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun KhalvatTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    val base = Typography()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = if (Fmt.isFa) base.withFont(Vazirmatn) else base,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        ),
        content = content
    )
}
