package com.android.kubota.ui.equipment

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.engineHours
import com.android.kubota.extensions.hasTelematics
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.kubota.service.api.KubotaServiceError
import java.util.UUID
import kotlin.collections.ArrayList


class EditEquipmentFragment: BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_edit_equipment

    private lateinit var machineCard: MachineCardView
    private lateinit var equipmentNickname: EditText
    private lateinit var equipmentHoursLayout: View
    private lateinit var equipmentHours: EditText
    private lateinit var saveButton: Button

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity() )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (this.requireActivity() !is AuthDelegate) {
            throw IllegalStateException("Fragment is not attached to an AuthDelegate Activity.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY)

        arguments?.getString(EQUIPMENT_KEY)?.let {
            equipmentUnitId = UUID.fromString(it)
        }
    }

    override fun initUi(view: View) {
        machineCard = view.findViewById(R.id.machineCard)
        equipmentNickname = view.findViewById(R.id.nickname)
        equipmentHoursLayout = view.findViewById(R.id.hoursLayout)
        equipmentHours = view.findViewById(R.id.hours)
        saveButton = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            it.hideKeyboard()
            val nickName = equipmentNickname.text.toString()
            val engineHours = equipmentHours.text.toString().toDoubleOrNull()
            viewModel.updateEquipmentUnit(this.authDelegate, nickName, engineHours)
        }
    }

    override fun loadData() {
        super.loadData()

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
                    else ->
                        this.showError(getString(R.string.server_error_message))
                }
            }
        })

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let {
                machineCard.setModel(it)

                equipmentHoursLayout.visibility = if (it.hasTelematics) View.GONE else View.VISIBLE
                equipmentNickname.setText(it.nickName)
                equipmentHours.setText(String.format("%.2f", it.engineHours))
            }
        })

        this.viewModel.unitUpdated.observe(viewLifecycleOwner, Observer { didUpdate ->
            notifyUpdateViewModel.unitUpdated.postValue(didUpdate)

            if (didUpdate) {
                parentFragmentManager.popBackStack()
            }
        })
        this.viewModel.updateData(this.authDelegate)
    }

    companion object {
        private const val EQUIPMENT_KEY = "EQUIPMENT_KEY"
        private const val FAULT_CODES_KEY = "FAULT_CODES_KEY"

        fun createInstance(equipmentId: UUID) : EditEquipmentFragment {
            return EditEquipmentFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }

        fun getBundle(equipmentId: UUID, faultCodes: ArrayList<Int>? = null): Bundle {
            val bundleSize = 1 + (faultCodes?.size ?: 0)
            val data = Bundle(bundleSize)
            data.putString(EQUIPMENT_KEY, equipmentId.toString())

            faultCodes?.let {
                data.putIntegerArrayList(FAULT_CODES_KEY, it)
            }
            return data
        }
    }
}