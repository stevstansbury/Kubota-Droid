package com.android.kubota.ui

import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface FlowActivity {
    fun addFragmentToBackStack(fragment: Fragment)
    fun clearBackStack()
    fun showProgressBar()
    fun hideProgressBar()
    fun makeSnackbar(): Snackbar?
}

interface TabbedActivity: FlowActivity {
    fun getCurrentTab(): Tabs
}

interface AccountController {
    fun changePassword()
    fun signIn()
    fun createAccount()
    fun logout()
}

interface ControlledActivity: FlowActivity {
    fun showKubotaLogoToolbar()
    fun showRegularToolbar()
    fun getSupportFragmentManager(): FragmentManager
    fun setDisplayHomeAsUp(show: Boolean)
}

interface TabbedControlledActivity: ControlledActivity {
    fun getCurrentTab(): Tabs
    fun hideActionBar(): Unit?
    fun hideFAB()
    fun showFAB()
}