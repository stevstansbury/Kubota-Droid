package com.android.kubota.preference

import android.content.Context
import android.util.AttributeSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.android.kubota.R
import com.android.kubota.databinding.PreferenceKubotaSwitchBinding

class KubotaSwitchPreference(context: Context, attrs: AttributeSet) :
    SwitchPreference(context, attrs) {

    init {
        layoutResource = R.layout.preference_kubota_switch
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.KubotaSwitchPreference,
            0, 0
        )
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val binding = DataBindingUtil.bind<PreferenceKubotaSwitchBinding>(holder!!.itemView)
        binding?.setVariable(BR.title, title)
        binding?.setVariable(BR.summary, summary)
        binding?.setVariable(BR.isChecked, mChecked)
    }
}
