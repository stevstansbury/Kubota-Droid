package com.android.kubota.ui.equipment

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class FaultCodeInquiryFragment: BaseEquipmentUnitFragment() {
    override val layoutResId: Int = R.layout.fragment_fault_code_inquiry

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var faultCodeEditText: EditText
    private var faultCode: Int? = null

    companion object {
        fun createInstance(equipmentId: UUID): FaultCodeInquiryFragment {
            return FaultCodeInquiryFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override fun initUi(view: View) {
        activity?.setTitle(R.string.fault_code_inquiry)
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        submitButton = view.findViewById(R.id.submitButton)
        faultCodeEditText = view.findViewById(R.id.faultCodeEditText)

        faultCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s == null || s.toString().isBlank() || s.toString().isEmpty()) {
                    submitButton.isEnabled = false
                    return
                }

                val newFaultCode = s.toString().toInt()
                if (newFaultCode != faultCode) {
                    faultCode = newFaultCode
                    submitButton.isEnabled = true
                } else {
                    submitButton.isEnabled = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateUISubmitInquiry(){
        submitButton.setOnClickListener(null)
        submitButton.hideKeyboard()
        submitButton.visibility = View.GONE
        faultCodeEditText.background = null
        faultCodeEditText.setTextColor(Color.BLACK)
        faultCodeEditText.isEnabled = false
    }

    override fun onDataLoaded(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        equipmentNicknameTextView.text = display.nickname
        modelImageView.setImageResource(display.imageResId)
        modelTextView.text = display.modelName
        serialNumberTextView.text = display.serialNumber

        submitButton.setOnClickListener {
            faultCode?.let {code->
                updateUISubmitInquiry()
                flowActivity?.addFragmentToBackStack(
                    FaultCodeResultsFragment.createInstance(unit.id, arrayListOf(code))
                )
            }
        }
    }
}