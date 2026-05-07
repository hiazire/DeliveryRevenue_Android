@file:Suppress("DEPRECATION")
package com.q8js.deliveryrevenue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.q8js.deliveryrevenue.data.AppSettings
import com.q8js.deliveryrevenue.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    current: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var recipientEmail by remember(current) { mutableStateOf(current.recipientEmail) }
    var smtpHost by remember(current) { mutableStateOf(current.smtpHost) }
    var smtpPort by remember(current) { mutableStateOf(current.smtpPort.toString()) }
    var senderEmail by remember(current) { mutableStateOf(current.senderEmail) }
    var senderPassword by remember(current) { mutableStateOf(current.senderPassword) }
    var showPassword by remember { mutableStateOf(false) }
    var cloudVisionApiKey by remember(current) { mutableStateOf(current.cloudVisionApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "設定",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Purple80)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(
                            AppSettings(
                                recipientEmail = recipientEmail.trim(),
                                smtpHost = smtpHost.trim(),
                                smtpPort = smtpPort.toIntOrNull() ?: 587,
                                senderEmail = senderEmail.trim(),
                                senderPassword = senderPassword,
                                cloudVisionApiKey = cloudVisionApiKey.trim()
                            )
                        )
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "儲存", tint = Orange80)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel("圖像辨識（Google Cloud Vision）")

            OutlinedTextField(
                value = cloudVisionApiKey,
                onValueChange = { cloudVisionApiKey = it },
                label = { Text("Cloud Vision API Key", color = TextSecondary) },
                placeholder = { Text("AIza...", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Purple80
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColors(),
                singleLine = true
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PurpleContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "💡 如何取得 Cloud Vision API Key",
                        color = Orange80,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "1. 前往 console.cloud.google.com\n2. 建立專案 → 啟用 Cloud Vision API\n3. 憑證 → 建立 API 金鑰 → 複製貼上",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            SectionLabel("收件設定")

            AppTextField(
                value = recipientEmail,
                onValueChange = { recipientEmail = it },
                label = "收件者 Email",
                placeholder = "example@gmail.com",
                keyboardType = KeyboardType.Email
            )

            SectionLabel("寄件設定（SMTP）")

            AppTextField(
                value = senderEmail,
                onValueChange = { senderEmail = it },
                label = "寄件者 Email",
                placeholder = "yourapp@gmail.com",
                keyboardType = KeyboardType.Email
            )

            OutlinedTextField(
                value = senderPassword,
                onValueChange = { senderPassword = it },
                label = { Text("App 密碼", color = TextSecondary) },
                placeholder = { Text("Gmail App Password", color = TextSecondary.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Purple80
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColors(),
                singleLine = true
            )

            AppTextField(
                value = smtpHost,
                onValueChange = { smtpHost = it },
                label = "SMTP 主機",
                placeholder = "smtp.gmail.com"
            )

            AppTextField(
                value = smtpPort,
                onValueChange = { smtpPort = it },
                label = "SMTP Port",
                placeholder = "587",
                keyboardType = KeyboardType.Number
            )

            // Help note
            Card(
                colors = CardDefaults.cardColors(containerColor = PurpleContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "💡 Gmail 使用說明",
                        color = Orange80,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "請至 Google 帳戶 → 安全性 → 兩步驟驗證（需開啟）→ 應用程式密碼，產生 App 密碼後填入上方欄位。",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    onSave(
                        AppSettings(
                            recipientEmail = recipientEmail.trim(),
                            smtpHost = smtpHost.trim(),
                            smtpPort = smtpPort.toIntOrNull() ?: 587,
                            senderEmail = senderEmail.trim(),
                            senderPassword = senderPassword,
                            cloudVisionApiKey = cloudVisionApiKey.trim()
                        )
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text("儲存設定", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Orange60,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = outlinedTextFieldColors(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Purple80,
    unfocusedBorderColor = DividerColor,
    cursorColor = Orange80,
    focusedContainerColor = SurfaceVariant,
    unfocusedContainerColor = SurfaceDark
)
