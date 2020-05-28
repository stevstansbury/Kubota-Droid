package com.android.kubota.ui.equipment

import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import androidx.lifecycle.Observer
import com.android.kubota.extensions.displayInfo
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class EngineHoursFragment: BaseEquipmentUnitFragment() {

    companion object {
        fun createInstance(equipmentId: UUID): EngineHoursFragment {
            return EngineHoursFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_engine_hours

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHoursEditText: EditText
    private lateinit var saveButton: Button

    private var engineHours = 0

    override fun initUi(view: View) {
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        saveButton = view.findViewById(R.id.saveButton)
        engineHoursEditText = view.findViewById(R.id.engineHoursEditText)

        saveButton.isEnabled = true
        saveButton.setOnClickListener {
            saveButton.setOnClickListener(null)
            saveButton.hideKeyboard()

            this.equipmentUnitId?.let { this.viewModel.saveEngineHours(engineHours.toDouble()) }
        }
    }

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let { this.onBindData(it) }
        })

        this.viewModel.engineHoursSaved.observe(viewLifecycleOwner, Observer { isSaved ->
            if (isSaved) parentFragmentManager.popBackStack()
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        engineHoursEditText.text = Editable.Factory.getInstance().newEditable(display.engineHours)
        modelImageView.setImageResource(display.imageResId)
        serialNumberTextView.text = display.serialNumber
        modelTextView.text = display.modelName
        equipmentNicknameTextView.text = display.nickname
    }
}