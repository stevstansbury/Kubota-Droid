package com.android.kubota.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.preference.DropDownPreference
import com.android.kubota.R

class KubotaDropDownPreference(context: Context, attrs: AttributeSet) :
    DropDownPreference(context, attrs) {

    init {
        layoutResource = R.layout.preference_kubota_dropdown
    }

    override fun createAdapter(): ArrayAdapter<*> {
        return ArrayAdapter<Any>(context, android.R.layout.simple_list_item_1)
    }
}
