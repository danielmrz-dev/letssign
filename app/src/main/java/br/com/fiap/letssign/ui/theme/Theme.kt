package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeloColorScheme = lightColorScheme(
    primary = Purple600,
    onPrimary = Color.White,
    primaryContainer = Lavender100,
    onPrimaryContainer = Purple700,
    secondary = Indigo700,
    onSecondary = Color.White,
    background = Lavender50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Lavender100,
    onSurfaceVariant = Ink600,
    outline = Line,
    error = ErrorFg,
    onError = Color.White,
)

/** O design do selo. é claro; o app não usa cores dinâmicas para manter a identidade visual. */
@Composable
fun SeloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeloColorScheme,
        typography = Typography,
        content = content,
    )
}
