package com.android.kubota.ui.equipment.maintenance

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.databinding.FragmentSelectMaintenanceBinding
import com.android.kubota.databinding.ItemSelectMaintenanceScheduleBinding
import com.android.kubota.extensions.engineHours
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.extensions.showKeyboard
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.android.kubota.viewmodel.equipment.MaintenanceIntervalItem
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit

class SelectMaintenanceFragment : BaseEquipmentUnitFragment() {

    override val layoutResId = R.layout.fragment_select_maintenance

    private lateinit var binding: FragmentSelectMaintenanceBinding

    val engineHours = MutableLiveData("")

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): SelectMaintenanceFragment {
            return SelectMaintenanceFragment().apply {
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
        binding = FragmentSelectMaintenanceBinding.bind(view)
        binding.lifecycleOwner = this
        binding.host = this
        binding.viewModel = viewModel

        binding.btnChange.setOnClickListener {
            binding.hoursLayout.isVisible = true
            binding.btnChange.isVisible = false
            binding.btnCancel.isVisible = true

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

            binding.hoursLayout.clearFocus()

            it.hideKeyboard()
        }

        binding.btnSave.setOnClickListener {
            engineHours.value = binding.hours.text.toString()

            binding.hoursLayout.isVisible = false
            binding.btnSave.isVisible = false
            binding.btnChange.isVisible = true
            binding.btnCancel.isVisible = false

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

        viewModel.equipmentUnit.observe(viewLifecycleOwner) { unit ->
            unit?.let {
                engineHours.postValue(it.engineHours.toString())
                this.equipmentUnit = it
            }
        }

        this.viewModel.nextMaintenanceSchedules.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                binding.rvSchedules.adapter =
                    MaintenanceScheduleAdapter(items = items, onItemClick = {
                        viewModel.equipmentUnit.value?.let { unit ->
                            flowActivity?.addFragmentToBackStack(
                                MaintenanceChecklistFragment.createInstance(
                                    unit,
                                    this
                                )
                            )
                        }
                    })
            }
        }

        this.viewModel.unitUpdated.observe(viewLifecycleOwner) { didUpdate ->
            if (didUpdate) {
                viewModel.reload(authDelegate)
                notifyUpdateViewModel.unitUpdated.postValue(didUpdate)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private inner class MaintenanceScheduleAdapter(
        private val items: List<MaintenanceIntervalItem>,
        val onItemClick: (MaintenanceIntervalItem.() -> Unit)?
    ) :
        RecyclerView.Adapter<MaintenanceScheduleAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemSelectMaintenanceScheduleBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(items[position])

        inner class ViewHolder(val itemBinding: ItemSelectMaintenanceScheduleBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: MaintenanceIntervalItem) {
                itemBinding.item = item
                itemBinding.root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
                itemBinding.tvInterval.text = when (item.intervalType) {
                    "Every X Hours" -> {
                        itemView.context.getString(
                            R.string.maintenance_item_hours,
                            item.intervalValue ?: 0
                        )
                    }
                    "Every X Months" -> {
                        itemView.context.getString(
                            R.string.maintenance_item_months,
                            item.intervalValue ?: 0
                        )
                    }
                    "Every X Years" -> {
                        itemView.context.getString(
                            R.string.maintenance_item_years,
                            item.intervalValue ?: 0
                        )
                    }
                    "Annually" -> {
                        itemView.context.getString(R.string.maintenance_item_anually)
                    }
                    "Daily Check" -> {
                        itemView.context.getString(R.string.maintenance_item_daily)
                    }
                    "As Needed" -> {
                        itemView.context.getString(R.string.maintenance_item_as_needed)
                    }
                    else -> {
                        "Unknown"
                    }
                }

                itemBinding.executePendingBindings()
            }
        }
    }

}