package com.q8js.deliveryrevenue.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.q8js.deliveryrevenue.data.AppState
import com.q8js.deliveryrevenue.ui.theme.*
import com.q8js.deliveryrevenue.util.EmailSender
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExpenseItem(val category: String, val amount: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    appState: AppState,
    settings: com.q8js.deliveryrevenue.data.AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 1. 日期選擇狀態
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }

    // 2. 營業額輸入狀態
    var normalFP by remember { mutableStateOf("") }
    var normalUE by remember { mutableStateOf("") }
    var extendedFP by remember { mutableStateOf("") }
    var extendedUE by remember { mutableStateOf("") }

    // 3. 現金與備用金狀態
    var reserveCash by remember { mutableStateOf("8000") } // 預設 8000
    var drawerCash by remember { mutableStateOf("") }

    // 4. 支出明細狀態（預設 3 個空槽位）
    val expenseList = remember {
        mutableStateListOf(
            ExpenseItem("進貨", ""),
            ExpenseItem("耗材", ""),
            ExpenseItem("雜支", "")
        )
    }

    // 常用支出分類
    val expenseCategories = listOf("進貨", "耗材", "雜支", "退款", "零用金", "其他")

    // 5. 帶入外送營業額試算提示狀態
    var hasAutofilled by remember { mutableStateOf(false) }
    val showAutofillPrompt = appState is AppState.Done && !hasAutofilled

    // 6. 信件寄送狀態
    var isSending by remember { mutableStateOf(false) }
    var sendResultMsg by remember { mutableStateOf<String?>(null) }

    // 自動加總計算
    val normalFPVal = normalFP.toDoubleOrNull() ?: 0.0
    val normalUEVal = normalUE.toDoubleOrNull() ?: 0.0
    val normalTotal = normalFPVal + normalUEVal

    val extendedFPVal = extendedFP.toDoubleOrNull() ?: 0.0
    val extendedUEVal = extendedUE.toDoubleOrNull() ?: 0.0
    val extendedTotal = extendedFPVal + extendedUEVal

    val reserveCashVal = reserveCash.toDoubleOrNull() ?: 0.0
    val reserveCashShort = (8000.0 - reserveCashVal).coerceAtLeast(0.0)

    val drawerCashVal = drawerCash.toDoubleOrNull() ?: 0.0
    val totalExpenses = expenseList.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    
    // 總外送營業額
    val totalDelivery = normalTotal + extendedTotal
    // 實際收銀機總額（收銀現金 + 備用金 - 支出）
    val actualDrawerTotal = drawerCashVal + reserveCashVal - totalExpenses

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NishikiBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // ── 頂部導航列 ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = NishikiWhite
                    )
                }
                Text(
                    text = "日營業額回報",
                    color = NishikiWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.bindParentPadding())
                )
            }

            // ── 表單內容 ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 日期選擇卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "回報日期",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentDate.format(dateFormatter),
                                color = NishikiWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showDatePicker = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NishikiCream,
                                contentColor = NishikiRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("調整", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. 外送營業額自動帶入提示
                if (showAutofillPrompt) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NishikiGold.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NishikiGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "今日外送營業額，已有試算結果。",
                                color = NishikiWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "請問是否自動帶入？",
                                color = NishikiWhite.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val result = (appState as AppState.Done).result
                                        normalFP = result.normalFoodPanda.toInt().toString()
                                        normalUE = result.normalUberEats.toInt().toString()
                                        extendedFP = result.extendedFoodPanda.toInt().toString()
                                        extendedUE = result.extendedUberEats.toInt().toString()
                                        hasAutofilled = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NishikiRed, contentColor = NishikiWhite),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("是", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { hasAutofilled = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NishikiBlueLight, contentColor = NishikiWhite),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("否")
                                }
                            }
                        }
                    }
                }

                // 3. 一般營業時間卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "一般營業時間 (00:00 - 13:29)",
                            color = NishikiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = normalFP,
                                onValueChange = { normalFP = it },
                                label = { Text("FP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = normalUE,
                                onValueChange = { normalUE = it },
                                label = { Text("UE") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("一般營時 & 雙平台加總:", color = TextSecondary, fontSize = 13.sp)
                            Text("${normalTotal.toInt()} 元", color = NishikiWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. 延長營業時間卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "延長營業時間 (13:30 - 23:59)",
                            color = NishikiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = extendedFP,
                                onValueChange = { extendedFP = it },
                                label = { Text("FP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = extendedUE,
                                onValueChange = { extendedUE = it },
                                label = { Text("UE") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("延長營時 & 雙平台加總:", color = TextSecondary, fontSize = 13.sp)
                            Text("${extendedTotal.toInt()} 元", color = NishikiWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 5. 備用金與收銀現金卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "現金與備用金管理",
                            color = NishikiGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = reserveCash,
                                onValueChange = { reserveCash = it },
                                label = { Text("實有備用金") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = drawerCash,
                                onValueChange = { drawerCash = it },
                                label = { Text("收銀機現金") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = getTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("備用金缺 (8000 - 備用金):", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${reserveShortText(reserveCashShort)} 元",
                                color = if (reserveCashShort > 0) NishikiRedLight else SuccessColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. 支出明細卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "支出項目",
                                color = NishikiGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // 新增支出按鍵 (加號)
                            IconButton(
                                onClick = {
                                    expenseList.add(ExpenseItem("進貨", ""))
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(NishikiGold, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "新增支出",
                                    tint = NishikiBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 支出明細列表
                        expenseList.forEachIndexed { index, expense ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 下拉式選單
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable { dropdownExpanded = true }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = expense.category,
                                            color = NishikiWhite,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "下拉",
                                            tint = TextSecondary
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.background(NishikiBlueLight)
                                    ) {
                                        expenseCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat, color = NishikiWhite) },
                                                onClick = {
                                                    expenseList[index] = expense.copy(category = cat)
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 金額輸入
                                OutlinedTextField(
                                    value = expense.amount,
                                    onValueChange = { valStr ->
                                        expenseList[index] = expense.copy(amount = valStr)
                                    },
                                    placeholder = { Text("金額") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = getTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                )

                                // 刪除按鈕
                                IconButton(
                                    onClick = { expenseList.removeAt(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "刪除",
                                        tint = NishikiRedLight.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (expenseList.isNotEmpty()) {
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("支出小計:", color = TextSecondary, fontSize = 13.sp)
                                Text("${totalExpenses.toInt()} 元", color = NishikiRedLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }

            // ── 底部發送按鈕 ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NishikiBlueLight)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        isSending = true
                        sendResultMsg = null
                        scope.launch {
                            try {
                                val dateStr = currentDate.format(dateFormatter)
                                val success = EmailSender.sendDailyReportEmail(
                                    cfg = settings,
                                    date = dateStr,
                                    normalFP = normalFPVal,
                                    normalUE = normalUEVal,
                                    extendedFP = extendedFPVal,
                                    extendedUE = extendedUEVal,
                                    reserveCash = reserveCashVal,
                                    reserveCashShort = reserveCashShort,
                                    drawerCash = drawerCashVal,
                                    expenses = expenseList.filter { it.amount.isNotEmpty() }.toList(),
                                    totalDelivery = totalDelivery,
                                    actualDrawerTotal = actualDrawerTotal
                                )
                                sendResultMsg = if (success) "✅ 日報已成功寄出！" else "❌ 寄送失敗，請確認 SMTP 設定。"
                            } catch (e: Exception) {
                                sendResultMsg = "❌ 錯誤: ${e.message}"
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NishikiRed,
                        contentColor = NishikiWhite,
                        disabledContainerColor = NishikiRedDeep
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = NishikiWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("確認並寄送日報", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── DatePickerDialog ──────────────────────────────────────────────
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                currentDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = NishikiRed)
                    ) {
                        Text("確定", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("不調整")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState
                )
            }
        }

        // ── 寄送結果彈窗 ──────────────────────────────────────────────────
        if (sendResultMsg != null) {
            Dialog(onDismissRequest = { sendResultMsg = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NishikiBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sendResultMsg ?: "",
                            color = NishikiWhite,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { sendResultMsg = null },
                            colors = ButtonDefaults.buttonColors(containerColor = NishikiRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("確認")
                        }
                    }
                }
            }
        }
    }
}

// 輔助函式：取得一致的 TextField 樣式
@Composable
private fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NishikiWhite,
    unfocusedTextColor = NishikiWhite,
    focusedBorderColor = NishikiGold,
    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
    focusedLabelColor = NishikiGold,
    unfocusedLabelColor = TextSecondary,
    cursorColor = NishikiGold
)

// 輔助函式：格式化備用金缺額文字
private fun reserveShortText(value: Double): String {
    return if (value <= 0) "0" else String.format("%.0f", value)
}

// 輔助擴充：解決 dp.toPx 在 padding 等無效的位置，只留間隔
private fun Int.bindParentPadding() = this.dp
