package com.q8js.deliveryrevenue.util

import com.q8js.deliveryrevenue.data.AppSettings
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmailSender {

    suspend fun sendSuccessEmail(
        cfg: AppSettings,
        totalAmount: Double,
        date: String,
        count: Int,
        normalFoodPanda: Double,
        normalUberEats: Double,
        extendedFoodPanda: Double,
        extendedUberEats: Double
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                // 這裡的密碼必須是 16 位數的「應用程式密碼」
                return PasswordAuthentication(cfg.senderEmail, cfg.senderPassword)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(cfg.senderEmail))
            setRecipients(Message.RecipientType.TO, cfg.recipientEmail)

            // 設定標題：q8js_外送未入機加總金額 {yyyy/mm/dd}
            subject = "q8js_外送未入機加總金額 $date"

            // 設定內文
            val normalTotal = normalFoodPanda + normalUberEats
            val extendedTotal = extendedFoodPanda + extendedUberEats
            setText("""
                【一般營業時間】
                FoodPanda：${String.format("%.0f", normalFoodPanda)}
                Uber Eats：${String.format("%.0f", normalUberEats)}
                一般營時 + 雙平台 Total：${String.format("%.0f", normalTotal)}

                【延長營業時間】
                FoodPanda：${String.format("%.0f", extendedFoodPanda)}
                Uber Eats：${String.format("%.0f", extendedUberEats)}
                延長營時 + 雙平台 Total：${String.format("%.0f", extendedTotal)}

                外送平台總金額：${String.format("%.0f", totalAmount)}
            """.trimIndent())
        }

        Transport.send(message)
    }

    suspend fun sendErrorEmail(
        cfg: AppSettings,
        details: String
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(cfg.senderEmail, cfg.senderPassword)
            }
        })

        val today = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(cfg.senderEmail))
            setRecipients(Message.RecipientType.TO, cfg.recipientEmail)
            subject = "q8js_Failed calculation total delivery revenue - $today"
            setText("""
                辨識過程中發現日期不一致，請檢查以下圖片：
                
                $details
            """.trimIndent())
        }

        Transport.send(message)
    }

    suspend fun sendDailyReportEmail(
        cfg: AppSettings,
        date: String,
        normalFP: Double,
        normalUE: Double,
        extendedFP: Double,
        extendedUE: Double,
        reserveCash: Double,
        reserveCashShort: Double,
        drawerCash: Double,
        expenses: List<com.q8js.deliveryrevenue.ui.ExpenseItem>,
        totalDelivery: Double,
        actualDrawerTotal: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.host", cfg.smtpHost.ifEmpty { "smtp.gmail.com" })
                put("mail.smtp.port", cfg.smtpPort.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(cfg.senderEmail, cfg.senderPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(cfg.senderEmail))
                setRecipients(Message.RecipientType.TO, cfg.recipientEmail)
                subject = "q8js_日營業額回報 $date"

                val normalTotal = normalFP + normalUE
                val extendedTotal = extendedFP + extendedUE

                val expensesText = if (expenses.isEmpty()) {
                    "無"
                } else {
                    expenses.mapIndexed { idx, item ->
                        "${idx + 1}. [${item.category}]：${item.amount} 元"
                    }.joinToString("\n")
                }

                setText("""
                    【日營業額回報 - $date】

                    【一般營業時間】
                    FoodPanda：${String.format("%.0f", normalFP)} 元
                    Uber Eats：${String.format("%.0f", normalUE)} 元
                    一般營時 & 雙平台加總：${String.format("%.0f", normalTotal)} 元

                    【延長營業時間】
                    FoodPanda：${String.format("%.0f", extendedFP)} 元
                    Uber Eats：${String.format("%.0f", extendedUE)} 元
                    延長營時 & 雙平台加總：${String.format("%.0f", extendedTotal)} 元

                    【現金與備用金】
                    實有備用金：${String.format("%.0f", reserveCash)} 元
                    備用金缺額：${String.format("%.0f", reserveShortTextVal(reserveCashShort))} 元
                    收銀機現金：${String.format("%.0f", drawerCash)} 元

                    【支出明細】
                    $expensesText

                    外送平台總金額：${String.format("%.0f", totalDelivery)} 元
                    實際收銀加總 (收銀現金 + 備用金 - 支出)：${String.format("%.0f", actualDrawerTotal)} 元
                """.trimIndent())
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun reserveShortTextVal(value: Double): Double {
        return if (value <= 0) 0.0 else value
    }
}