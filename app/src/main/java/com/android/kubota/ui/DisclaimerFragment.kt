package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.android.kubota.R
import com.android.kubota.utility.AccountPrefs

private const val KEY_VIEW_MODE = "view_mode"

class DisclaimerFragment: BaseFragment() {

    companion object {
        private const val VIEW_MODE_NORMAL = 0
        const val VIEW_MODE_RESPONSE_REQUIRED = 1

        fun createInstance(viewMode: Int = VIEW_MODE_NORMAL): DisclaimerFragment {
            val fragment = DisclaimerFragment()
            val args = Bundle(1)
            args.putInt(KEY_VIEW_MODE, viewMode)
            fragment.arguments = args

            return fragment
        }
    }

    override val layoutResId: Int = R.layout.fragment_disclaimer

    private var callback: DisclaimerInterface? = null

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.title = getString(R.string.disclaimer_title)
        }
    }

    override fun initUi(view: View) {
        activity?.title = getString(R.string.disclaimer_title)

        val buttonBar = view.findViewById<LinearLayout>(R.id.buttonBar)
        when (arguments?.getInt(KEY_VIEW_MODE) ?: VIEW_MODE_NORMAL) {
            VIEW_MODE_NORMAL -> {
                buttonBar.visibility = View.GONE
            }
            else -> {
                buttonBar.visibility = View.VISIBLE
                buttonBar.findViewById<View>(R.id.acceptButton).setOnClickListener {
                    AccountPrefs.setDisclaimerAccepted(requireContext())
                    callback?.onDisclaimerAccepted()
                }
                buttonBar.findViewById<View>(R.id.cancelButton).setOnClickListener {
                    callback?.onDisclaimerDeclined()
                }
            }
        }
    }

    override fun loadData() {

    }

    fun setDisclaimerInterface(disclaimedInterface: DisclaimerInterface) {
        callback = disclaimedInterface
    }
}

interface DisclaimerInterface {
    fun onDisclaimerAccepted()
    fun onDisclaimerDeclined()
}