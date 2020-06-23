package com.android.kubota.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.extensions.isLocationEnabled
import com.android.kubota.ui.dealer.DealersFragment
import com.android.kubota.ui.equipment.AddEquipmentActivity
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import com.android.kubota.ui.equipment.MyEquipmentsListFragment
import com.android.kubota.ui.equipment.MyEquipmentsListFragment.Companion.ADD_EQUIPMENT_REQUEST_CODE
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.android.kubota.ui.resources.CategoriesFragment
import com.android.kubota.utility.Constants
import com.android.kubota.utility.Constants.VIEW_MODE_EQUIPMENT
import com.android.kubota.utility.Constants.VIEW_MODE_MY_DEALERS
import com.android.kubota.utility.Constants.VIEW_MODE_PROFILE
import com.android.kubota.utility.Constants.VIEW_MODE_RESOURCES
import com.android.kubota.viewmodel.equipment.EquipmentListViewModel
import com.android.kubota.viewmodel.resources.EquipmentCategoriesViewModel
import com.inmotionsoftware.promisekt.Promise
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.kubota_toolbar_with_logo.*
import kotlinx.android.synthetic.main.toolbar_with_progress_bar.*
import java.lang.ref.WeakReference
import java.util.*

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
    private lateinit var rootView: View
    private lateinit var fragmentParentLayout: CoordinatorLayout

    private val equipmentListViewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this, signInHandler = WeakReference { this.signInAsync() })
    }

    private val equipmentCategoriesViewModel: EquipmentCategoriesViewModel by lazy {
        EquipmentCategoriesViewModel.instance(owner = this)
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootView = findViewById(R.id.root)
        fragmentParentLayout = findViewById(R.id.coordinatorLayout)
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

        AppProxy.proxy.accountManager.isAuthenticated.observe(this, Observer {
            this.equipmentListViewModel.updateData()
        })

        this.equipmentListViewModel.updateData()
        this.equipmentCategoriesViewModel.updateData()
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
        } else if (requestCode == ADD_EQUIPMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(AddEquipmentActivity.NEW_EQUIPMENT_UUID)?.let {
                val unitId = UUID.fromString(it)
                addFragmentToBackStack(
                    EquipmentDetailFragment.createInstance(unitId)
                )
                makeSnackbar().setText(R.string.equipment_added).setAction(R.string.undo_action) {
                    equipmentListViewModel.deleteEquipmentUnit(unitId)
                    goToTab(Tabs.Equipment())

                }.show()
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
            .replace(R.id.fragmentPane,
                MyEquipmentsListFragment()
            )
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

    override fun makeSnackbar(): Snackbar {
        return Snackbar.make(fragmentParentLayout, "", Snackbar.LENGTH_SHORT).apply {
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
        // TODO: Need to pass in the forgotPasswordToken?
        AccountSetupActivity.startActivityForChangePassword(this)
    }

    override fun signIn() {
        AccountSetupActivity.startActivityForSignIn(this)
    }

    override fun signInAsync(): Promise<Unit> {
        SessionExpiredDialogFragment().show(supportFragmentManager, SESSION_EXPIRED_DIALOG_TAG)

        // FIXME: Need to start the AcccountSetupActivity and wait for result
        return Promise.value(Unit)
    }

    override fun createAccount() {
        AccountSetupActivity.startActivityForCreateAccount(this)
    }

    override fun logout() {
        AppProxy.proxy.accountManager.logout()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            ScannerFragment.CAMERA_PERMISSION -> {
                when {
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.
                        addFragmentToBackStack(ScannerFragment())
                    }
                    grantResults.isEmpty() -> {
                        // the user did not deny permissions
                    }
                    else -> {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        android.app.AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setMessage(getString(R.string.accept_camera_permission))
                            .setPositiveButton("Ok") { _: DialogInterface, _: Int -> }
                            .show()
                    }
                }
            }
        }
    }

    override fun goToTab(tab: Tabs) {
        navigation.selectedItemId = when (tab) {
            is Tabs.Profile -> R.id.navigation_profile
            is Tabs.Resources -> R.id.navigation_resources
            is Tabs.Dealers -> R.id.navigation_dealers
            is Tabs.Equipment -> R.id.navigation_equipment
        }
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

const val SESSION_EXPIRED_DIALOG_TAG = "SessionExpiredDialogFragment"

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
