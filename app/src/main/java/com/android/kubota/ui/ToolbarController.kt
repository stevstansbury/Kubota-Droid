package com.android.kubota.ui

import androidx.fragment.app.FragmentManager
import com.android.kubota.R

interface ToolbarController {
    fun getOnBackStackChangedListener() : FragmentManager.OnBackStackChangedListener
}

 private class MainToolbarController(private val activity: TabbedControlledActivity) : ToolbarController {

    override fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener {
        return FragmentManager.OnBackStackChangedListener {
            when {
                activity.getSupportFragmentManager().backStackEntryCount > 1 -> {
                    hideActionBarLogo()
                    activity.setDisplayHomeAsUp(true)
                }
                activity.getCurrentTab() is Tabs.Dealers -> activity.hideActionBar()
                activity.getCurrentTab() is Tabs.Profile ->  {
                    activity.setDisplayHomeAsUp(false)
                    hideActionBarLogo()
                }
                else -> {
                    activity.setDisplayHomeAsUp(false)
                    showActionBarLogo()
                }
            }
        }
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

private class SignInToolbarController(private val activity: ControlledActivity) : ToolbarController {

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

class ToolbarControllerFactory {
    companion object {
        fun createToolbarController(controlledActivity: ControlledActivity): ToolbarController {
            if (controlledActivity is TabbedControlledActivity) {
                return MainToolbarController(activity = controlledActivity)
            }
            return SignInToolbarController(activity = controlledActivity)
        }
    }
}