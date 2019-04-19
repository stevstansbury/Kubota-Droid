package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.view.View
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.kubota.repository.ext.getPublicClientApplication
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_with_progress_bar.*

class MainActivity : BaseActivity(), TabbedControlledActivity {
    companion object {
        const val LOG_IN_REQUEST_CODE = 1
        private const val BACK_STACK_ROOT_TAG = "root_fragment"
        private const val SELECTED_TAB = "selected_tab"
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        when (item.itemId) {
            R.id.navigation_equipment -> {
                if (currentTab is Tabs.Equipment) return@OnNavigationItemSelectedListener false

                currentTab = Tabs.Equipment()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onEquipmentTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dealers -> {
                if (currentTab is Tabs.Dealer) return@OnNavigationItemSelectedListener false

                currentTab = Tabs.Dealer()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onDealersTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dealer_locator -> {
                if (currentTab is Tabs.Locator) return@OnNavigationItemSelectedListener false

                currentTab = Tabs.Locator()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onDealerLocatorTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                if (currentTab is Tabs.Profile) return@OnNavigationItemSelectedListener false

                currentTab = Tabs.Profile()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onProfileTabClicked()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private lateinit var currentTab: Tabs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        val viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        if (savedInstanceState == null) {
            currentTab = Tabs.Equipment()
            if (viewModel.user.value == null) {
                startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
            } else {
                val isGuest = viewModel.user.value?.isGuest() ?: true
                if (isGuest) {
                    startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
                } else {
                    onEquipmentTabClicked()
                }
            }
        } else {
            when(savedInstanceState.getInt(SELECTED_TAB, R.id.navigation_equipment)) {
                R.id.navigation_dealers -> {
                    currentTab = Tabs.Dealer()
                    navigation.selectedItemId = R.id.navigation_dealers
                }
                R.id.navigation_dealer_locator -> {
                    currentTab = Tabs.Locator()
                    navigation.selectedItemId = R.id.navigation_dealer_locator
                }
                R.id.navigation_profile -> {
                    currentTab = Tabs.Profile()
                    navigation.selectedItemId = R.id.navigation_profile
                }
                else -> {
                    currentTab = Tabs.Equipment()
                    navigation.selectedItemId = R.id.navigation_equipment
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(SELECTED_TAB, navigation.selectedItemId)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            restoreStatusBarColor()

            super.onBackPressed()
        } else if (navigation.selectedItemId != R.id.navigation_equipment) {
            navigation.selectedItemId = R.id.navigation_equipment
        } else if (navigation.selectedItemId == R.id.navigation_equipment && supportFragmentManager.backStackEntryCount == 1) {
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LOG_IN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onEquipmentTabClicked()
            } else {
                finish()
            }
            return
        }

        if (requestCode ==  AuthenticationConstants.UIRequest.BROWSER_FLOW) {
            getPublicClientApplication().handleInteractiveRequestRedirect(requestCode, resultCode, data)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun getLayOutResId(): Int = R.layout.activity_main

    override fun getFragmentContainerId(): Int = R.id.fragmentPane

    override fun getCurrentTab(): Tabs {
        return when(navigation.selectedItemId) {
            R.id.navigation_equipment -> Tabs.Equipment()
            R.id.navigation_dealers -> Tabs.Dealer()
            R.id.navigation_dealer_locator -> Tabs.Locator()
            else -> Tabs.Profile()
        }
    }

    private fun onEquipmentTabClicked() {
        restoreStatusBarColor()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, MyEquipmentsListFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onDealersTabClicked() {
        restoreStatusBarColor()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, MyDealersListFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onDealerLocatorTabClicked() {
        restoreStatusBarColor()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, DealerLocatorFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onProfileTabClicked() {
        restoreStatusBarColor()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, ProfileFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    override fun hideActionBar() {
        toolbarProgressBar.visibility = View.GONE
        supportActionBar?.hide()
    }

    override fun showKubotaLogoToolbar() {
        super.showKubotaLogoToolbar()
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility = View.INVISIBLE
    }

    override fun showRegularToolbar() {
        super.showRegularToolbar()
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility = View.INVISIBLE
    }

    private fun restoreStatusBarColor() {
        if (supportFragmentManager.findFragmentById(R.id.fragmentPane) is DealerDetailFragment) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        }
    }
}

sealed class Tabs {
    class Equipment: Tabs()
    class Dealer: Tabs()
    class Locator: Tabs()
    class Profile: Tabs()
}
