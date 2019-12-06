package com.android.kubota.ui


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.android.kubota.R
import com.android.kubota.extensions.createAccount
import com.android.kubota.extensions.forgotPassword
import com.android.kubota.utility.Constants.FORGOT_PASSWORD_EXCEPTION
import com.kubota.repository.ext.getPublicClientApplication
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException

class SignUpFragment : BaseFragment() {

    private var accountSignUpController: AccountSignUpController? = null
    private var isLoading = false
    private val callback = object : AuthenticationCallback {

        override fun onSuccess(authenticationResult: IAuthenticationResult?) {
            authenticationResult?.let {
                accountSignUpController?.onAuthSuccess(it)
            }
        }

        override fun onCancel() {
            isLoading = false
            flowActivity?.hideProgressBar()
        }

        override fun onError(exception: MsalException?) {
            if (exception?.message?.contains(FORGOT_PASSWORD_EXCEPTION) == true) {
                activity?.let {
                    it.getPublicClientApplication().forgotPassword(it, this)
                }
            }
            isLoading = false
            flowActivity?.hideProgressBar()
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AccountSignUpController) {
            accountSignUpController = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        view.findViewById<Button>(R.id.createAccountButton).setOnClickListener { onCreateAccountClicked() }
        view.findViewById<Button>(R.id.signInButton).setOnClickListener { onSignInClicked() }
        view.findViewById<TextView>(R.id.continueTextView).setOnClickListener { onGuestContinueClicked() }
        view.findViewById<TextView>(R.id.termsAndConditionsLink).setOnClickListener { onTermsAndConditionsClicked() }

        return view
    }

    private fun onCreateAccountClicked() {
        if (!isLoading) {
            activity?.let {
                isLoading = true
                flowActivity?.showProgressBar()
                it.getPublicClientApplication().createAccount(it, callback)
            }
        }
    }

    private fun onSignInClicked() {
        startActivity(Intent(requireContext(), AccountSetupActivity::class.java))
    }

    private fun onGuestContinueClicked() {
        if (!isLoading) {
            accountSignUpController?.onContinueAsGuest()
        }
    }

    private fun onTermsAndConditionsClicked() {
        flowActivity?.addFragmentToBackStack(LegalTermsFragment())
    }
}
