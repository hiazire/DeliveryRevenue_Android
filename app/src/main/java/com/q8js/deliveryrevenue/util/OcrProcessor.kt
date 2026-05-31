package com.q8js.deliveryrevenue.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.q8js.deliveryrevenue.data.ExtractedAmount
import com.q8js.deliveryrevenue.data.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalTime

object OcrProcessor {

    @Suppress("UNUSED_PARAMETER")
    suspend fun extractAmounts(
        context: Context,
        uri: Uri,
        apiKey: String
    ): Pair<List<ExtractedAmount>, String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            val result = Tasks.await(recognizer.process(image))

            // 改用「座標幾何雷達法」精準鎖定金額與平台
            val amounts = parseAmountsFromGeometry(result)

            return@withContext Pair(amounts, result.text)

        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext Pair(emptyList(), "圖片讀取失敗: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(emptyList(), "文字辨識失敗: ${e.message}")
        }
    }

    private fun parseAmountsFromGeometry(result: Text): List<ExtractedAmount> {
        val elements = mutableListOf<Text.Element>()

        // 將所有辨識到的「單字(Element)」攤平收集起來
        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    elements.add(element)
                }
            }
        }

        val amounts = mutableListOf<ExtractedAmount>()
        // 擴大容錯：只要包含 panda 或 eats 就視為平台名稱，避免反光造成些微錯字
        val platformRegex = Regex("(?i)(panda|eats)")

        // 1. 找出所有「外送平台名稱」的單字座標
        val platformElements = elements.filter {
            platformRegex.containsMatchIn(it.text.replace(" ", ""))
        }

        // 2. 針對每一個平台名稱錨點，往它的「右邊」尋找金額
        for (platform in platformElements) {
            val pBox = platform.boundingBox ?: continue
            val platformTextNormalized = platform.text.replace(" ", "").lowercase()
            val platformType = when {
                platformTextNormalized.contains("panda") -> PlatformType.FOODPANDA
                platformTextNormalized.contains("eats") -> PlatformType.UBER_EATS
                else -> PlatformType.UNKNOWN
            }

            // 篩選出在平台名稱「右側」的所有單字
            val rightElements = elements.filter { el ->
                val elBox = el.boundingBox ?: return@filter false
                // 必須在平台名稱的右邊 (使用 centerX 確保不會誤判)
                val isToRight = elBox.centerX() > pBox.centerX()

                // 必須在同一區間內 (放寬垂直容忍度到字體高度的 1.5 倍，抵抗反光造成的座標扭曲)
                val tolerance = pBox.height() * 1.5f
                val isAligned = Math.abs(elBox.centerY() - pBox.centerY()) < tolerance

                val isDifferent = el !== platform

                isToRight && isAligned && isDifferent
            }.sortedBy { it.boundingBox!!.left } // 依照 X 座標由左至右排序

            // 3. 掃描右邊的單字，找出距離最近的第一個「純數字」
            var foundAmount: Double? = null
            for (el in rightElements) {
                val text = el.text.replace(" ", "")

                // 避開時間欄位，改用 continue 跳過
                if (text.contains(":")) {
                    continue
                }

                // 濾除所有非數字字元，檢查是否為有效金額
                val digitsOnly = text.replace(Regex("\\D"), "")
                if (digitsOnly.isNotEmpty()) {
                    val amount = digitsOnly.toDoubleOrNull()
                    if (amount != null) {
                        foundAmount = amount
                        break // 成功找到這筆訂單的金額，結束金額搜尋
                    }
                }
            }

            if (foundAmount != null) {
                // 4. 尋找與此平台錨點 Y 軸最近的「時間」文字
                // 支援格式：HH:mm, 上午/下午 H:mm, H:mm AM/PM, H:mm等
                val timeRegex = Regex("""(?i)(上午|下午|AM|PM)?\s*(\d{1,2})\s*:\s*(\d{2})\s*(AM|PM)?""")
                var bestTime: LocalTime? = null
                var minDistance = Float.MAX_VALUE
                val maxVerticalDistance = pBox.height() * 3.0f // 容許垂直 3 倍字體高度距離

                for (el in elements) {
                    val elBox = el.boundingBox ?: continue
                    val text = el.text.replace(" ", "")
                    val match = timeRegex.find(text)
                    if (match != null) {
                        val distance = Math.abs(elBox.centerY() - pBox.centerY()).toFloat()
                        if (distance < maxVerticalDistance && distance < minDistance) {
                            val period1 = match.groups[1]?.value
                            val hourStr = match.groups[2]!!.value
                            val minStr = match.groups[3]!!.value
                            val period2 = match.groups[4]?.value

                            var hour = hourStr.toIntOrNull() ?: continue
                            val minute = minStr.toIntOrNull() ?: continue

                            val isPM = (period1 != null && (period1.contains("下午") || period1.contains("PM") || period1.contains("pm"))) ||
                                    (period2 != null && (period2.contains("PM") || period2.contains("pm")))

                            val isAM = (period1 != null && (period1.contains("上午") || period1.contains("AM") || period1.contains("am"))) ||
                                    (period2 != null && (period2.contains("AM") || period2.contains("am")))

                            if (isPM && hour < 12) {
                                hour += 12
                            }
                            if (isAM && hour == 12) {
                                hour = 0
                            }

                            try {
                                bestTime = LocalTime.of(hour, minute)
                                minDistance = distance
                            } catch (e: Exception) {
                                // 忽略不合規的時間
                            }
                        }
                    }
                }

                amounts.add(ExtractedAmount(amount = foundAmount, platform = platformType, orderTime = bestTime))
            }
        }
        return amounts
    }

    // 為了相容 ViewModel 舊的呼叫而保留，但實際上已不再使用舊版字串邏輯
    fun parseAmounts(text: String): List<Double> {
        return emptyList()
    }
}