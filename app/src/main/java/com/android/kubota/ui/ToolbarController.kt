package com.android.kubota.ui

import android.support.v4.app.FragmentManager

interface ToolbarController {
    fun getOnBackStackChangedListener() : FragmentManager.OnBackStackChangedListener;
}

 private class MainToolbarController(private val activity: TabbedControlledActivity) : ToolbarController {

    override fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener {
        return FragmentManager.OnBackStackChangedListener {
            activity.setDisplayHomeAsUp(activity.getSupportFragmentManager().backStackEntryCount > 1)
            if (activity.getSupportFragmentManager().backStackEntryCount > 1) {
                activity.showRegularToolbar()
            } else if (activity.getCurrentTab() is Tabs.Equipment) {
                activity.showKubotaLogoToolbar()
            } else {
                activity.showRegularToolbar()
            }
        }
    }

}

private class SignInToolbarController(private val activity: ControlledActivity) : ToolbarController {

    init {
        activity.setDisplayHomeAsUp(true)
    }

    override fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener {
        return FragmentManager.OnBackStackChangedListener {
            if (activity.getSupportFragmentManager().backStackEntryCount == 1) {
                activity.showKubotaLogoToolbar()
            } else {
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