package com.android.kubota.ui.equipment.maintenance

import android.annotation.SuppressLint
import android.text.InputFilter
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MutableLiveData
import com.android.kubota.R
import com.android.kubota.databinding.FragmentMaintenanceChecklistBinding
import com.android.kubota.extensions.engineHours
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.android.kubota.ui.popCurrentTabStack
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.android.kubota.viewmodel.equipment.MaintenanceChecklistItemViewModel
import com.android.kubota.viewmodel.equipment.MaintenanceIntervalItem
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit

class MaintenanceChecklistFragment : BaseEquipmentUnitFragment() {

    override val layoutResId = R.layout.fragment_maintenance_checklist

    private lateinit var binding: FragmentMaintenanceChecklistBinding

    val areItemsChecked = MutableLiveData(false)
    val saveEnabled = MutableLiveData(true)

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

    companion object {
        const val INTERVAL_KEY = "INTERVAL_KEY"
        const val INTERVAL_NOTE_KEY = "INTERVAL_NOTE_KEY"
        const val INTERVAL_ENGINE_HOURS_KEY = "INTERVAL_ENGINE_HOURS_KEY"
        fun createInstance(
            equipmentUnit: EquipmentUnit,
            maintenanceIntervalItem: MaintenanceIntervalItem,
            note: String? = null,
            engineHours: Long? = null
        ): MaintenanceChecklistFragment {
            return MaintenanceChecklistFragment().apply {
                arguments = bundleOf(
                    EQUIPMENT_KEY to equipmentUnit,
                    INTERVAL_KEY to maintenanceIntervalItem,
                    INTERVAL_NOTE_KEY to note,
                    INTERVAL_ENGINE_HOURS_KEY to engineHours
                )
            }
        }
    }

    override fun initUi(view: View) {
        binding = FragmentMaintenanceChecklistBinding.bind(view)
        binding.lifecycleOwner = this
        binding.host = this
        binding.viewModel = viewModel

        val interval = arguments?.getParcelable(INTERVAL_KEY) as? MaintenanceIntervalItem
        val intervalNote = arguments?.getString(INTERVAL_NOTE_KEY)
        val intervalEngineHours = arguments?.getLong(INTERVAL_ENGINE_HOURS_KEY)

        if (interval == null) {
            activity?.popCurrentTabStack()
            return
        }

        viewModel.loadActions(interval)

        val displayedInterval = when (interval.intervalType) {
            "Every X Hours" -> {
                requireContext().getString(
                    R.string.maintenance_item_hours,
                    interval.intervalValue
                )
            }
            "Every X Months" -> {
                requireContext().getString(
                    R.string.maintenance_item_months,
                    interval.intervalValue
                )
            }
            "Every X Years" -> {
                requireContext().getString(
                    R.string.maintenance_item_years,
                    interval.intervalValue
                )
            }
            "Annually" -> {
                requireContext().getString(R.string.maintenance_item_anually)
            }
            "Daily Check" -> {
                requireContext().getString(R.string.maintenance_item_daily)
            }
            "As Needed" -> {
                requireContext().getString(R.string.maintenance_item_as_needed)
            }
            else -> {
                "Unknown"
            }
        }

        binding.tvIntervalTitle.text = requireContext().getString(
            R.string.maintenance_checklist_title,
            displayedInterval
        )

        if (intervalNote != null) {
            binding.notes.setText(intervalNote)
        }

        val engineHours = if (intervalEngineHours != 0L) {
            intervalEngineHours.toString()
        } else {
            viewModel.equipmentUnit.value?.engineHours?.toString() ?: ""
        }

        val inputFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { !it.isSurrogate() }
        }
        binding.notes.filters = arrayOf(inputFilter)

        binding.hours.setText(engineHours)
        binding.hours.doOnTextChanged { text, _, _, _ ->
            saveEnabled.value = !text.isNullOrBlank()
        }

        binding.btnToggleAll.setOnClickListener {
            viewModel.toggleAllChecklistItems()

            areItemsChecked.value =
                viewModel.equipmentMaintenanceChecklist.value.orEmpty()
                    .any { it is MaintenanceChecklistItemViewModel && it.checked }
        }

        binding.btnSave.setOnClickListener {
            viewModel.updateMaintenanceSchedule(
                interval,
                binding.hours.text.toString().toDouble().toLong(),
                binding.notes.text.toString(),
            )
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        if (viewModel.equipmentMaintenanceSchedule.value?.isEmpty() == true || viewModel.equipmentMaintenanceHistory.value?.isEmpty() == true) {
            viewModel.loadMaintenanceInfo()
        }

        viewModel.equipmentMaintenanceChecklist.observe(this) { list ->
            areItemsChecked.value =
                list.any { it is MaintenanceChecklistItemViewModel && it.checked }

            binding.rvChecklist.adapter = MaintenanceChecklistAdapter(list) { item ->
                viewModel.toggleChecklistItem(item)

                areItemsChecked.value =
                    viewModel.equipmentMaintenanceChecklist.value.orEmpty()
                        .any { it is MaintenanceChecklistItemViewModel && it.checked }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            when (it) {
                true -> this.showProgressBar()
                else -> this.hideProgressBar()
            }
        }

        this.viewModel.error.observe(viewLifecycleOwner) { error ->
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

        this.viewModel.unitUpdated.observe(viewLifecycleOwner) { didUpdate ->
            notifyUpdateViewModel.unitUpdated.postValue(didUpdate)

            if (didUpdate) {
                activity?.popCurrentTabStack()
                activity?.popCurrentTabStack()
            }
        }
    }
}