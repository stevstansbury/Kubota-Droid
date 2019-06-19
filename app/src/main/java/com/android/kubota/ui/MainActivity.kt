package com.android.kubota.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import com.android.kubota.R
import com.android.kubota.extensions.changePassword
import com.android.kubota.extensions.createAccount
import com.android.kubota.extensions.forgotPassword
import com.android.kubota.extensions.login
import com.android.kubota.utility.Constants
import com.android.kubota.utility.Constants.VIEW_MODE_DEALER_LOCATOR
import com.android.kubota.utility.Constants.VIEW_MODE_EQUIPMENT
import com.android.kubota.utility.Constants.VIEW_MODE_MY_DEALERS
import com.android.kubota.utility.Constants.VIEW_MODE_PROFILE
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.kubota.repository.data.Account
import com.kubota.repository.ext.getPublicClientApplication
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_with_progress_bar.*

private const val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1
private const val LOG_IN_REQUEST_CODE = 1
private const val BACK_STACK_ROOT_TAG = "root_fragment"
private const val SELECTED_TAB = "selected_tab"

class MainActivity : BaseActivity(), TabbedControlledActivity, TabbedActivity, AccountController {

    override val rootTag: String? = BACK_STACK_ROOT_TAG

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        when (item.itemId) {
            R.id.navigation_equipment -> {
                if (currentTab is Tabs.Equipment) return@OnNavigationItemSelectedListener false

                Constants.Analytics.setViewMode(VIEW_MODE_EQUIPMENT)
                currentTab = Tabs.Equipment()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onEquipmentTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dealers -> {
                if (currentTab is Tabs.Dealer) return@OnNavigationItemSelectedListener false

                Constants.Analytics.setViewMode(VIEW_MODE_MY_DEALERS)
                currentTab = Tabs.Dealer()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onDealersTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dealer_locator -> {
                if (currentTab is Tabs.Locator) return@OnNavigationItemSelectedListener false

                Constants.Analytics.setViewMode(VIEW_MODE_DEALER_LOCATOR)
                currentTab = Tabs.Locator()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onDealerLocatorTabClicked()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                if (currentTab is Tabs.Profile) return@OnNavigationItemSelectedListener false

                Constants.Analytics.setViewMode(VIEW_MODE_PROFILE)
                currentTab = Tabs.Profile()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onProfileTabClicked()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private lateinit var currentTab: Tabs
    private lateinit var viewModel: UserViewModel

    private val callback = object : AuthenticationCallback {

        override fun onSuccess(authenticationResult: AuthenticationResult?) {
            authenticationResult?.let {
                viewModel.addUser(this@MainActivity, it)
            }
        }

        override fun onCancel() {
            hideProgressBar()
        }

        override fun onError(exception: MsalException?) {
            if (exception?.message?.contains(Constants.FORGOT_PASSWORD_EXCEPTION) == true) {
                getPublicClientApplication().forgotPassword(this@MainActivity, this)
            }

            hideProgressBar()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            currentTab = Tabs.Equipment()
            onEquipmentTabClicked()
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

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)

        var showSignUpActivity = savedInstanceState == null
        viewModel.user.observe(this, Observer {
            if (showSignUpActivity && (it == null || it.isGuest())) {
                startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
            } else if (it?.flags == Account.FLAGS_TOKEN_EXPIRED) {
                viewModel.logout(this)
                SessionExpiredDialogFragment().show(supportFragmentManager, SESSION_EXPIRED_DIALOG_TAG)
            } else if (showSignUpActivity) {
                checkLocationPermissions()
            }

            showSignUpActivity = false
        })
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(SELECTED_TAB, navigation.selectedItemId)
    }

    override fun onBackPressed() {
        val currFragment = supportFragmentManager.findFragmentById(R.id.fragmentPane)
        if (currFragment is BackableFragment) {
            if (currFragment.onBackPressed()) return
        }

        if (supportFragmentManager.backStackEntryCount > 1) {

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
                checkLocationPermissions()
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, MyEquipmentsListFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onDealersTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, MyDealersListFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onDealerLocatorTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, DealerLocatorFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onProfileTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, ProfileFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    override fun hideActionBar() {
        toolbarProgressBar.visibility = View.GONE
        supportActionBar?.hide()
    }

    override fun makeSnackbar(): Snackbar? {
        return super.makeSnackbar()?.apply {
            val lp = view.layoutParams as CoordinatorLayout.LayoutParams
            val xMargin = resources.getDimension(R.dimen.snack_bar_horizontal_margin).toInt()
            val yMargin = resources.getDimension(R.dimen.snack_bar_vertical_margin).toInt()

            lp.setMargins(
                lp.leftMargin + xMargin,
                lp.topMargin,
                lp.rightMargin + xMargin,
                lp.bottomMargin + yMargin
            )
            view.layoutParams = lp
            view.setBackgroundResource(R.drawable.snack_bar_background)
        }
    }


    override fun showKubotaLogoToolbar() {
        super.showKubotaLogoToolbar()
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility = View.INVISIBLE
    }

    override fun showRegularToolbar() {
        super.showRegularToolbar()
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility = View.INVISIBLE
    }

    override fun changePassword() {
        showProgressBar()
        getPublicClientApplication().changePassword(this, callback)
    }

    override fun signIn() {
        showProgressBar()
        getPublicClientApplication().login(this, callback)
    }

    override fun createAccount() {
        showProgressBar()
        getPublicClientApplication().createAccount(this, callback)
    }

    override fun logout() {
        viewModel.logout(this)
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        }
    }
}

sealed class Tabs {
    class Equipment: Tabs()
    class Dealer: Tabs()
    class Locator: Tabs()
    class Profile: Tabs()
}

private const val SESSION_EXPIRED_DIALOG_TAG = "SessionExpiredDialogFragment"

class SessionExpiredDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.session_expired_dialog_title)
            .setMessage(R.string.session_expired_dialog_message)
            .setPositiveButton(R.string.session_expired_button_text) { _, _ ->
                startActivityForResult(Intent(requireContext(), SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
            }
            .create()
    }
}

abstract class BaseDealerFragment : BaseFragment() {
    private var tabbedActivity: TabbedActivity? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is TabbedActivity) {
            tabbedActivity = context
        }
    }

    fun popToRootIfNecessary() {
        if (tabbedActivity?.getCurrentTab() is Tabs.Dealer) {
            fragmentManager?.popBackStack(BACK_STACK_ROOT_TAG, 0)
        }
    }
}