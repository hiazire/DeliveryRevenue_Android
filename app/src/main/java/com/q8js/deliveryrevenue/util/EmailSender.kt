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
            setText("""
                【一般營業時間】
                FoodPanda：${String.format("%.0f", normalFoodPanda)}
                Uber Eats：${String.format("%.0f", normalUberEats)}

                【延長營業時間】
                FoodPanda：${String.format("%.0f", extendedFoodPanda)}
                Uber Eats：${String.format("%.0f", extendedUberEats)}

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
}