package com.android.kubota.ui

import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class EquipmentSearchViewModel : ObservableViewModel() {

    @get:Bindable
    var pin: String = ""
        set(value) {
            field = value.trim()
            validate()
            notifyPropertyChanged(BR.pin)
        }

    @get:Bindable
    var three: String = ""
        set(value) {
            field = value.trim()
            validate()
            notifyPropertyChanged(BR.three)
        }

    @get:Bindable
    var valid: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.valid)
        }

    private fun pinIsValid(): Boolean {
        return pin.length == 5
    }

    private fun threeIsValid(): Boolean {
        return three.length == 3
    }

    private fun validate() {
        valid = pinIsValid() && threeIsValid()
    }
}
