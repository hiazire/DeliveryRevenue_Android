package com.q8js.deliveryrevenue.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object OcrProcessor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private const val VISION_URL = "https://vision.googleapis.com/v1/images:annotate"

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun extractAmounts(
        context: Context,
        uri: Uri,
        apiKey: String
    ): Pair<List<Double>, String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IllegalStateException("請在設定中填入 Cloud Vision API Key")
        val base64Image = encodeImageToBase64(context, uri)
        val rawText = callVisionApi(base64Image, apiKey)
        val amounts = parseAmounts(rawText)
        Pair(amounts, rawText)
    }

    // ── Cloud Vision REST call ────────────────────────────────────────────────

    private fun callVisionApi(base64Image: String, apiKey: String): String {
        val requestBody = VisionRequest(
            requests = listOf(
                AnnotateImageRequest(
                    image = VisionImage(content = base64Image),
                    features = listOf(
                        Feature(type = "DOCUMENT_TEXT_DETECTION", maxResults = 1)
                    ),
                    imageContext = ImageContext(languageHints = listOf("zh-TW", "zh-CN", "en"))
                )
            )
        )

        val json = gson.toJson(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$VISION_URL?key=$apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Cloud Vision API 回應為空")

        if (!response.isSuccessful) {
            val err = runCatching { gson.fromJson(responseBody, VisionErrorResponse::class.java) }.getOrNull()
            throw RuntimeException("Cloud Vision API 錯誤：${err?.error?.message ?: "HTTP ${response.code}"}")
        }

        val visionResponse = gson.fromJson(responseBody, VisionResponse::class.java)
        val fullText = visionResponse.responses?.firstOrNull()?.fullTextAnnotation?.text
        val simpleText = visionResponse.responses?.firstOrNull()?.textAnnotations?.firstOrNull()?.description
        return fullText ?: simpleText ?: ""
    }

    // ── Image encoding ────────────────────────────────────────────────────────

    private fun encodeImageToBase64(context: Context, uri: Uri): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("無法載入圖片")

        val resized = resizeBitmapIfNeeded(bitmap, 2048)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    // ── Amount parsing ────────────────────────────────────────────────────────

    /**
     * Parse monetary amounts from Cloud Vision OCR text.
     *
     * Handles:
     *   Uber Eats:  小計 $123 / 總計 $153
     *   Foodpanda:  訂單金額 NT$250
     *   POS:        合計 1,250 / NT$1,250 / TWD 1,250
     *
     * Strategy: prefer 總計/合計 lines; fallback to all HIGH-confidence currency hits.
     */
    fun parseAmounts(text: String): List<Double> {
        if (text.isBlank()) return emptyList()
        val candidates = mutableListOf<AmountCandidate>()

        for ((idx, line) in text.lines().withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || isNonMonetaryLine(trimmed)) continue

            val currencyHits = extractCurrencyPrefixed(trimmed)
            currencyHits.forEach {
                candidates.add(AmountCandidate(it, ConfidenceLevel.HIGH, idx, trimmed))
            }
            if (currencyHits.isEmpty()) {
                extractKeywordAdjacent(trimmed).forEach {
                    candidates.add(AmountCandidate(it, ConfidenceLevel.MEDIUM, idx, trimmed))
                }
            }
        }

        val filtered = candidates.filter { it.value in 1.0..999_999.0 }
        return deduplicateAmounts(filtered)
    }

    private fun deduplicateAmounts(candidates: List<AmountCandidate>): List<Double> {
        if (candidates.isEmpty()) return emptyList()
        val totalRe = Regex("""總計|合計|Total|TOTAL|grand.?total""", RegexOption.IGNORE_CASE)
        val totalCandidates = candidates.filter { totalRe.containsMatchIn(it.sourceLine) }
        return if (totalCandidates.isNotEmpty()) {
            totalCandidates.map { it.value }.distinct()
        } else {
            candidates.filter { it.confidence == ConfidenceLevel.HIGH }
                .map { it.value }.distinct()
                .ifEmpty { candidates.map { it.value }.distinct() }
        }
    }

    private fun isNonMonetaryLine(line: String): Boolean {
        if (line.matches(Regex(".*\\b\\d{8,}\\b.*")) && !line.contains(".")) return true
        if (line.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?(\\s*(AM|PM|上午|下午))?"))) return true
        if (line.matches(Regex("\\+?\\d[\\d\\s\\-]{8,15}"))) return true
        if (line.matches(Regex("\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}.*"))) return true
        return false
    }

    private val currencyPrefixRe = Regex("""(?:NT\s*\$?|TWD\s*|\$)(\d{1,6}(?:,\d{3})*(?:\.\d{1,2})?)""")
    private fun extractCurrencyPrefixed(line: String): List<Double> =
        currencyPrefixRe.findAll(line).mapNotNull { it.groupValues[1].replace(",","").toDoubleOrNull() }.toList()

    private val keywordRe = Regex(
        """(?:訂單金額|交易金額|總金額|小計|合計|總計|金額|實收|應收|營業額|收入|subtotal|total)[^\d]*(\d{1,6}(?:,\d{3})*(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private fun extractKeywordAdjacent(line: String): List<Double> =
        keywordRe.findAll(line).mapNotNull { it.groupValues[1].replace(",","").toDoubleOrNull() }.toList()

    private enum class ConfidenceLevel { HIGH, MEDIUM }
    private data class AmountCandidate(val value: Double, val confidence: ConfidenceLevel, val lineIndex: Int, val sourceLine: String)

    // ── Gson data classes ─────────────────────────────────────────────────────

    data class VisionRequest(val requests: List<AnnotateImageRequest>)
    data class AnnotateImageRequest(val image: VisionImage, val features: List<Feature>, val imageContext: ImageContext? = null)
    data class VisionImage(val content: String)
    data class Feature(val type: String, val maxResults: Int = 1)
    data class ImageContext(@SerializedName("languageHints") val languageHints: List<String>)
    data class VisionResponse(val responses: List<AnnotateImageResponse>?)
    data class AnnotateImageResponse(val textAnnotations: List<TextAnnotation>?, val fullTextAnnotation: FullTextAnnotation?)
    data class TextAnnotation(val description: String?)
    data class FullTextAnnotation(val text: String?)
    data class VisionErrorResponse(val error: VisionError?)
    data class VisionError(val message: String?, val code: Int?)
}
