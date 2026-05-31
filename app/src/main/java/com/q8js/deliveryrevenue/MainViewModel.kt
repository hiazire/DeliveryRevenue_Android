package com.q8js.deliveryrevenue

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.q8js.deliveryrevenue.data.*
import com.q8js.deliveryrevenue.util.EmailSender
import com.q8js.deliveryrevenue.util.ExifUtil
import com.q8js.deliveryrevenue.util.OcrProcessor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _imageItems = MutableStateFlow<List<ImageItem>>(emptyList())
    val imageItems: StateFlow<List<ImageItem>> = _imageItems.asStateFlow()

    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _emailState = MutableStateFlow<EmailState>(EmailState.Idle)
    val emailState: StateFlow<EmailState> = _emailState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        val context = getApplication<Application>()
        val newItems = uris.map { uri ->
            val dateTime = ExifUtil.extractDateTime(context, uri)
            ImageItem(uri = uri, dateTime = dateTime)
        }
        _imageItems.update { current -> current + newItems }
        _appState.value = AppState.Idle
        _emailState.value = EmailState.Idle
    }

    fun removeImage(uri: Uri) {
        _imageItems.update { it.filter { item -> item.uri != uri } }
        if (_imageItems.value.isEmpty()) {
            _appState.value = AppState.Idle
        }
    }

    fun clearAll() {
        _imageItems.value = emptyList()
        _appState.value = AppState.Idle
        _emailState.value = EmailState.Idle
    }

    fun processImages() {
        if (_imageItems.value.isEmpty()) return
        viewModelScope.launch {
            _appState.value = AppState.Processing
            try {
                val context = getApplication<Application>()
                val processed = _imageItems.value.map { item ->
                    try {
                        val (amounts, rawText) = OcrProcessor.extractAmounts(context, item.uri, settings.value.cloudVisionApiKey)
                        item.copy(
                            extractedAmounts = amounts,
                            rawText = rawText,
                            isProcessed = true,
                            error = if (amounts.isEmpty()) "未偵測到金額" else null
                        )
                    } catch (e: Exception) {
                        item.copy(
                            isProcessed = true,
                            error = e.message ?: "處理失敗"
                        )
                    }
                }
                _imageItems.value = processed

                // Compute result
                val result = computeResult(processed)
                _appState.value = AppState.Done(result)
            } catch (e: Exception) {
                _appState.value = AppState.Error(e.message ?: "未知錯誤")
            }
        }
    }

    private fun computeResult(items: List<ImageItem>): ProcessingResult {
        var normalFoodPanda = 0.0
        var normalUberEats = 0.0
        var extendedFoodPanda = 0.0
        var extendedUberEats = 0.0

        for (item in items) {
            for (extAmount in item.extractedAmounts) {
                val orderTime = extAmount.orderTime
                    ?: item.dateTime?.toLocalTime()
                    ?: java.time.LocalTime.of(12, 0)

                val isExtended = !orderTime.isBefore(java.time.LocalTime.of(13, 30))

                when (extAmount.platform) {
                    PlatformType.FOODPANDA -> {
                        if (isExtended) extendedFoodPanda += extAmount.amount
                        else normalFoodPanda += extAmount.amount
                    }
                    PlatformType.UBER_EATS -> {
                        if (isExtended) extendedUberEats += extAmount.amount
                        else normalUberEats += extAmount.amount
                    }
                    PlatformType.UNKNOWN -> {}
                }
            }
        }

        val total = items.sumOf { it.extractedAmounts.sumOf { ext -> ext.amount } }
        val dates = items.map { it.date }.toSet()
        val hasConflict = dates.size > 1
        val primaryDate = if (!hasConflict) dates.firstOrNull() else null

        return ProcessingResult(
            totalAmount = total,
            imageItems = items,
            hasDateConflict = hasConflict,
            dates = dates,
            primaryDate = primaryDate,
            normalFoodPanda = normalFoodPanda,
            normalUberEats = normalUberEats,
            extendedFoodPanda = extendedFoodPanda,
            extendedUberEats = extendedUberEats
        )
    }

    fun sendEmail() {
        val state = _appState.value
        if (state !is AppState.Done) return

        viewModelScope.launch {
            _emailState.value = EmailState.Sending
            try {
                val result = state.result
                val cfg = settings.value

                // 取得第一張照片的 LocalDate 物件
                val firstItemDate = result.imageItems.firstOrNull()?.date
                    ?: throw IllegalStateException("無法讀取首張圖片日期")

                // 【修正 2：解決 LocalDate 轉 String】
                // LocalDate.toString() 預設是 YYYY-MM-DD，我們將 "-" 替換為 "/" 以符合你的標題需求
                val dateString = firstItemDate.toString().replace("-", "/")

                if (result.hasDateConflict) {
                    val details = result.imageItems.joinToString("\n") { item ->
                        val formattedDate = item.date?.toString()?.replace("-", "/") ?: "無日期資訊"
                        "• ${item.uri.lastPathSegment ?: "圖片"}: $formattedDate"
                    }

                    EmailSender.sendErrorEmail(cfg, details)
                } else {
                    // 算出所有圖片中，成功抓取到的交易總筆數 (例如：第一張7筆 + 第二張2筆 = 9筆)
                    val totalTransactionCount = result.imageItems.sumOf { it.extractedAmounts.size }

                    EmailSender.sendSuccessEmail(
                        cfg = cfg,
                        totalAmount = result.totalAmount,
                        date = dateString,
                        count = totalTransactionCount,
                        normalFoodPanda = result.normalFoodPanda,
                        normalUberEats = result.normalUberEats,
                        extendedFoodPanda = result.extendedFoodPanda,
                        extendedUberEats = result.extendedUberEats
                    )
                }
                _emailState.value = EmailState.Success
            } catch (e: Exception) {
                _emailState.value = EmailState.Error(e.message ?: "寄送失敗")
            }
        }
    }

    fun saveSettings(s: AppSettings) {
        viewModelScope.launch { settingsRepo.saveSettings(s) }
    }

    fun resetEmailState() {
        _emailState.value = EmailState.Idle
    }
}
