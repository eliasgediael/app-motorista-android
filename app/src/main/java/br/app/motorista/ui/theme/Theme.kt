package br.app.motorista.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Cores funcionais dos balões de análise (verde/amarelo/vermelho). */
object FunctionalColors {
    val Good = Color(0xFF1FA463)
    val Attention = Color(0xFFF2B01E)
    val Bad = Color(0xFFE5484D)
}

val Green = Color(0xFF1B8A5A)
val GreenSoft = Color(0xFFE6F4EC)
val SurfaceLight = Color(0xFFF6F8F7)
val TextPrimary = Color(0xFF15211B)
val TextSecondary = Color(0xFF5F6E66)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenSoft,
    onPrimaryContainer = Green,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outlineVariant = Color(0xFFDDE5E0),
)

// Variação escura (tela 50) ainda não refinada; apenas evita tema claro forçado.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CC98B),
    primaryContainer = Color(0xFF16382A),
    onPrimaryContainer = Color(0xFF4CC98B),
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
