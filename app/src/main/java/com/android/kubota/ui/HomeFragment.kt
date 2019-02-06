package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.HomeViewModel

class HomeFragment(): Fragment() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, null)

        val factory = InjectorUtils.provideHomeViewModelFactory(context!!)
        viewModel = ViewModelProviders.of(this, factory).get(HomeViewModel::class.java)

        val selectedModelLayout = view.findViewById<LinearLayout>(R.id.selectedModelLayout)
        viewModel.selectedModel.observe(this, Observer {
            resetPreferenceLayoutOrientation(selectedModelLayout, getNewPreferenceLayoutOrientation(it))
            val text1 = if (it != null) context?.getText(R.string.selected_equipment) else context?.getText(R.string.select_your_equipment)
            val text2 = if (it != null) it else context?.getText(R.string.plus_symbol)
            updatePreferencesTextViews(selectedModelLayout, text1, text2)
        })

        selectedModelLayout.setOnClickListener {
            if (viewModel.selectedModel.value == null) {
                // TODO Attach the AddEquipmentFragment
            } else {
                // TODO Attach the MyEquipmentFragment
            }
        }

        val selectedDealerLayout = view.findViewById<LinearLayout>(R.id.selectedDealerLayout)
        viewModel.selectedDealer.observe(this, Observer {
            resetPreferenceLayoutOrientation(selectedDealerLayout, getNewPreferenceLayoutOrientation(it))
            val text1 = if (it != null) context?.getText(R.string.selected_dealer) else context?.getText(R.string.select_your_dealer)
            val text2 = if (it != null) it else context?.getText(R.string.plus_symbol)
            updatePreferencesTextViews(selectedDealerLayout, text1, text2)
        })

        selectedDealerLayout.setOnClickListener {
            if (viewModel.selectedDealer.value == null) {
                // TODO Attach the DealerLocatorFragment
            } else {
                // TODO Attach the MyDealersFragment
            }
        }


        return view
    }

    private fun getNewPreferenceLayoutOrientation(observedValue: String?) =
        if (observedValue == null) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

    private fun resetPreferenceLayoutOrientation(layout: LinearLayout, newOrientation: Int) {
        if (layout.orientation != newOrientation) {
            layout.removeAllViews()
            layout.orientation = newOrientation
            val layoutRes = if (newOrientation == LinearLayout.VERTICAL) R.layout.selected_preference_view else R.layout.no_selected_preference_view
            activity?.layoutInflater?.inflate(layoutRes, layout, true)
        }
    }

    private fun updatePreferencesTextViews(layout: LinearLayout, text1: CharSequence?, text2: CharSequence?) {
        layout.findViewById<TextView>(R.id.text1).text = text1
        layout.findViewById<TextView>(R.id.text2).text = text2
    }
}