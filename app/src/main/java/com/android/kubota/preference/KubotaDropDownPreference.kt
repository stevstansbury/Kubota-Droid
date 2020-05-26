package com.android.kubota.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceViewHolder
import com.android.kubota.R
import kotlinx.android.synthetic.main.preference_kubota_dropdown.view.*

class KubotaDropDownPreference(context: Context, attrs: AttributeSet) :
    DropDownPreference(context, attrs) {

    init {
        layoutResource = R.layout.preference_kubota_dropdown
    }

    override fun onBindViewHolder(view: PreferenceViewHolder?) {
        view?.itemView?.dropdown_options?.adapter = ArrayAdapter.createFromResource(
            context,
            R.array.preference_units_entries,
            android.R.layout.simple_list_item_1
        )
        view?.itemView?.dropdown_options?.setSelection(0,false)
        view?.itemView?.dropdown_options?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    this@KubotaDropDownPreference.callChangeListener(
                        parent?.adapter?.getItem(
                            position
                        )
                    )
                }
            }
    }
}
