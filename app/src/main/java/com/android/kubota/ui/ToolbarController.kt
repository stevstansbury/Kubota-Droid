package com.android.kubota.ui

import androidx.fragment.app.FragmentManager

interface ToolbarController {
    fun getOnBackStackChangedListener() : FragmentManager.OnBackStackChangedListener
}

 private class MainToolbarController(private val activity: TabbedControlledActivity) : ToolbarController {

    override fun getOnBackStackChangedListener(): FragmentManager.OnBackStackChangedListener {
        return FragmentManager.OnBackStackChangedListener {
            when {
                activity.getSupportFragmentManager().backStackEntryCount > 1 -> {
                    if (activity.getCurrentTab() is Tabs.Dealer || activity.getCurrentTab() is Tabs.Locator) {
                        val fragment = activity.getSupportFragmentManager().fragments.firstOrNull { it.isVisible }
                        if (fragment is DealerDetailFragment) {
                            activity.hideActionBar()
                            return@OnBackStackChangedListener
                        }
                    }

                    activity.setDisplayHomeAsUp(true)
                    activity.showRegularToolbar()
                }
                activity.getCurrentTab() is Tabs.Equipment -> activity.showKubotaLogoToolbar()
                else -> {
                    activity.setDisplayHomeAsUp(false)
                    activity.showRegularToolbar()
                }
            }
        }
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