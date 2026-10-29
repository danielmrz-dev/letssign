package br.com.fiap.letssign.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta do selo. (derivada do roxo/índigo da LetsSign)
val Navy950 = Color(0xFF07073F)
val Navy900 = Color(0xFF0D0B5C)
val Indigo800 = Color(0xFF221A94)
val Indigo700 = Color(0xFF2A1C9C)
val Purple700 = Color(0xFF5A25B0)
val Purple600 = Color(0xFF6C2FC2)
val Purple500 = Color(0xFF7D45D6)
val Violet400 = Color(0xFFB04DFF)

val Lavender50 = Color(0xFFF5F2FF)
val Lavender100 = Color(0xFFEDE6FF)
val Lavender200 = Color(0xFFD9CCFA)
val DisabledPurple = Color(0xFFC6B3EC)

val Ink900 = Color(0xFF15114A)
val Ink600 = Color(0xFF5D5A7E)
val Ink400 = Color(0xFF8E8BA8)
val Line = Color(0xFFE4E0F0)

val SuccessBg = Color(0xFFE2F4E8)
val SuccessFg = Color(0xFF1E7A45)
val WarningBg = Color(0xFFFFF0D4)
val WarningFg = Color(0xFF8A5500)
val InfoBg = Color(0xFFE6ECFF)
val InfoFg = Color(0xFF2C4AB0)
val ErrorBg = Color(0xFFFDE8EC)
val ErrorFg = Color(0xFFC0263F)

/** Cor de cada signatário: usada no avatar e nos campos de assinatura. */
val SignerPalette = listOf(
    Color(0xFF6A2DB8),
    Color(0xFF2F74E0),
    Color(0xFFB23FE3),
    Color(0xFF0E7C86),
    Color(0xFFD9822B),
    Color(0xFF2E8B57),
)

fun signerColor(index: Int): Color {
    val size = SignerPalette.size
    return SignerPalette[((index % size) + size) % size]
}
