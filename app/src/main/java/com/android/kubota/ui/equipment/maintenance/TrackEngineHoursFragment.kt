package com.android.kubota.ui.equipment.maintenance

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Button
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.android.kubota.ui.popCurrentTabStack
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.google.android.material.textfield.TextInputEditText
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit

class TrackEngineHoursFragment : BaseEquipmentUnitFragment() {

    override val layoutResId = R.layout.fragment_track_engine_hours

    private lateinit var equipmentHours: TextInputEditText
    private lateinit var nextButton: Button

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): TrackEngineHoursFragment {
            return TrackEngineHoursFragment().apply {
                arguments = getBundle(equipmentUnit)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (this.requireActivity() !is AuthDelegate) {
            throw IllegalStateException("Fragment is not attached to an AuthDelegate Activity.")
        }
    }

    override fun initUi(view: View) {
        equipmentHours = view.findViewById(R.id.hours)
        nextButton = view.findViewById(R.id.nextButton)

        nextButton.setOnClickListener {
            it.hideKeyboard()
            val engineHours = equipmentHours.text.toString().toDoubleOrNull()
            viewModel.updateEngineHours(this.authDelegate, engineHours)
        }

        equipmentHours.doOnTextChanged { text, _, _, _ ->
            nextButton.isEnabled = !text.isNullOrBlank()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> this.showBlockingActivityIndicator()
                else -> this.hideBlockingActivityIndicator()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                when (error) {
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet ->
                        this.showError(getString(R.string.connectivity_error_message))
                    is KubotaServiceError.ServerMaintenance ->
                        this.showError(getString(R.string.server_maintenance))
                    else ->
                        this.showError(getString(R.string.server_error_message))
                }
            }
        })

        this.viewModel.unitUpdated.observe(viewLifecycleOwner) { didUpdate ->
            notifyUpdateViewModel.unitUpdated.postValue(didUpdate)

            if (didUpdate) {
                viewModel.equipmentUnit.value?.let { unit ->
                    activity?.popCurrentTabStack()
                    flowActivity?.addFragmentToBackStack(
                        MaintenanceHistoryFragment.createInstance(
                            unit
                        )
                    )
                }
            }
        }
    }
}