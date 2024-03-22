package com.stringee.kotlin_onetoonecallsample.common

import android.content.Context
import android.content.SharedPreferences


class PrefUtils private constructor(applicationContext: Context) {
    init {
        preferences = applicationContext.getSharedPreferences(Constant.PREF_BASE, Context.MODE_PRIVATE)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return preferences.getInt(key, defValue)
    }

    fun getString(key: String?, defValue: String?): String? {
        return preferences.getString(key, defValue)
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return preferences.getBoolean(key, defValue)
    }

    fun getLong(key: String?, defValue: Long): Long {
        return preferences.getLong(key, defValue)
    }

    fun putInt(key: String?, defValue: Int) {
        val editor = preferences.edit()
        editor.putInt(key, defValue)
        editor.apply()
    }

    fun putString(key: String?, defValue: String?) {
        val editor = preferences.edit()
        editor.putString(key, defValue)
        editor.apply()
    }

    fun putBoolean(key: String?, defValue: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(key, defValue)
        editor.apply()
    }

    fun putLong(key: String?, defValue: Long) {
        val editor = preferences.edit()
        editor.putLong(key, defValue)
        editor.apply()
    }

    fun clearData() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    companion object {
        private lateinit var preferences: SharedPreferences

        @Volatile
        private var instance: PrefUtils? = null
        @Synchronized
        fun getInstance(context: Context): PrefUtils {
            return instance ?: synchronized(this) {
                instance
                    ?: PrefUtils(context.applicationContext).also {
                        instance = it
                    }
            }
        }
    }
}

