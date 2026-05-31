package com.q8js.deliveryrevenue.data

import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class PlatformType(val displayName: String) {
    FOODPANDA("FoodPanda"),
    UBER_EATS("Uber Eats"),
    UNKNOWN("未知")
}

data class ExtractedAmount(
    val amount: Double,
    val platform: PlatformType,
    val orderTime: LocalTime? = null
)

data class ImageItem(
    val uri: Uri,
    val dateTime: LocalDateTime?,          // from EXIF
    val extractedAmounts: List<ExtractedAmount> = emptyList(),
    val rawText: String = "",
    val isProcessed: Boolean = false,
    val error: String? = null
) {
    val date: LocalDate? get() = dateTime?.toLocalDate()
}

data class ProcessingResult(
    val totalAmount: Double,
    val imageItems: List<ImageItem>,
    val hasDateConflict: Boolean,
    val dates: Set<LocalDate?>,
    val primaryDate: LocalDate?,
    val normalFoodPanda: Double = 0.0,
    val normalUberEats: Double = 0.0,
    val extendedFoodPanda: Double = 0.0,
    val extendedUberEats: Double = 0.0
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
