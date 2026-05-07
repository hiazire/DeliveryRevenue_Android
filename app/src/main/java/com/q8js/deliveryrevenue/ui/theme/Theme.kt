package com.q8js.deliveryrevenue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark purple-orange palette
val Purple80 = Color(0xFFCB9EFF)
val Purple60 = Color(0xFFAB6EEF)
val Purple40 = Color(0xFF8B3ED9)
val PurpleDark = Color(0xFF1A0A2E)
val PurpleMid = Color(0xFF2D1B4E)
val PurpleContainer = Color(0xFF3D2060)

val Orange80 = Color(0xFFFFB366)
val Orange60 = Color(0xFFFF8C00)
val Orange40 = Color(0xFFE07000)
val OrangeLight = Color(0xFFFFD08A)

val BackgroundDark = Color(0xFF0D0515)
val SurfaceDark = Color(0xFF160D2A)
val SurfaceVariant = Color(0xFF221540)
val CardBackground = Color(0xFF1C1035)
val DividerColor = Color(0xFF2E1F50)

val ErrorColor = Color(0xFFFF5252)
val SuccessColor = Color(0xFF69F0AE)
val TextPrimary = Color(0xFFF0E6FF)
val TextSecondary = Color(0xFFB89FD8)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF1A0050),
    primaryContainer = PurpleContainer,
    onPrimaryContainer = Purple80,
    secondary = Orange80,
    onSecondary = Color(0xFF2A1500),
    secondaryContainer = Color(0xFF3D2200),
    onSecondaryContainer = OrangeLight,
    tertiary = Purple60,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    outline = DividerColor,
)

@Composable
fun DeliveryRevenueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
