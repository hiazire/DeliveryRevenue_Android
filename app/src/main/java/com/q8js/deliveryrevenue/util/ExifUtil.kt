package com.q8js.deliveryrevenue.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ExifUtil {

    // EXIF date formats
    private val exifFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    private val exifDateOnly = DateTimeFormatter.ofPattern("yyyy:MM:dd")

    fun extractDateTime(context: Context, uri: Uri): LocalDateTime? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)

                dateStr?.let { parseExifDateTime(it) }
            }
        } catch (e: Exception) {
            // If no EXIF, try to get from file metadata
            getDateTimeFromUri(context, uri)
        }
    }

    private fun parseExifDateTime(dateStr: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateStr, exifFormatter)
        } catch (e: DateTimeParseException) {
            try {
                // If only date is available, default time to 00:00:00
                val date = LocalDate.parse(dateStr.take(10), exifDateOnly)
                date.atStartOfDay()
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }

    private fun getDateTimeFromUri(context: Context, uri: Uri): LocalDateTime? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(android.provider.MediaStore.Images.Media.DATE_TAKEN),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dateTaken = it.getLong(0)
                    if (dateTaken > 0) {
                        val instant = java.time.Instant.ofEpochMilli(dateTaken)
                        LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
