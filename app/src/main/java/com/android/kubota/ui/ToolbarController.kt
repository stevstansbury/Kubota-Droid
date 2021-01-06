package com.android.kubota.ui

import androidx.fragment.app.FragmentManager
import com.android.kubota.R

class MainToolbarController(private val activity: TabbedControlledActivity) {

    fun showRootToolbar(tab: Tab) {
        when (tab) {
            Tab.Equipment,
            Tab.Resources -> {
                activity.setDisplayHomeAsUp(false)
                showActionBarLogo()
            }
            Tab.Dealers -> activity.hideActionBar()
            Tab.Profile -> {
                activity.setDisplayHomeAsUp(false)
                hideActionBarLogo()
            }
        }
    }

    fun showSubScreenToolbar() {
        hideActionBarLogo()
        activity.setDisplayHomeAsUp(true)
    }

    private fun showActionBarLogo() {
        activity.getSupportActionBar()?.let {
            it.setCustomView(R.layout.view_actionbar_logo)
            it.setDisplayShowCustomEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
        activity.showRegularToolbar()
    }

    private fun hideActionBarLogo() {
        activity.getSupportActionBar()?.let {
            it.setDisplayShowCustomEnabled(false)
            it.setDisplayShowTitleEnabled(true)
        }
        activity.showRegularToolbar()
    }

}

interface ToolbarController {
    fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener
}

class SignInToolbarController(private val activity: ControlledActivity) : ToolbarController {

    init {
        activity.showKubotaLogoToolbar()
    }

    override fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener {
        return FragmentManager.OnBackStackChangedListener {
            if (activity.getSupportFragmentManager().backStackEntryCount == 1) {
                activity.showKubotaLogoToolbar()
            } else {
                activity.setDisplayHomeAsUp(true)
                activity.showRegularToolbar()
            }
        }
    }
}
