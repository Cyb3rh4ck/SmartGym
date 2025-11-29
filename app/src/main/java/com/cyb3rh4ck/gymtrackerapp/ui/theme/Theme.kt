package com.cyb3rh4ck.gymtrackerapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ... imports ...

private val AdrenalineColorScheme = darkColorScheme(
    primary = AdrenalineRed,
    onPrimary = Color.White, // Texto sobre el rojo
    primaryContainer = AdrenalineRedDark,
    onPrimaryContainer = Color.White,

    secondary = NeonGreen, // Usaremos el verde neón para detalles secundarios (checks, etc)
    onSecondary = Color.Black,

    background = VoidBlack,
    onBackground = WhiteGhost,

    surface = CarbonGrey,
    onSurface = WhiteGhost,

    surfaceVariant = SteelGrey, // Para tarjetas o bordes suaves
    onSurfaceVariant = SilverMist,

    error = AlertOrange,
    onError = Color.Black
)

@Composable
fun GymTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // <--- IMPORTANTE: Pon esto en FALSE para que no use los colores de fondo de pantalla del usuario
    content: @Composable () -> Unit
) {
    // Forzamos la paleta oscura Adrenaline para ese look "Brutal" siempre,
    // o puedes dejar la lógica del sistema si prefieres.
    // Aquí asumo que quieres que la app se vea dark siempre para mantener la estética.
    val colorScheme = AdrenalineColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
