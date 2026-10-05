package com.screensolver.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.screensolver.ai.data.model.AiConfig

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = createSecurePrefs(context)

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_FILE = "ai_solver_secure_prefs"
        private const val KEY_BASE_URL = "key_base_url"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_AUTO_LOOP = "key_auto_loop"
        private const val KEY_AUTO_INTERVAL = "key_auto_interval"

        const val DEFAULT_BASE_URL = "https://api.9router.com/v1"
        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Không thể khởi tạo EncryptedSharedPreferences, fallback SharedPreferences thông thường", e)
            context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getAiConfig(): AiConfig {
        return AiConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            isAutoLoopEnabled = prefs.getBoolean(KEY_AUTO_LOOP, false),
            autoIntervalSeconds = prefs.getFloat(KEY_AUTO_INTERVAL, 2.0f)
        )
    }

    fun saveAiConfig(config: AiConfig) {
        prefs.edit()
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putBoolean(KEY_AUTO_LOOP, config.isAutoLoopEnabled)
            .putFloat(KEY_AUTO_INTERVAL, config.autoIntervalSeconds)
            .apply()
    }

    fun setAutoLoopEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOOP, enabled).apply()
    }

    fun isAutoLoopEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOOP, false)
    }
}
