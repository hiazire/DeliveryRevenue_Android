@file:Suppress("DEPRECATION")
package com.q8js.deliveryrevenue.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.q8js.deliveryrevenue.data.*
import com.q8js.deliveryrevenue.ui.theme.*
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    images: List<ImageItem>,
    appState: AppState,
    emailState: EmailState,
    onAddImages: (List<Uri>) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onClearAll: () -> Unit,
    onProcess: () -> Unit,
    onSendEmail: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismissEmailResult: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) onAddImages(uris) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(emailState) {
        when (emailState) {
            is EmailState.Success -> {
                snackbarHostState.showSnackbar("✅ Email 已成功寄出")
                onDismissEmailResult()
            }
            is EmailState.Error -> {
                snackbarHostState.showSnackbar("❌ 寄送失敗：${emailState.message}")
                onDismissEmailResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.radialGradient(listOf(Orange80, Purple60)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "外送營業額",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "設定", tint = Purple80)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark,
        floatingActionButton = {
            if (images.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("選擇圖片") },
                    containerColor = Purple60,
                    contentColor = TextPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (images.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${images.size} 張圖片",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Row {
                                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Orange80, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("新增", color = Orange80, fontSize = 13.sp)
                                }
                                TextButton(onClick = onClearAll) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("清除", color = ErrorColor, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    items(images, key = { it.uri.toString() }) { item ->
                        ImageCard(item = item, onRemove = { onRemoveImage(item.uri) })
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }

                ResultPanel(
                    appState = appState,
                    emailState = emailState,
                    onProcess = onProcess,
                    onSendEmail = onSendEmail
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Purple40.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = Purple80.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "選擇外送平台截圖",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "支援多張圖片\n自動加總訂單金額",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ImageCard(item: ImageItem, onRemove: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(
            1.dp,
            when {
                item.error != null -> ErrorColor.copy(alpha = 0.5f)
                item.isProcessed && item.extractedAmounts.isNotEmpty() -> SuccessColor.copy(alpha = 0.3f)
                else -> DividerColor
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.uri.lastPathSegment?.substringAfterLast("/") ?: "圖片",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        item.dateTime?.format(dateTimeFmt) ?: "無日期 EXIF",
                        color = if (item.dateTime != null) TextSecondary else ErrorColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                when {
                    !item.isProcessed -> Text(
                        "待辨識",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    item.error != null -> Text(
                        "⚠ ${item.error}",
                        color = ErrorColor,
                        fontSize = 11.sp
                    )
                    else -> {
                        val total = item.extractedAmounts.sumOf { it.amount }
                        val fpCount = item.extractedAmounts.count { it.platform == PlatformType.FOODPANDA }
                        val ueCount = item.extractedAmounts.count { it.platform == PlatformType.UBER_EATS }
                        Text(
                            "NT$ ${"%.2f".format(total)}  (${item.extractedAmounts.size} 筆: FP $fpCount, UE $ueCount)",
                            color = SuccessColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultPanel(
    appState: AppState,
    emailState: EmailState,
    onProcess: () -> Unit,
    onSendEmail: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (appState) {
                is AppState.Idle -> {
                    Button(
                        onClick = onProcess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("開始辨識金額", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                is AppState.Processing -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = Purple80,
                            trackColor = SurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("辨識中，請稍候...", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                is AppState.Done -> {
                    val result = appState.result
                    if (result.hasDateConflict) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("圖片日期不一致", color = ErrorColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "偵測到多個日期：${result.dates.joinToString(", ") { it?.format(dateFmt) ?: "未知" }}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onSendEmail,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = emailState !is EmailState.Sending,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (emailState is EmailState.Sending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("寄出錯誤通知", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val normalTotal = result.normalFoodPanda + result.normalUberEats
                                val extendedTotal = result.extendedFoodPanda + result.extendedUberEats
                                val reportText = """
                                    【一般營業時間】
                                    FoodPanda：${"%.0f".format(result.normalFoodPanda)}
                                    Uber Eats：${"%.0f".format(result.normalUberEats)}
                                    一般營時 + 雙平台 Total：${"%.0f".format(normalTotal)}

                                    【延長營業時間】
                                    FoodPanda：${"%.0f".format(result.extendedFoodPanda)}
                                    Uber Eats：${"%.0f".format(result.extendedUberEats)}
                                    延長營時 + 雙平台 Total：${"%.0f".format(extendedTotal)}

                                    外送平台總金額：${"%.0f".format(result.totalAmount)}
                                """.trimIndent()

                                Text(
                                    text = reportText,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                if (result.primaryDate != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "日期: ${result.primaryDate.format(dateFmt)}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onSendEmail,
                                enabled = emailState !is EmailState.Sending,
                                colors = ButtonDefaults.buttonColors(containerColor = Purple60),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (emailState is EmailState.Sending) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("寄送報告", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is AppState.Error -> {
                    Text(
                        "錯誤：${appState.message}",
                        color = ErrorColor,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onProcess,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60)
                    ) {
                        Text("重試")
                    }
                }
            }
        }
    }
}
