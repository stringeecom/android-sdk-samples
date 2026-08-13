package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.content.Context
import android.content.SharedPreferences

/** Persists tokens only after a successful Stringee or Firebase registration event. */
class TokenStore(context: Context) {
    private val preferences: SharedPreferences

    init {
        preferences = context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    val token: String
        get() = preferences.getString(PREF_ACCESS_TOKEN, "")!!

    fun saveConnectedToken(token: String?) {
        val normalized = normalize(token)
        if (!normalized.isEmpty()) {
            preferences.edit().putString(PREF_ACCESS_TOKEN, normalized).apply()
        }
    }

    val fcmToken: String
        get() = preferences.getString(PREF_FCM_TOKEN, "")!!

    fun saveFcmToken(token: String?) {
        val normalized = normalize(token)
        if (!normalized.isEmpty()) {
            preferences.edit().putString(PREF_FCM_TOKEN, normalized).apply()
        }
    }

    private fun normalize(value: String?): String {
        return if (value == null) "" else value.trim { it <= ' ' }
    }

    companion object {
        const val PREFS: String = "stringee_call_sample"
        private const val PREF_ACCESS_TOKEN = "last_connected_access_token"
        private const val PREF_FCM_TOKEN = "last_fcm_token"
    }
}
