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
            val date = ExifUtil.extractDate(context, uri)
            ImageItem(uri = uri, date = date)
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
        val total = items.sumOf { it.extractedAmounts.sum() }
        val dates = items.map { it.date }.toSet()
        val hasConflict = dates.size > 1
        val primaryDate = if (!hasConflict) dates.firstOrNull() else null
        return ProcessingResult(
            totalAmount = total,
            imageItems = items,
            hasDateConflict = hasConflict,
            dates = dates,
            primaryDate = primaryDate
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

                if (result.hasDateConflict) {
                    val details = result.imageItems.joinToString("\n") { item ->
                        "• ${item.uri.lastPathSegment ?: "圖片"}: ${item.date ?: "無日期資訊"}"
                    }
                    EmailSender.sendErrorEmail(cfg, result.dates, details)
                } else {
                    val date = result.primaryDate
                        ?: throw IllegalStateException("無法讀取圖片日期，請確認圖片包含 EXIF 資訊")
                    EmailSender.sendSuccessEmail(cfg, result.totalAmount, date, result.imageItems.size)
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
