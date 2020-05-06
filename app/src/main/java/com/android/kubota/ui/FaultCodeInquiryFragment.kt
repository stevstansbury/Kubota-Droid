package com.android.kubota.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.FaultCodeInquiryViewModel
import com.android.kubota.viewmodel.UIEquipment

private const val EQUIPMENT_KEY = "equipment"

class FaultCodeInquiryFragment: BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_fault_code_inquiry

    private lateinit var viewModel: FaultCodeInquiryViewModel

    private var equipmentId = 0

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var faultCodeEditText: EditText
    private var faultCode: Int? = null

    companion object {
        fun createInstance(equipmentId: Int): FaultCodeInquiryFragment {
            val data = Bundle(1)
            data.putInt(EQUIPMENT_KEY, equipmentId)

            val fragment = FaultCodeInquiryFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getInt(EQUIPMENT_KEY)?.let {id ->
            equipmentId = id
            val factory = InjectorUtils.provideFaultCodeInquiryViewModel(requireContext(), equipmentId)
            viewModel = ViewModelProvider(this, factory)
                .get(FaultCodeInquiryViewModel::class.java)

            equipmentId > 0
        } ?: false
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

    override fun loadData() {
        viewModel.loadModel(equipmentId).observe(viewLifecycleOwner, Observer {equipment->
            equipment?.let {
                updateUI(it)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer {loading ->
            when (loading) {
                true -> flowActivity?.showProgressBar()
                else -> flowActivity?.hideProgressBar()
            }
        })

        viewModel.equipmentImage.observe(viewLifecycleOwner, Observer {
            if (it != 0) modelImageView.setImageResource(it)
        })

        viewModel.equipmentSerial.observe(viewLifecycleOwner, Observer {
            serialNumberTextView.text = if (it != null) {
                getString(R.string.equipment_serial_number_fmt, it)
            } else {
                getString(R.string.equipment_serial_number)
            }
        })

        viewModel.equipmentModel.observe(viewLifecycleOwner, Observer {
            modelTextView.text = it
        })

        viewModel.equipmentNickname.observe(viewLifecycleOwner, Observer {
            equipmentNicknameTextView.text =
                if (it.isNullOrBlank())
                    getString(R.string.no_equipment_name_fmt, viewModel.equipmentModel.value)
                else
                    it
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

    private fun updateUI(equipment: UIEquipment) {
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

        submitButton.setOnClickListener {
            faultCode?.let {code->
                updateUISubmitInquiry()
                flowActivity?.addFragmentToBackStack(
                    FaultCodeResultsFragment.createInstance(equipment.id, arrayListOf(code))
                )
            }
        }
    }
}