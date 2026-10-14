package com.dylanpgp.anywhare.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, AMOLED, LIGHT, PURPLE }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val welcomeSeen: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME]
                    ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                welcomeSeen = prefs[Keys.WELCOME_SEEN] ?: false,
            )
        }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun markWelcomeSeen() {
        store.edit { it[Keys.WELCOME_SEEN] = true }
    }
}
