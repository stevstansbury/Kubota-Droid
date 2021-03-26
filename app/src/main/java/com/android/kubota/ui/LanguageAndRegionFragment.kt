package com.android.kubota.ui

import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.Button
import com.android.kubota.R

class LanguageAndRegionFragment: BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_language_and_region

    private lateinit var changeLanguageButton: Button

    override fun initUi(view: View) {
        activity?.title = getString(R.string.language_country_region)

        changeLanguageButton = view.findViewById(R.id.changeLanguageButton)
        changeLanguageButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityForResult(intent, 0)
        }
    }

    override fun loadData() {}

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.title = getString(R.string.language_country_region)
        }
    }
}