package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.android.kubota.R

class LandingDialogFragment: DialogFragment() {

    companion object {
        private const val TAG = "LandingDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            LandingDialogFragment().show(fragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_fragment_landing, container, false)

        view.findViewById<Button>(R.id.createAccountButton).setOnClickListener { onCreateAccountClicked() }
        view.findViewById<Button>(R.id.signInButton).setOnClickListener { onSignInClicked() }
        view.findViewById<TextView>(R.id.continueTextView).setOnClickListener { onGuestContinueClicked() }

        return view
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setWindowAnimations(R.style.FullScreenDialogSlideAnimation)
    }

    private fun onCreateAccountClicked() {
        AccountSetupActivity.startActivityForCreateAccount(requireContext())
    }

    private fun onSignInClicked() {
        startActivity(Intent(requireContext(), AccountSetupActivity::class.java))
    }

    private fun onGuestContinueClicked() {
        dismiss()
    }
}