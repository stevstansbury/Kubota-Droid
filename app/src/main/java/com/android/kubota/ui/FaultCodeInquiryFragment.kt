package com.android.kubota.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.FaultCodeInquiryViewModel
import com.android.kubota.viewmodel.UIEquipment

private const val EQUIPMENT_KEY = "equipment"

class FaultCodeInquiryFragment: BaseFragment() {
    private lateinit var viewModel: FaultCodeInquiryViewModel

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fault_code_inquiry, container, false)

        val equipmentId = arguments?.getInt(EQUIPMENT_KEY) ?: 0

        if (equipmentId == 0) {
            fragmentManager?.popBackStack()
            return null
        }

        val factory = InjectorUtils.provideFaultCodeInquiryViewModel(requireContext(), equipmentId)
        viewModel = ViewModelProviders.of(this, factory).get(FaultCodeInquiryViewModel::class.java)

        activity?.title = getString(R.string.fault_code_inquiry)
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

        viewModel.loadModel(equipmentId).observe(this, Observer {equipment->
            equipment?.let {
                updateUI(it)
            }
        })

        viewModel.isLoading.observe(this, Observer {loading ->
            when (loading) {
                true -> flowActivity?.showProgressBar()
                else -> flowActivity?.hideProgressBar()
            }
        })

        viewModel.equipmentImage.observe(this, Observer {
            if (it != 0) modelImageView.setImageResource(it)
        })

        viewModel.equipmentSerial.observe(this, Observer {
            serialNumberTextView.text = if (it != null) {
                getString(R.string.equipment_serial_number_fmt, it)
            } else {
                getString(R.string.equipment_serial_number)
            }
        })

        viewModel.equipmentModel.observe(this, Observer {
            modelTextView.text = it
        })

        viewModel.equipmentNickname.observe(this, Observer {
            equipmentNicknameTextView.text =
                if (it.isNullOrBlank())
                    getString(R.string.no_equipment_name_fmt, viewModel.equipmentModel.value)
                else
                    it
        })

        return view
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
                flowActivity?.addFragmentToBackStack(FaultCodeResultsFragment.createInstance(equipment.id,
                    equipment.model, equipment.serialNumber ?: "", equipment.nickname ?: "", arrayListOf(code)))
            }
        }
    }
}