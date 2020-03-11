package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R

import com.android.kubota.viewmodel.UIEquipment

private const val EQUIPMENT_KEY = "equipment"

class FaultCodeInquiryFragment: BaseFragment() {
    private lateinit var equipment: UIEquipment
    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var submitButton: Button

    companion object {
        fun createInstance(equipment: UIEquipment): FaultCodeInquiryFragment {
            val data = Bundle(1)
            data.putParcelable(EQUIPMENT_KEY, equipment)

            val fragment = FaultCodeInquiryFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fault_code_inquiry, container, false)

        val equipment = arguments?.getParcelable(EQUIPMENT_KEY) as UIEquipment?

        if (equipment == null) {
            fragmentManager?.popBackStack()
            return null
        }

        activity?.title = getString(R.string.fault_code_inquiry)
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        submitButton = view.findViewById(R.id.submitButton)

        updateUI(equipment)

        return view
    }

    private fun updateUI(equipment: UIEquipment) {
        this.equipment = equipment
        val equipmentNickname =
            if (equipment.nickname.isNullOrBlank())
                getString(R.string.no_equipment_name_fmt, equipment.model)
            else
                equipment.nickname

        equipmentNicknameTextView.text = equipmentNickname

        if (equipment.imageResId != 0) {
            modelImageView.setImageResource(equipment.imageResId)
        }

        modelTextView.text = equipment.model
        serialNumberTextView.text = if (equipment.serialNumber != null) {
            getString(R.string.equipment_serial_number_fmt, equipment.serialNumber)
        } else {
            getString(R.string.equipment_serial_number)
        }
    }
}