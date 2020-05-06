package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
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
import com.android.kubota.viewmodel.EngineHoursViewModel

private const val EQUIPMENT_KEY = "equipment"

class EngineHoursFragment: BaseFragment() {

    companion object {
        fun createInstance(equipmentId: Int): EngineHoursFragment {
            val data = Bundle(1)
            data.putInt(EQUIPMENT_KEY, equipmentId)

            return EngineHoursFragment().apply {
                arguments = data
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_engine_hours

    private lateinit var viewModel: EngineHoursViewModel

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHoursEditText: EditText
    private lateinit var saveButton: Button

    private var engineHours = 0

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.let {
            val equipmentId = it.getInt(EQUIPMENT_KEY)
            val factory = InjectorUtils.provideEngineHoursViewModel(requireContext(), equipmentId)
            viewModel = ViewModelProvider(this, factory)
                .get(EngineHoursViewModel::class.java)

            equipmentId > 0
        }
            ?: false
    }

    override fun initUi(view: View) {
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        saveButton = view.findViewById(R.id.saveButton)
        engineHoursEditText = view.findViewById(R.id.engineHoursEditText)

        saveButton.setOnClickListener {
            saveButton.setOnClickListener(null)
            saveButton.hideKeyboard()
            viewModel.updateEngineHours(engineHours)
            parentFragmentManager.popBackStack()
        }
    }

    override fun loadData() {
        viewModel.equipmentEngineHours.observe(viewLifecycleOwner, Observer {
            engineHours = it
            engineHoursEditText.text = Editable.Factory.getInstance().newEditable("${engineHours}")
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
}