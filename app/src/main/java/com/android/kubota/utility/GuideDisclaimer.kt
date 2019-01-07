package com.android.kubota.utility

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class GuideDisclaimer {

    companion object {

        private const val DISCLAIMER_KEY = "DISCLAIMER_ACCEPTED"

        fun isAccepted(context: Context):Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DISCLAIMER_KEY)
        }

        fun accept(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(True).commit()
        }
    }
}