package com.android.kubota.ui.equipment

import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.EquipmentUnitUpdateType
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

            this.equipmentUnitId?.let { saveEngineHours(equipmentId = it, hours = engineHours.toDouble()) }
        }
    }

    override fun onDataLoaded(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        engineHoursEditText.text = Editable.Factory.getInstance().newEditable(display.engineHours)
        modelImageView.setImageResource(display.imageResId)
        serialNumberTextView.text = display.serialNumber
        modelTextView.text = display.modelName
        equipmentNicknameTextView.text = display.nickname
    }

    private fun saveEngineHours(equipmentId: UUID, hours: Double) {
        this.showProgressBar()
        AuthPromise()
            .onSignIn { signIn() }
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                        .updateEquipmentUnit(type = EquipmentUnitUpdateType.UnverifiedEngineHours(equipmentId, hours))
            }
            .done { parentFragmentManager.popBackStack() }
            .ensure { this.hideProgressBar() }
            .catch { this.showError(it) }
    }

}