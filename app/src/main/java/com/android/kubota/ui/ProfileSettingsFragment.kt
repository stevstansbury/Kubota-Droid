package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.android.kubota.R
import com.android.kubota.preference.KubotaDropDownPreference
import com.android.kubota.preference.KubotaSwitchPreference

class ProfileSettingsFragment : PreferenceFragmentCompat() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.title = "Settings"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<KubotaDropDownPreference>("units")?.setOnPreferenceChangeListener { preference, newValue ->
            //TODO: use newValue : String
            true
        }
        findPreference<KubotaSwitchPreference>("notifications")?.setOnPreferenceChangeListener { preference, newValue ->
            //TODO: use newValue : Boolean
            true
        }
        findPreference<KubotaSwitchPreference>("messages")?.setOnPreferenceChangeListener { preference, newValue ->
            //TODO: use newValue : Boolean
            true
        }
        findPreference<KubotaSwitchPreference>("alerts")?.setOnPreferenceChangeListener { preference, newValue ->
            //TODO: use newValue : Boolean
            true
        }
    }
}
