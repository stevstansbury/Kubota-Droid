package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.kubota.R
import com.android.kubota.preference.KubotaDropDownPreference
import com.android.kubota.preference.KubotaSwitchPreference
import com.android.kubota.viewmodel.SettingsViewModel
import com.google.android.material.snackbar.Snackbar
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.internal.getDefaultMeasurementUnit

class ProfileSettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var measurementUnitPreference: KubotaDropDownPreference
    private lateinit var messagesPreference: KubotaSwitchPreference
    private lateinit var alertsPreference: KubotaSwitchPreference

    private val preferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
        stopListeningForChanges()

        val currentSettings = viewModel.settings.value!!
        when (preference) {
            is KubotaDropDownPreference -> {
                val newMeasurementUnit = if (newValue == "US") {
                    MeasurementUnitType.US
                } else {
                    MeasurementUnitType.METRIC
                }

                viewModel.updateSettings(
                    currentSettings.copy(measurementUnit = newMeasurementUnit)
                )
            }
            is KubotaSwitchPreference -> {
                if (preference == alertsPreference){
                    val masterChecked = newValue as Boolean && messagesPreference.isChecked
                    viewModel.updateSettings(
                        currentSettings.copy(
                            subscribedToNotifications = masterChecked,
                            subscribedToAlerts = newValue
                        )
                    )
                } else {
                    val masterChecked = newValue as Boolean && alertsPreference.isChecked
                    viewModel.updateSettings(
                        currentSettings.copy(
                            subscribedToNotifications = masterChecked,
                            subscribedToMessages = newValue
                        )
                    )
                }
            }
        }

        true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.title = getString(R.string.profile_settings)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        measurementUnitPreference = findPreference("units")!!
        messagesPreference = findPreference("messages")!!
        alertsPreference = findPreference("alerts")!!

        viewModel.loading.observe(this, Observer {
            when (it) {
                true -> showLoadingDialog()
                false -> dismissLoadingDialog()
            }
        })

        viewModel.settings.observe(this, Observer {
            stopListeningForChanges()

            val measurementUnit = it.measurementUnit ?: requireContext().getDefaultMeasurementUnit()
            when (measurementUnit) {
                MeasurementUnitType.US -> measurementUnitPreference.value = "US"
                MeasurementUnitType.METRIC -> measurementUnitPreference.value = "Metric"
            }

            when (it.subscribedToMessages) {
                false -> messagesPreference.isChecked = false
                else -> messagesPreference.isChecked = true
            }

            when (it.subscribedToAlerts) {
                false -> alertsPreference.isChecked = false
                else -> alertsPreference.isChecked = true
            }

            listenForChanges()
        })

        viewModel.error.observe(this, Observer {error ->
            error?.let {
                val errorStringResId = when (it) {
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet -> R.string.connectivity_error_message
                    is KubotaServiceError.ServerMaintenance -> R.string.server_maintenance
                    else -> R.string.server_error_message
                }

                Snackbar.make(requireView(), errorStringResId, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoadingDialog() {
        if (childFragmentManager.findFragmentByTag(LoadingDialogFragment.TAG) == null) {
            LoadingDialogFragment().show(childFragmentManager, LoadingDialogFragment.TAG)
        }
    }

    private fun dismissLoadingDialog() {
        (childFragmentManager.findFragmentByTag(LoadingDialogFragment.TAG) as? LoadingDialogFragment)?.dismiss()
    }

    private fun listenForChanges() {
        measurementUnitPreference.onPreferenceChangeListener = preferenceChangeListener
        messagesPreference.onPreferenceChangeListener = preferenceChangeListener
        alertsPreference.onPreferenceChangeListener = preferenceChangeListener
    }

    private fun stopListeningForChanges() {
        measurementUnitPreference.onPreferenceChangeListener = null
        messagesPreference.onPreferenceChangeListener = null
        alertsPreference.onPreferenceChangeListener = null
    }
}

class LoadingDialogFragment: DialogFragment() {

    companion object {
        const val TAG = "LoadingDialogFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.AccountSetUpTheme_LoadingDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_loading, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing as we are either loading the UI or making changes to the settings.
                }
            })
    }
}
