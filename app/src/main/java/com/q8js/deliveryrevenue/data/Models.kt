package com.q8js.deliveryrevenue.data

import android.net.Uri
import java.time.LocalDate

data class ImageItem(
    val uri: Uri,
    val date: LocalDate?,          // from EXIF
    val extractedAmounts: List<Double> = emptyList(),
    val rawText: String = "",
    val isProcessed: Boolean = false,
    val error: String? = null
)

data class ProcessingResult(
    val totalAmount: Double,
    val imageItems: List<ImageItem>,
    val hasDateConflict: Boolean,
    val dates: Set<LocalDate?>,
    val primaryDate: LocalDate?
)

data class AppSettings(
    val recipientEmail: String = "",
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 587,
    val senderEmail: String = "",
    val senderPassword: String = "",
    val cloudVisionApiKey: String = ""
)

sealed class AppState {
    object Idle : AppState()
    object Processing : AppState()
    data class Done(val result: ProcessingResult) : AppState()
    data class Error(val message: String) : AppState()
}

sealed class EmailState {
    object Idle : EmailState()
    object Sending : EmailState()
    object Success : EmailState()
    data class Error(val message: String) : EmailState()
}
