package com.android.kubota.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.EditText
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.databinding.library.baseAdapters.BR

class EquipmentSearchViewModel : ObservableViewModel() {

    @get:Bindable
    var pin: String = ""
        set(value) {
            field = value.trim()
            notifyPropertyChanged(BR.pin)
            validate()
        }

    @get:Bindable
    var three: String = ""
        set(value) {
            field = value.trim()
            notifyPropertyChanged(BR.three)
            validate()
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
