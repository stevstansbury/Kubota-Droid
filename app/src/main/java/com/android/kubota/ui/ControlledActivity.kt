package com.android.kubota.ui

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.microsoft.identity.client.AuthenticationResult

interface FlowActivity {
    fun addFragmentToBackStack(fragment: Fragment)
    fun showProgressBar()
    fun hideProgressBar()
}

interface AccountSignUpController {
    fun onAuthSuccess(authenticationResult: AuthenticationResult)
    fun onContinueAsGuest()
}

interface ControlledActivity: FlowActivity {
    fun showKubotaLogoToolbar()
    fun showRegularToolbar()
    fun getSupportFragmentManager(): FragmentManager
    fun setDisplayHomeAsUp(show: Boolean)
}

interface TabbedControlledActivity: ControlledActivity {
    fun getCurrentTab(): Tabs
}