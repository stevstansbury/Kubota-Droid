package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.kubota.repository.ext.getPublicClientApplication
import com.microsoft.identity.client.*

class SignUpActivity(): BaseActivity(), AccountSignUpController {

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        viewModel.user.observe(this, Observer {
            if (it == null) {
                viewModel.addGuestAccount()
            }

            if (it != null && !it.isGuest()) {
                navigateToMainActivity()
            } else if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentPane, SignUpFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        })
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
        } else {
            finish()
        }
    }

    override fun getLayOutResId(): Int = R.layout.activity_sign_up

    override fun getFragmentContainerId(): Int = R.id.fragmentPane

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        getPublicClientApplication().handleInteractiveRequestRedirect(requestCode, resultCode, data)
    }

    private fun navigateToMainActivity() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onAuthSuccess(authenticationResult: AuthenticationResult) {
        viewModel.addUser(context = this, authenticationResult = authenticationResult)
    }

    override fun onContinueAsGuest() {
        navigateToMainActivity()
    }
}