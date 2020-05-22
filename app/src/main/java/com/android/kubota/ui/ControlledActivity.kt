package com.android.kubota.ui

import androidx.appcompat.app.ActionBar
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.promisekt.Promise

interface FlowActivity {
    fun addFragmentToBackStack(fragment: Fragment)
    fun clearBackStack()
    fun showProgressBar()
    fun hideProgressBar()
    fun makeSnackbar(): Snackbar?
}

interface TabbedActivity: FlowActivity {
    fun getCurrentTab(): Tabs
    fun goToTab(tab: Tabs)
}

interface AccountController {
    fun changePassword()
    fun signIn()
    fun createAccount()
    fun logout()
    fun signInAsync(): Promise<Unit>
}

interface ControlledActivity: FlowActivity {
    fun showKubotaLogoToolbar()
    fun showRegularToolbar()
    fun getSupportFragmentManager(): FragmentManager
    fun setDisplayHomeAsUp(show: Boolean)
}

interface TabbedControlledActivity: ControlledActivity {
    fun getSupportActionBar(): ActionBar?
    fun getCurrentTab(): Tabs
    fun hideActionBar(): Unit?
}