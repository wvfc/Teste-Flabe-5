package br.com.refrigeracaopro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Identidade visual: azul escuro, branco, cinza e detalhes em verde.
 * Suporta modo claro e escuro automaticamente.
 */
val AzulEscuro = Color(0xFF0D2C4F)
val AzulMedio = Color(0xFF1B4A7E)
val Verde = Color(0xFF2EA66B)
val VerdeClaro = Color(0xFF7BD3A8)
val CinzaClaro = Color(0xFFF2F4F7)
val CinzaMedio = Color(0xFF8A94A6)

private val EsquemaClaro = lightColorScheme(
    primary = AzulEscuro,
    onPrimary = Color.White,
    primaryContainer = AzulMedio,
    onPrimaryContainer = Color.White,
    secondary = Verde,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F2E4),
    onSecondaryContainer = Color(0xFF0A3D24),
    background = CinzaClaro,
    surface = Color.White,
    surfaceVariant = Color(0xFFE7EAF0),
    onSurfaceVariant = Color(0xFF44505F),
    outline = CinzaMedio,
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFF8FB7E8),
    onPrimary = Color(0xFF06203C),
    primaryContainer = AzulMedio,
    onPrimaryContainer = Color.White,
    secondary = VerdeClaro,
    onSecondary = Color(0xFF06301C),
    secondaryContainer = Color(0xFF1C5237),
    onSecondaryContainer = Color(0xFFD7F2E4),
    background = Color(0xFF101418),
    surface = Color(0xFF181D23),
    surfaceVariant = Color(0xFF252C35),
    onSurfaceVariant = Color(0xFFBCC6D4),
)

@Composable
fun RefrigeracaoProTheme(content: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
        content = content
    )
}
