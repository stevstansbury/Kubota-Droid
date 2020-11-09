package com.android.kubota.ui

import androidx.appcompat.app.ActionBar
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager


interface FlowActivity {
    fun addFragmentToBackStack(fragment: Fragment)
    fun clearBackStack()
    fun showProgressBar()
    fun hideProgressBar()
    fun showBlockingActivityIndicator()
    fun hideBlockingActivityIndicator()
    fun makeSnackbar(): Snackbar?
}

interface TabbedActivity: FlowActivity {
    fun getCurrentTab(): Tab
    fun goToTab(tab: Tab)
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
    fun getSupportActionBar(): ActionBar?
    fun getCurrentTab(): Tab
    fun hideActionBar(): Unit?
}
