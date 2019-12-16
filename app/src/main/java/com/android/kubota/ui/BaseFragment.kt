package com.android.kubota.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback


abstract class BaseFragment : Fragment() {
    protected var flowActivity: FlowActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }
}

abstract class BaseAccountSetUpFragment : Fragment(), AccountSetUpFragment {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
    }

    protected lateinit var accountSetUpContext: AccountSetUpContext

    override fun onAttach(context: Context) {
        super.onAttach(context)

        accountSetUpContext = context as AccountSetUpContext
        accountSetUpContext.setNextButtonEnable(false)
        accountSetUpContext.setNextButtonText(getActionButtonText())

        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                onBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )
    }

    @StringRes abstract fun getActionButtonText(): Int
}