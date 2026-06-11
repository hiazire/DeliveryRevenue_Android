package com.q8js.deliveryrevenue.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.q8js.deliveryrevenue.R
import com.q8js.deliveryrevenue.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Google Font Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val fontName = GoogleFont("Noto Serif TC")
val NotoSerifTC = FontFamily(
    Font(googleFont = fontName, fontProvider = provider)
)

@Composable
fun HomeScreen(onNavigateToMain: () -> Unit) {
    // ── 水波 shimmer（背景緩慢漂移的橢圓漸層）────────────────────────────────
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -120f, targetValue = 120f,
        animationSpec = infiniteRepeatable(tween(7000, easing = EaseInOut), RepeatMode.Reverse),
        label = "shimmerX"
    )
    val shimmerY by shimmerTransition.animateFloat(
        initialValue = -80f, targetValue = 80f,
        animationSpec = infiniteRepeatable(tween(5500, easing = EaseInOut), RepeatMode.Reverse),
        label = "shimmerY"
    )

    // ── 根 Box ────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NishikiBlue)
            .drawBehind {
                val cx = size.width * 0.5f + shimmerX.dp.toPx()
                val cy = size.height * 0.38f + shimmerY.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NishikiGoldFaint, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = size.width * 0.72f
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.72f
                )
                val cx2 = size.width * 0.62f - shimmerX.dp.toPx() * 0.6f
                val cy2 = size.height * 0.65f + shimmerY.dp.toPx() * 0.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x18FFFFFF), Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = size.width * 0.45f
                    ),
                    center = Offset(cx2, cy2),
                    radius = size.width * 0.45f
                )
            }
    ) {
        // App 標題 (固定在上方)
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "外送管理",
                color = NishikiWhite.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                fontFamily = NotoSerifTC,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(36.dp).height(1.dp).background(NishikiGold.copy(alpha = 0.6f)))
        }

        // 按鍵區（垂直排列，置底）
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 按鍵 2: 日營業額回報 (米白長方形)
            DraggableNishikiButton(
                text = "日營業額回報",
                width = 180,
                height = 70,
                baseColor = NishikiCream,
                deepColor = NishikiCreamDeep,
                textColor = NishikiRed,
                dropDelay = 150,
                onClick = { /* 無功能 */ }
            )

            Spacer(Modifier.height(24.dp))

            // 按鍵 1: 外送營業額 (朱紅正方形 180x180)
            DraggableNishikiButton(
                text = "外送\n營業額",
                width = 180,
                height = 180,
                baseColor = NishikiRed,
                deepColor = NishikiRedDeep,
                textColor = NishikiWhite,
                dropDelay = 0,
                onClick = onNavigateToMain
            )
        }

        // 版本號
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            Text(
                text = "v1.0",
                color = NishikiWhite.copy(alpha = 0.15f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Light,
                fontFamily = NotoSerifTC,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun DraggableNishikiButton(
    text: String,
    width: Int,
    height: Int,
    baseColor: Color,
    deepColor: Color,
    textColor: Color,
    dropDelay: Int,
    onClick: () -> Unit
) {
    // 掉落動畫
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(dropDelay.toLong())
        isLaunched = true
    }
    val dropY by animateFloatAsState(
        targetValue = if (isLaunched) 0f else -2500f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "dropY"
    )

    // 拖曳與回彈邏輯
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    // 呼吸燈
    val breathTransition = rememberInfiniteTransition(label = "breath")
    val breathAlpha by breathTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "borderAlpha"
    )

    val tileShape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .offset { IntOffset(dragOffset.value.x.roundToInt(), (dragOffset.value.y + dropY).roundToInt()) }
            .size(width.dp, height.dp)
            .clip(tileShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, deepColor),
                    start = Offset(0f, 0f),
                    end = Offset(width.toFloat(), height.toFloat())
                )
            )
            .border(
                width = 1.5.dp,
                color = (if (baseColor == NishikiRed) NishikiRedLight else Color.White).copy(alpha = breathAlpha),
                shape = tileShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = textColor.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        scope.launch { dragOffset.stop() }
                    },
                    onDragEnd = {
                        scope.launch {
                            dragOffset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = 0.5f,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo(dragOffset.value + dragAmount)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 微光
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0x28FFFFFF), Color.Transparent),
                    center = Offset(40f, 40f),
                    radius = 160f
                )
            )
        )

        Text(
            text = text,
            color = textColor,
            fontSize = if (height > 100) 24.sp else 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = NotoSerifTC,
            letterSpacing = 0.15.sp,
            lineHeight = if (height > 100) 36.sp else 24.sp,
            textAlign = TextAlign.Center
        )
    }
}
