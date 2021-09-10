package com.android.kubota.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.EditText
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.databinding.library.baseAdapters.BR
import com.kubota.service.domain.EquipmentModel

class EquipmentSearchViewModel : ObservableViewModel() {

    @get:Bindable
    var type: EquipmentModel.Type = EquipmentModel.Type.Machine
        private set

    @get:Bindable
    var pinOrSerial: String = ""
        set(value) {
            field = value.trim()
            notifyPropertyChanged(BR.pinOrSerial)
            validate()
        }

    @get:Bindable
    var model: String = ""
        set(value) {
            field = value.trim()
            notifyPropertyChanged(BR.model)
            validate()
        }

    @get:Bindable
    var valid: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.valid)
        }

    fun setEquipmentType(equipmentType: EquipmentModel.Type) {
        type = equipmentType
        notifyPropertyChanged(BR.type)
    }

    private fun pinOrSerialIsValid(): Boolean {
        return pinOrSerial.length >= 5
    }

    private fun modelIsValid(): Boolean {
        return if (type == EquipmentModel.Type.Machine) {
            model.length == 3
        } else {
            model.isNotEmpty()
        }
    }

    private fun validate() {
        valid = pinOrSerialIsValid() && modelIsValid()
    }
}

@SuppressLint("ClickableViewAccessibility")
fun EditText.onRightDrawableClicked(onClicked: (view: EditText) -> Unit) {
    this.setOnTouchListener { v, event ->
        var hasConsumed = false
        if (v is EditText) {
            if (event.x >= v.width - v.totalPaddingRight) {
                if (event.action == MotionEvent.ACTION_UP) {
                    onClicked(this)
                }
                hasConsumed = true
            }
        }
        hasConsumed
    }
}

@BindingAdapter("android:text")
fun EditText.toggleClearButton(old: String?, new: String?) {
    when (new?.isNotEmpty()) {
        true -> this.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            android.R.drawable.presence_offline,
            0
        )
        else -> this.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
    }
}
