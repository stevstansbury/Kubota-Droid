package com.android.kubota.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import androidx.lifecycle.Observer
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
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.extensions.*
import com.android.kubota.ui.dealer.DealersFragment
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.android.kubota.ui.resources.CategoriesFragment
import com.android.kubota.utility.Constants
import com.android.kubota.utility.Constants.VIEW_MODE_EQUIPMENT
import com.android.kubota.utility.Constants.VIEW_MODE_MY_DEALERS
import com.android.kubota.utility.Constants.VIEW_MODE_PROFILE
import com.android.kubota.utility.Constants.VIEW_MODE_RESOURCES
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.kubota.repository.data.Account
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.kubota_toolbar_with_logo.*
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
            R.id.navigation_resources -> {
                if (currentTab !is Tabs.Resources) {
                    Constants.Analytics.setViewMode(VIEW_MODE_RESOURCES)
                    currentTab = Tabs.Resources()
                } else if (supportFragmentManager.findFragmentById(R.id.fragmentPane) is CategoriesFragment) {
                    return@OnNavigationItemSelectedListener false
                }

                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onResourcesTabClicked()

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dealers -> {
                if (currentTab is Tabs.Dealers) return@OnNavigationItemSelectedListener false

                Constants.Analytics.setViewMode(VIEW_MODE_MY_DEALERS)
                currentTab = Tabs.Dealers()
                supportFragmentManager.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                onDealersTabClicked()
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
    private lateinit var rootView: View

    @SuppressLint("NewApi")
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

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            currentTab = Tabs.Equipment()
            onEquipmentTabClicked()
        } else {
            when(savedInstanceState.getInt(SELECTED_TAB, R.id.navigation_equipment)) {
                R.id.navigation_dealers -> {
                    currentTab = Tabs.Dealers()
                    navigation.selectedItemId = R.id.navigation_dealers
                }
                R.id.navigation_resources -> {
                    currentTab = Tabs.Resources()
                    navigation.selectedItemId = R.id.navigation_resources
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
        viewModel = ViewModelProvider(this, factory)
            .get(UserViewModel::class.java)

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

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun getLayOutResId(): Int = R.layout.activity_main

    override fun getFragmentContainerId(): Int = R.id.fragmentPane

    override fun getCurrentTab(): Tabs {
        return when(navigation.selectedItemId) {
            R.id.navigation_equipment -> Tabs.Equipment()
            R.id.navigation_resources -> Tabs.Resources()
            R.id.navigation_dealers -> Tabs.Dealers()
            else -> Tabs.Profile()
        }
    }

    private fun onEquipmentTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, MyEquipmentsListFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onResourcesTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, CategoriesFragment())
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commitAllowingStateLoss()
    }

    private fun onDealersTabClicked() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, DealersFragment())
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
        toolbarWithLogo.visibility = View.GONE
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
        viewModel.user.value?.accessToken?.let {
            AccountSetupActivity.startActivityForChangePassword(this, it)
        }
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

sealed class Tabs {
    class Equipment: Tabs()
    class Resources: Tabs()
    class Dealers: Tabs()
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