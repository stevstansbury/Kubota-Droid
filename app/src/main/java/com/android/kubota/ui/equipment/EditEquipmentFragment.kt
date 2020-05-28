package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.ui.AccountController
import com.android.kubota.ui.FlowActivity
import com.android.kubota.viewmodel.equipment.EquipmentUnitViewModel
import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.api.KubotaServiceError
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.collections.ArrayList


class EditEquipmentFragment: DialogFragment() {

    private val flowActivity: FlowActivity by lazy {
        requireActivity() as FlowActivity
    }

    private var equipmentUnitId: UUID? = null
    private var faultCodes: List<Int>? = null

    private lateinit var machineCard: MachineCardView
    private lateinit var equipmentNickname: EditText
    private lateinit var equipmentHoursLayout: View
    private lateinit var equipmentHours: EditText
    private lateinit var saveButton: Button

    private val viewModel: EquipmentUnitViewModel by lazy {
        EquipmentUnitViewModel.instance(
            owner = this,
            equipmentUnitId = this.equipmentUnitId!!,
            faultCodes = this.faultCodes,
            signInHandler = WeakReference { this.signInAsync() }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY)

        arguments?.getString(EQUIPMENT_KEY)?.let {
            equipmentUnitId = UUID.fromString(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_equipment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        machineCard = view.findViewById(R.id.machineCard)
        equipmentNickname = view.findViewById(R.id.nickname)
        equipmentHoursLayout = view.findViewById(R.id.hoursLayout)
        equipmentHours = view.findViewById(R.id.hours)
        saveButton = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            viewModel.updateEquipmentUnit()
        }
        loadData()
    }

    private fun loadData() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> flowActivity.showProgressBar()
                else -> flowActivity.hideProgressBar()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                when (error) {
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet ->
                        flowActivity.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
                    else ->
                        this.flowActivity.makeSnackbar()?.setText(R.string.server_error_message)?.show()
                }
            }
        })

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let {
                machineCard.setModel(it)

                equipmentHoursLayout.visibility = if (it.hasTelematics) View.GONE else View.VISIBLE
                equipmentHours.setText(it.engineHours?.toInt().toString())
            }
        })
    }

    private fun signInAsync(): Promise<Unit> {
        return (this.requireActivity() as? AccountController)?.signInAsync() ?: Promise.value(Unit)
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