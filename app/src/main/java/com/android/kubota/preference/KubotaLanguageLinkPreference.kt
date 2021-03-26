package com.android.kubota.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.android.kubota.R

class KubotaLanguageLinkPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.preference_kubota_language_link
    }
}