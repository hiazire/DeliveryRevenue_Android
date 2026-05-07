package com.q8js.deliveryrevenue.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val RECIPIENT_EMAIL = stringPreferencesKey("recipient_email")
        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SENDER_EMAIL = stringPreferencesKey("sender_email")
        val SENDER_PASSWORD = stringPreferencesKey("sender_password")
        val CLOUD_VISION_API_KEY = stringPreferencesKey("cloud_vision_api_key")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            recipientEmail = prefs[RECIPIENT_EMAIL] ?: "",
            smtpHost = prefs[SMTP_HOST] ?: "smtp.gmail.com",
            smtpPort = prefs[SMTP_PORT] ?: 587,
            senderEmail = prefs[SENDER_EMAIL] ?: "",
            senderPassword = prefs[SENDER_PASSWORD] ?: "",
            cloudVisionApiKey = prefs[CLOUD_VISION_API_KEY] ?: ""
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[RECIPIENT_EMAIL] = settings.recipientEmail
            prefs[SMTP_HOST] = settings.smtpHost
            prefs[SMTP_PORT] = settings.smtpPort
            prefs[SENDER_EMAIL] = settings.senderEmail
            prefs[SENDER_PASSWORD] = settings.senderPassword
            prefs[CLOUD_VISION_API_KEY] = settings.cloudVisionApiKey
        }
    }
}
