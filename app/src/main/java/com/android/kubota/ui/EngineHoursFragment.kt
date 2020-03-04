package com.android.kubota.ui

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

    private lateinit var viewModel: EngineHoursViewModel

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var saveButton: Button

    private var engineHours = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments == null) {
            fragmentManager?.popBackStack()
            return
        }

        arguments?.let {
            val equipmentId = arguments?.getInt(EQUIPMENT_KEY) ?: 0
            val factory = InjectorUtils.provideEngineHoursViewModel(requireContext(), equipmentId)
            viewModel = ViewModelProviders.of(this, factory).get(EngineHoursViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_engine_hours, null)

        activity?.title = getString(R.string.engine_hours)
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        saveButton = view.findViewById(R.id.saveButton)
        val engineHoursEditText = view.findViewById<EditText>(R.id.engineHoursEditText)

        viewModel.equipmentEngineHours.observe(this, Observer {
            engineHours = it
            engineHoursEditText.text = Editable.Factory.getInstance().newEditable("${engineHours}")
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

        engineHoursEditText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if (s == null || s.toString().isBlank() || s.toString().isEmpty()) {
                    saveButton.isEnabled = false
                    return
                }

                val newEngineHours = s.toString().toInt()
                if (newEngineHours != engineHours) {
                    engineHours = newEngineHours
                    saveButton.isEnabled = true
                } else {
                    saveButton.isEnabled = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        saveButton.setOnClickListener {
            saveButton.setOnClickListener(null)
            saveButton.hideKeyboard()
            viewModel.updateEngineHours(engineHours)
            fragmentManager?.popBackStack()
        }

        return view
    }
}