package com.q8js.deliveryrevenue.util

import com.q8js.deliveryrevenue.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    suspend fun sendSuccessEmail(
        settings: AppSettings,
        totalAmount: Double,
        date: LocalDate,
        imageCount: Int
    ) = withContext(Dispatchers.IO) {
        val subject = "q8js_外送平台營業額加總 ${date.format(dateFormatter)}"
        val body = buildSuccessBody(totalAmount, date, imageCount)
        sendEmail(settings, subject, body)
    }

    suspend fun sendErrorEmail(
        settings: AppSettings,
        dates: Set<LocalDate?>,
        conflictDetails: String
    ) = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val nowStr = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        val subject = "q8js_Failed calculation total delivery revenue - $nowStr"
        val body = buildErrorBody(dates, conflictDetails)
        sendEmail(settings, subject, body)
    }

    private fun buildSuccessBody(total: Double, date: LocalDate, imageCount: Int): String {
        return """
外送平台營業額加總報告
========================================
日期：${date.format(dateFormatter)}
圖片數量：$imageCount 張
========================================
總交易金額：NT$ ${"%.2f".format(total)}
========================================

此郵件由「外送營業額」APP 自動產生。
        """.trimIndent()
    }

    private fun buildErrorBody(dates: Set<LocalDate?>, details: String): String {
        val datesStr = dates.joinToString(", ") {
            it?.format(dateFormatter) ?: "未知日期"
        }
        return """
⚠️ 圖片日期不一致，無法加總

偵測到的日期：$datesStr

詳細資訊：
$details

請確認所有圖片屬於同一天，再重新上傳計算。

此郵件由「外送營業額」APP 自動產生。
        """.trimIndent()
    }

    private fun sendEmail(settings: AppSettings, subject: String, body: String) {
        if (settings.senderEmail.isBlank() || settings.senderPassword.isBlank()) {
            throw IllegalStateException("請先在設定中填入寄件者帳號和密碼")
        }
        if (settings.recipientEmail.isBlank()) {
            throw IllegalStateException("請先在設定中填入收件者 Email")
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", settings.smtpHost)
            put("mail.smtp.port", settings.smtpPort.toString())
            put("mail.smtp.ssl.trust", settings.smtpHost)
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(settings.senderEmail, settings.senderPassword)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(settings.senderEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.recipientEmail))
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
        }

        Transport.send(message)
    }
}
