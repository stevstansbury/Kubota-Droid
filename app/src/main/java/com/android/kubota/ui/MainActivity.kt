package com.android.kubota.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import com.android.kubota.ui.notification.NotificationDetailFragment
import com.android.kubota.ui.resources.EquipmentModelDetailFragment
import com.android.kubota.utility.AuthPromise
import com.android.kubota.viewmodel.equipment.EquipmentListViewModel
import com.android.kubota.viewmodel.resources.EquipmentCategoriesViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.kubota_toolbar_with_logo.*
import java.util.*


private const val LOG_IN_REQUEST_CODE = 1
private const val SELECTED_TAB = "selected_tab"

class MainActivity : BaseActivity(), TabbedControlledActivity, TabbedActivity, AccountController {

    private val navStack = NavStack(supportFragmentManager, MainToolbarController(this))

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val tab = when (item.itemId) {
            R.id.navigation_equipment -> Tab.Equipment
            R.id.navigation_resources -> Tab.Resources
            R.id.navigation_dealers -> Tab.Dealers
            R.id.navigation_profile -> Tab.Profile
            else -> TODO()
        }
        navStack.showTab(tab, true)
        true
    }

    private lateinit var listener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var rootView: View
    private lateinit var fragmentParentLayout: CoordinatorLayout

    private val equipmentListViewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this)
    }

    private val equipmentCategoriesViewModel: EquipmentCategoriesViewModel by lazy {
        EquipmentCategoriesViewModel.instance(owner = this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
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

        if (savedInstanceState == null) {
            navStack.showTab(Tab.Equipment, false)
        } else if (savedInstanceState.containsKey(Tab.Equipment.name)) {
            navStack.resumeSavedState(savedInstanceState)
            when (savedInstanceState.getInt(SELECTED_TAB, R.id.navigation_equipment)) {
                R.id.navigation_dealers -> {
                    navigation.selectedItemId = R.id.navigation_dealers
                }
                R.id.navigation_resources -> {
                    navigation.selectedItemId = R.id.navigation_resources
                }
                R.id.navigation_profile -> {
                    navigation.selectedItemId = R.id.navigation_profile
                }
                else -> {
                    navigation.selectedItemId = R.id.navigation_equipment
                }
            }
        }

        navigation.setOnNavigationItemSelectedListener(navListener)

        AppProxy.proxy.accountManager.isAuthenticated.observe(this, Observer {
            this.equipmentListViewModel.updateData(this)
        })

        this.equipmentListViewModel.updateData(this)
        this.equipmentCategoriesViewModel.updateData()

        intent?.let { handleDeepLink(it) }
    }

    override fun onResume() {
        super.onResume()

        handleLocaleChange()

        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_TAB, navigation.selectedItemId)
        outState.putAll(navStack.getSavedState())
    }

    override fun onPause() {
        super.onPause()

        rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    override fun clearBackStack() {
        TODO()
    }

    override fun addFragmentToBackStack(fragment: Fragment) {
        navStack.addToBackStack(fragment, getCurrentTab())
    }

    override fun onBackPressed() {
        if (isKeyboardOpen()) {
            rootView.hideKeyboard()
        }

        navigation.setOnNavigationItemSelectedListener(null)
        val didGoBack = navStack.goBack()
        navStack.visibleTab()?.let {
            navigation.selectedItemId = when(it) {
                Tab.Equipment -> R.id.navigation_equipment
                Tab.Resources -> R.id.navigation_resources
                Tab.Dealers -> R.id.navigation_dealers
                Tab.Profile -> R.id.navigation_profile
            }
        }
        navigation.setOnNavigationItemSelectedListener(navListener)

        if (!didGoBack) {
            super.onBackPressed()
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val messageId = intent.extras?.getString("messageId")?.let { UUID.fromString(it) }

        val prefService = AppProxy.proxy.serviceManager.userPreferenceService
        AuthPromise()
            .then { prefService.getInbox(null, null) }
            .map { inboxMessages -> inboxMessages.first { it.id == messageId } }
            .done { addFragmentToBackStack(NotificationDetailFragment.createInstance(it)) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LOG_IN_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    checkLocationPermissions()
                } else {
                    finish()
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun getLayOutResId(): Int = R.layout.activity_main

    override fun getFragmentContainerId(): Int = R.id.fragmentPane

    override fun getCurrentTab(): Tab {
        return when (navigation.selectedItemId) {
            R.id.navigation_equipment -> Tab.Equipment
            R.id.navigation_resources -> Tab.Resources
            R.id.navigation_dealers -> Tab.Dealers
            else -> Tab.Profile
        }
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
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility =
            View.INVISIBLE
    }

    override fun showRegularToolbar() {
        super.showRegularToolbar()
        if (toolbarProgressBar.visibility == View.GONE) toolbarProgressBar.visibility =
            View.INVISIBLE
    }

    override fun changePassword() {
        this.startChangePasswordFlow()
    }

    override fun signIn() {
        this.startSignInFlow()
    }

    override fun createAccount() {
        this.startCreateAccountFlow()
    }

    override fun logout() {
        this.showBlockingActivityIndicator()
        AppProxy.proxy.accountManager.logout()
            .ensure {
                this.hideBlockingActivityIndicator()
            }
    }

    override fun goToTab(tab: Tab) {
        navigation.selectedItemId = when (tab) {
            Tab.Profile -> R.id.navigation_profile
            Tab.Resources -> R.id.navigation_resources
            Tab.Dealers -> R.id.navigation_dealers
            Tab.Equipment -> R.id.navigation_equipment
        }
    }

    private fun checkLocationPermissions() {
        this.requestPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.string.accept_location_permission
        )
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

    private fun calculateMarginOfError() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 50f,
        resources.displayMetrics
    ).toInt()


    //
    // FlowCoordinatorActivity
    //

    override fun onEquipmentUnitAdded(unit: EquipmentUnit) {
        addFragmentToBackStack(
            EquipmentDetailFragment.createInstance(unit)
        )
        makeSnackbar().setText(R.string.equipment_added).setAction(R.string.undo_action) {
            equipmentListViewModel.deleteEquipmentUnit(this, unit.id)
            goToTab(Tab.Equipment)
        }.show()

        equipmentListViewModel.updateData(this)
    }

    override fun onViewEquipmentModel(model: EquipmentModel) {
        goToTab(Tab.Resources)
        addFragmentToBackStack(EquipmentModelDetailFragment.instance(model))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                navStack.goUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun popCurrentTabStack() {
        navStack.goUp()
    }

    private fun handleLocaleChange() {
        if (AppProxy.proxy.preferences.languageTag != Locale.getDefault().toLanguageTag()) {
            AppProxy.proxy.onLocaleChanged()
            equipmentCategoriesViewModel.updateData()
            navStack.clearResourceStack()
        }
    }
}

enum class Tab {
    Equipment,
    Resources,
    Dealers,
    Profile,
}
