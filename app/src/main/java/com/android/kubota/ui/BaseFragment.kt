package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment


abstract class BaseFragment : Fragment() {
    protected var flowActivity: FlowActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }
}

abstract class BaseAccountSetUpFragment : Fragment() {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
    }

    protected lateinit var accountSetUpContext: AccountSetUpContext
    protected lateinit var actionButton: Button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountSetUpContext = context as AccountSetUpContext
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionButton.setOnClickListener { onActionButtonClicked() }
    }

    abstract fun onActionButtonClicked()
}