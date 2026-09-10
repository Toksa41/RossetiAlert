package com.rosseti.alert.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore для сохранения настроек:
 * - Код региона (19 — Хакасия)
 * - Названия района, города, улицы (из JSON API)
 * - Настройки уведомлений
 */
class SettingsStore(private val context: Context) {

    companion object {
        private val Context.store by preferencesDataStore(name = "settings")

        private val KEY_REGION_CODE = stringPreferencesKey("region_code")
        private val KEY_RAION = stringPreferencesKey("raion")
        private val KEY_GOROD = stringPreferencesKey("gorod")
        private val KEY_STREET = stringPreferencesKey("street")

        // Настройки уведомлений
        private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        private val KEY_NOTIF_HOUR = stringPreferencesKey("notif_hour") // "08:00"
    }

    val filterFlow: Flow<OutageFilter> = context.store.data.map { prefs ->
        OutageFilter(
            regionCode = prefs[KEY_REGION_CODE] ?: "",
            raion = prefs[KEY_RAION] ?: "",
            gorod = prefs[KEY_GOROD] ?: "",
            street = prefs[KEY_STREET] ?: ""
        )
    }

    /** Поток настроек уведомлений */
    val notifFlow: Flow<NotifSettings> = context.store.data.map { prefs ->
        NotifSettings(
            enabled = prefs[KEY_NOTIF_ENABLED] ?: true,
            hour = prefs[KEY_NOTIF_HOUR] ?: "08:00"
        )
    }

    suspend fun saveFilter(filter: OutageFilter) {
        context.store.edit { prefs ->
            prefs[KEY_REGION_CODE] = filter.regionCode
            prefs[KEY_RAION] = filter.raion
            prefs[KEY_GOROD] = filter.gorod
            prefs[KEY_STREET] = filter.street
        }
    }

    suspend fun saveNotifSettings(settings: NotifSettings) {
        context.store.edit { prefs ->
            prefs[KEY_NOTIF_ENABLED] = settings.enabled
            prefs[KEY_NOTIF_HOUR] = settings.hour
        }
    }
}

data class OutageFilter(
    val regionCode: String,
    val raion: String,
    val gorod: String,
    val street: String
) {
    fun isComplete(): Boolean =
        regionCode.isNotBlank() && raion.isNotBlank() && gorod.isNotBlank() && street.isNotBlank()
}

data class NotifSettings(
    val enabled: Boolean = true,
    val hour: String = "08:00"
)