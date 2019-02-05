package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.android.kubota.R
import com.android.kubota.extensions.createAccount
import com.android.kubota.extensions.login
import com.android.kubota.extensions.getPublicClientApplication
import com.android.kubota.utilities.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity(): AppCompatActivity() {

    private lateinit var viewModel: UserViewModel
    private val callback = object : AuthenticationCallback {

        override fun onSuccess(authenticationResult: AuthenticationResult?) {
            authenticationResult?.let {
                viewModel.addUser(it);
            }
        }

        override fun onCancel() {
            progressBar.visibility = View.GONE
        }

        override fun onError(exception: MsalException?) {
            progressBar.visibility = View.GONE
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        viewModel.user.observe(this, Observer {
            it?.let {
                if (!it.isGuest()) {
                    navigateToMainActivity()
                }
            }
        })

        createAccountButton.setOnClickListener { onCreateAccountClicked() }
        signInButton.setOnClickListener { onSignInClicked() }
        continueTextView.setOnClickListener { onGuestContinueClicked() }
        termsAndConditionsLink.setOnClickListener { onTermsAndConditionsClicked() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        getPublicClientApplication().handleInteractiveRequestRedirect(requestCode, resultCode, data)
    }

    private fun onCreateAccountClicked() {
        if (progressBar.visibility != View.VISIBLE) {
            progressBar.visibility = View.VISIBLE
            getPublicClientApplication().createAccount(this, callback)
        }
    }

    private fun onSignInClicked() {
        if (progressBar.visibility != View.VISIBLE) {
            progressBar.visibility = View.VISIBLE
            val pca = getPublicClientApplication();
            getPublicClientApplication().login(this, callback)
        }
    }

    private fun onGuestContinueClicked() {
        if (progressBar.visibility != View.VISIBLE) {
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onTermsAndConditionsClicked() {
        if (progressBar.visibility != View.VISIBLE) {
            //TODO: Launch Terms and Conditions screen once available
        }
    }

}