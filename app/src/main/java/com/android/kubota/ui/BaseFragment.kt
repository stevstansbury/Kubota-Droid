package com.android.kubota.ui

import android.content.Context
import android.support.v4.app.Fragment

abstract class BaseFragment() : Fragment() {
    protected var flowActivity: FlowActivity? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }
}