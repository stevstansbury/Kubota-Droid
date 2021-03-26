package com.android.kubota.app

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class AppPreferences(context: Context) {
    companion object {
        private const val KEY_CRP_SLT = "CRP_SLT"
        private const val KEY_FIRST_TIME_USED = "FIRST_TIME_USED"
        private const val KEY_GUIDES_DISCLAIMER_ACCEPTED = "GUIDES_DISCLAIMER_ACCEPTED"
        private const val KEY_FIRST_TIME_SCAN = "FIRST_TIME_SCAN"
        private const val LANGUAGE_TAG = "LANGUAGE_TAG"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences("KubotaPreferences", Context.MODE_PRIVATE)

    var firstTimeUsed: Boolean
        get() {
            return this.preferences.getBoolean(KEY_FIRST_TIME_USED, true)
        }
        set(value) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_FIRST_TIME_USED, value)
            editor.apply()
        }

    var guidesDisclaimerAccepted: Boolean
        get() {
            return this.preferences.getBoolean(KEY_GUIDES_DISCLAIMER_ACCEPTED, false)
        }
        set(value) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_GUIDES_DISCLAIMER_ACCEPTED, value)
            editor.apply()
        }

    var firstTimeScan: Boolean
        get() {
            return this.preferences.getBoolean(KEY_FIRST_TIME_SCAN, true)
        }
        set(value: Boolean) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_FIRST_TIME_SCAN, value)
            editor.apply()
        }

    val languageTag: String?
        get() {
            return this.preferences.getString(LANGUAGE_TAG, null)
        }

    fun setLanguageTag(value: String) {
        val editor = this.preferences.edit()
        editor.putString(LANGUAGE_TAG, value)
        editor.apply()
    }

    fun crpSlt(key: String): String {
        return this.preferences.getString(KEY_CRP_SLT + key, "") ?: ""
    }

    fun setCrpSlt(key: String, value: String) {
        val editor = this.preferences.edit()
        editor.putString(KEY_CRP_SLT + key, value)
        editor.apply()
    }

    fun setCryptoDataFor(key: String, encryptedData: String, encryptedIv: String) {
        val editor = this.preferences.edit()
        editor.putString(key, encryptedData)
        editor.putString("${key}_iv", encryptedIv)
        editor.apply()
    }

    fun getCryptoDataFor(key: String): Pair<String, String> {
        val encryptedString = this.preferences.getString(key, null) ?: ""
        val ivString = this.preferences.getString("${key}_iv", null) ?: ""

        return Pair(encryptedString, ivString)
    }

    fun clearKey(key: String) {
        this.preferences.edit().remove(key).apply()
    }

}
