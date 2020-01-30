package com.android.kubota.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.view.ViewTreeObserver
import com.android.kubota.R
import com.android.kubota.extensions.*
import com.android.kubota.utility.Constants
import com.android.kubota.utility.Constants.VIEW_MODE_DEALER_LOCATOR
import com.android.kubota.utility.Constants.VIEW_MODE_EQUIPMENT
import com.android.kubota.utility.Constants.VIEW_MODE_MY_DEALERS
import com.android.kubota.utility.Constants.VIEW_MODE_PROFILE
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kubota.repository.data.Account
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

    private lateinit var listener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var currentTab: Tabs
    private lateinit var viewModel: UserViewModel
    private lateinit var fab: FloatingActionButton
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootView = findViewById(R.id.root)
        listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            // Keep a reference to the last state of the keyboard
            private var lastState: Boolean = this@MainActivity.isKeyboardOpen()
            /**
             * Something in the layout has changed
             * so check if the keyboard is open or closed
             * and if the keyboard state has changed
             * save the new state and invoke the callback
             */
            override fun onGlobalLayout() {
                val isOpen = this@MainActivity.isKeyboardOpen()
                if (isOpen == lastState) {
                    return
                } else {
                    dispatchKeyboardEvent(isOpen)
                    lastState = isOpen
                }
            }
        }

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            (supportFragmentManager.findFragmentById(R.id.fragmentPane) as? FabOnClickListener?)?.onFABClick(fab)
        }
        findViewById<View>(R.id.scrim).setOnClickListener {
            fab.isExpanded = false
        }

        findViewById<View>(R.id.scanMenu).setOnClickListener {
            fab.isExpanded = false
            addFragmentToBackStack(AddEquipmentFlowFragment.createScanModeInstance())
        }
        findViewById<View>(R.id.manualEntryMenu).setOnClickListener {
            fab.isExpanded = false
            addFragmentToBackStack(AddEquipmentFlowFragment.createManualEntryModeInstance())
        }

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
            if (it?.flags == Account.FLAGS_TOKEN_EXPIRED) {
                viewModel.logout(this)
                SessionExpiredDialogFragment().show(supportFragmentManager, SESSION_EXPIRED_DIALOG_TAG)
            } else if (showSignUpActivity) {
                checkLocationPermissions()
            }

            showSignUpActivity = false
        })
    }

    override fun onResume() {
        super.onResume()

        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_TAB, navigation.selectedItemId)
    }

    override fun onPause() {
        super.onPause()

        rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    override fun onBackPressed() {
        if (isKeyboardOpen()) {
            rootView.hideKeyboard()
        }

        if (fab.isExpanded) {
            fab.isExpanded = false

            return
        }
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

    override fun startSupportActionMode(callback: androidx.appcompat.view.ActionMode.Callback): androidx.appcompat.view.ActionMode? {
        hideFAB()
        return super.startSupportActionMode(callback)
    }

    override fun onSupportActionModeFinished(mode: androidx.appcompat.view.ActionMode) {
        showFAB()
        super.onSupportActionModeFinished(mode)
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

    override fun hideFAB() {
        fab.hide()
    }

    override fun showFAB() {
        fab.show()
    }

    override fun changePassword() {
        AccountSetupActivity.startActivityForChangePassword(this)
    }

    override fun signIn() {
        AccountSetupActivity.startActivityForSignIn(this)
    }

    override fun createAccount() {
        AccountSetupActivity.startActivityForCreateAccount(this)
    }

    override fun logout() {
        viewModel.logout(this)
    }

    private fun checkLocationPermissions() {
        if (!isLocationEnabled()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        }
    }

    private fun isKeyboardOpen(): Boolean {
        val height = this.resources.displayMetrics.heightPixels
        val rootHeight = rootView.height
        val heightDiff = height - rootHeight
        val marginOfError = calculateMarginOfError()

        return heightDiff > marginOfError
    }

    private fun dispatchKeyboardEvent(isOpen: Boolean) {
        navigation.visibility = if (isOpen) View.GONE else View.VISIBLE
    }

    private fun calculateMarginOfError() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f,
        resources.displayMetrics).toInt()
}

interface FabOnClickListener {
    fun onFABClick(view: FloatingActionButton)
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
                startActivity(Intent(requireContext(), AccountSetupActivity::class.java))
            }
            .create()
    }
}

abstract class BaseDealerFragment : BaseFragment() {
    private var tabbedActivity: TabbedActivity? = null

    override fun onAttach(context: Context) {
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