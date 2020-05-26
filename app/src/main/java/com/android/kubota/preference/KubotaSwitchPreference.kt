package com.android.kubota.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.android.kubota.R
import com.android.kubota.databinding.PreferenceKubotaSwitchBinding

class KubotaSwitchPreference(context: Context, attrs: AttributeSet) :
    SwitchPreference(context, attrs) {

    private var title: String?
    private var summary: String?

    init {
        layoutResource = R.layout.preference_kubota_switch
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.KubotaSwitchPreference,
            0, 0
        ).apply {
            try {
                title = getString(R.styleable.KubotaSwitchPreference_title) ?: ""
                summary = getString(R.styleable.KubotaSwitchPreference_summary)
            } finally {
                recycle()
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val binding = DataBindingUtil.bind<PreferenceKubotaSwitchBinding>(holder!!.itemView)
        binding?.setVariable(BR.title, title)
        binding?.setVariable(BR.summary, summary)
        binding?.bigSwitch?.setOnCheckedChangeListener(checkChangedListener)
        binding?.littleSwitch?.setOnCheckedChangeListener(checkChangedListener)
    }

    private val checkChangedListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        this@KubotaSwitchPreference.callChangeListener(isChecked)
    }
}
