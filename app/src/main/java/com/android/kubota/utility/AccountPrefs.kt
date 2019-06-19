package com.android.kubota.utility

import android.content.Context

private var disclaimerPrefs: DisclaimerPrefs? = null
object AccountPrefs {

    @Synchronized
    private fun getPrefs(context: Context): DisclaimerPrefs {
        if (disclaimerPrefs == null) {
            disclaimerPrefs = DisclaimerPrefs(context)
        }

        return disclaimerPrefs as DisclaimerPrefs
    }

    fun getIsDisclaimerAccepted(context: Context): Boolean = getPrefs(context = context).getIsDisclaimerAccepted()

    fun setDisclaimerAccepted(context: Context) = getPrefs(context = context).setIsDisclaimerAccepted(boolean = true)

    fun clearDisclaimerAccepted(context: Context) = getPrefs(context = context).setIsDisclaimerAccepted(boolean = false)

}

private const val PREFERENCES = "MyPrefs"
private const val DISCLAIMER_KEY = "DISCLAIMER_ACCEPTED"

private class DisclaimerPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun setIsDisclaimerAccepted(boolean: Boolean) = prefs.edit().putBoolean(DISCLAIMER_KEY, boolean).apply()

    fun getIsDisclaimerAccepted():Boolean = prefs.getBoolean(DISCLAIMER_KEY, false)

}

