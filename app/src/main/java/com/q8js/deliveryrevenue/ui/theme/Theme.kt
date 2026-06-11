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

// ── Nishikigoi (深澤直人 × iida INFOBAR) ──────────────────────────────────
// 錦鯉 = 朱紅；池水 = 深藍灰；留白 = 溫白
val NishikiRed       = Color(0xFFC0392B)   // 錦鯉本體
val NishikiRedLight  = Color(0xFFE74C3C)   // 高光邊框
val NishikiRedDeep   = Color(0xFF962D22)   // 按下深色
val NishikiBlue      = Color(0xFF111B27)   // 池水深處（背景）
val NishikiBlueLight = Color(0xFF1E2D3D)   // 水面（背景輔助）
val NishikiWhite     = Color(0xFFF5F0EB)   // 留白 / 主文字
val NishikiCream     = Color(0xFFEFE8DE)   // 第二按鍵（米白）
val NishikiCreamDeep = Color(0xFFD6CECB)   // 米白按下深色
val NishikiGold      = Color(0xFFD4A855)   // 魚鱗反光（點綴）
val NishikiGoldFaint = Color(0x30D4A855)   // 水波 shimmer 透明金

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
