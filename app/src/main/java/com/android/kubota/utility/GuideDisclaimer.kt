package com.android.kubota.utility

import android.content.Context
import android.preference.PreferenceManager

class GuideDisclaimer {

    companion object {

        private const val DISCLAIMER_KEY = "DISCLAIMER_ACCEPTED"

        fun getIsDisclaimerAccepted(context: Context):Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DISCLAIMER_KEY, false)
        }

        fun setIsDisclaimerAccepted(context: Context, value: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(DISCLAIMER_KEY, value).apply()
        }
    }
}