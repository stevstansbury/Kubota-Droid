package com.android.kubota.ui.equipment.maintenance

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MutableLiveData
import com.android.kubota.R
import com.android.kubota.databinding.FragmentMaintenanceHistoryBinding
import com.android.kubota.extensions.engineHours
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.extensions.showKeyboard
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit

class MaintenanceHistoryFragment : BaseEquipmentUnitFragment() {

    override val layoutResId = R.layout.fragment_maintenance_history

    private lateinit var binding: FragmentMaintenanceHistoryBinding

    val engineHours = MutableLiveData("")

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): MaintenanceHistoryFragment {
            return MaintenanceHistoryFragment().apply {
                arguments = getBundle(equipmentUnit)
            }
        }
    }

    override fun initUi(view: View) {
        binding = FragmentMaintenanceHistoryBinding.bind(view)
        binding.lifecycleOwner = this
        binding.host = this
        binding.viewModel = viewModel

        binding.btnChange.setOnClickListener {
            binding.hoursLayout.isVisible = true
            binding.btnChange.isVisible = false
            binding.btnCancel.isVisible = true
            binding.btnTrackMaintenance.isVisible = false

            binding.btnSave.isVisible = true
            binding.btnSave.isEnabled = false

            binding.hours.requestFocus()
            binding.hours.showKeyboard()
        }

        binding.btnCancel.setOnClickListener {
            binding.hoursLayout.isVisible = false
            binding.btnSave.isVisible = false
            binding.btnChange.isVisible = true
            binding.btnCancel.isVisible = false
            binding.btnTrackMaintenance.isVisible = true

            binding.hoursLayout.clearFocus()

            it.hideKeyboard()
        }

        binding.btnSave.setOnClickListener {
            engineHours.value = binding.hours.text.toString()

            binding.hoursLayout.isVisible = false
            binding.btnSave.isVisible = false
            binding.btnChange.isVisible = true
            binding.btnCancel.isVisible = false
            binding.btnTrackMaintenance.isVisible = true

            it.hideKeyboard()

            viewModel.updateEngineHours(
                this.authDelegate,
                binding.hours.text.toString().toDoubleOrNull()
            )
        }

        binding.hours.doOnTextChanged { charSequence: CharSequence?, _: Int, _: Int, _: Int ->
            binding.btnSave.isEnabled = !charSequence.isNullOrEmpty()
        }

        binding.hoursLayout.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.hoursLayout.isVisible = false
                binding.btnChange.isVisible = true
                binding.btnCancel.isVisible = false

                binding.hoursLayout.hideKeyboard()
            }
        }

        binding.btnTrackMaintenance.setOnClickListener {
            viewModel.equipmentUnit.value?.let { unit ->
                flowActivity?.addFragmentToBackStack(SelectMaintenanceFragment.createInstance(unit))
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        if (viewModel.equipmentMaintenanceSchedule.value?.isEmpty() == true || viewModel.equipmentMaintenanceHistory.value?.isEmpty() == true) {
            viewModel.loadMaintenanceInfo()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            when (it) {
                true -> this.showProgressBar()
                else -> this.hideProgressBar()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
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
        }

        this.viewModel.equipmentUnit.observe(this) { unit ->
            unit?.let {
                engineHours.postValue(it.engineHours.toString())
                this.equipmentUnit = it
            }
        }

        viewModel.equipmentMaintenanceHistory.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                binding.recycler.isVisible = true
                binding.recycler.adapter =
                    MaintenanceHistoryAdapter(items = list, onItemClicked = { historyEntry ->
                        viewModel.equipmentUnit.value?.let { unit ->
                            flowActivity?.addFragmentToBackStack(
                                MaintenanceChecklistFragment.createInstance(
                                    equipmentUnit = unit,
                                    maintenanceIntervalItem = viewModel.getMaintenanceItem(
                                        historyEntry
                                    ),
                                    note = historyEntry.notes,
                                    engineHours = historyEntry.completedEngineHours
                                )
                            )
                        }
                    })
                binding.containerEmpty.isVisible = false
            } else {
                binding.recycler.isVisible = false
                binding.containerEmpty.isVisible = true
            }
        }

        this.viewModel.unitUpdated.observe(viewLifecycleOwner) { didUpdate ->
            if (didUpdate) {
                viewModel.reload(authDelegate)
                notifyUpdateViewModel.unitUpdated.postValue(didUpdate)
            }
        }
    }
}