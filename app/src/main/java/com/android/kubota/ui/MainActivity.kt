package com.android.kubota.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.dealer.DealersFragment
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import com.android.kubota.ui.equipment.MyEquipmentsListFragment
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.android.kubota.ui.resources.CategoriesFragment
import com.android.kubota.ui.resources.EquipmentModelDetailFragment
import com.android.kubota.viewmodel.equipment.EquipmentListViewModel
import com.android.kubota.viewmodel.resources.EquipmentCategoriesViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.kubota_toolbar_with_logo.*

private const val LOG_IN_REQUEST_CODE = 1
private const val SELECTED_TAB = "selected_tab"


class NavigationStack(private val fragmentManager: FragmentManager) {
    private val visitOrderStack = mutableListOf<Tab>()

    private val equipmentTabStack = mutableListOf<Fragment>()
    private val resourcesTabStack = mutableListOf<Fragment>()
    private val dealersTabStack = mutableListOf<Fragment>()
    private val profileTabStack = mutableListOf<Fragment>()

    fun currentTab(): Tab? = visitOrderStack.lastOrNull()

    private fun getCurrentlyVisibleFragment(): Fragment? {
        return when (currentTab()) {
            Tab.Equipment -> equipmentTabStack.lastOrNull()
            Tab.Resources -> resourcesTabStack.lastOrNull()
            Tab.Dealers -> dealersTabStack.lastOrNull()
            Tab.Profile -> profileTabStack.lastOrNull()
            else -> null
        }
    }

    private fun <T> MutableList<T>.removeLastOrNull(): T? =
        if (isEmpty()) null else removeAt(lastIndex)

    private fun popStackFragment(): Fragment? {
        check(visitOrderStack.size == equipmentTabStack.size + resourcesTabStack.size + dealersTabStack.size + profileTabStack.size)

        fun removeFragment(currentTab: Tab) {
            when (currentTab) {
                Tab.Equipment -> equipmentTabStack.removeLastOrNull()
                Tab.Resources -> resourcesTabStack.removeLastOrNull()
                Tab.Dealers -> dealersTabStack.removeLastOrNull()
                Tab.Profile -> profileTabStack.removeLastOrNull()
            }
        }

        val previousTab = visitOrderStack.removeLastOrNull()?.let { currentTab ->
            removeFragment(currentTab)
            visitOrderStack.lastOrNull()
        }
        return when (previousTab) {
            Tab.Equipment -> equipmentTabStack.lastOrNull()
            Tab.Resources -> resourcesTabStack.lastOrNull()
            Tab.Dealers -> dealersTabStack.lastOrNull()
            Tab.Profile -> profileTabStack.lastOrNull()
            else -> null
        }
    }

    fun show(tab: Tab, fragment: Fragment) {
        fun showInternal(tabRoot: Class<Fragment>, tabStack: MutableList<Fragment>) = handleShowTab(
            newTab = tab,
            previousTab = visitOrderStack.lastOrNull(),
            currentlyVisible = getCurrentlyVisibleFragment(),
            newFragment = fragment,
            tabRoot = tabRoot,
            tabStack = tabStack
        )
        when (tab) {
            Tab.Equipment -> showInternal(MyEquipmentsListFragment::class.java as Class<Fragment>, equipmentTabStack)
            Tab.Resources -> showInternal(CategoriesFragment::class.java as Class<Fragment>, resourcesTabStack)
            Tab.Dealers -> showInternal(DealersFragment::class.java as Class<Fragment>, dealersTabStack)
            Tab.Profile -> showInternal(ProfileFragment::class.java as Class<Fragment>, profileTabStack)
        }
    }

    fun tryToGoBackOrFalse(): Boolean {
        val currentlyVisible = getCurrentlyVisibleFragment()
        val previousFragment = popStackFragment() ?: return false

        check(currentlyVisible != previousFragment)
        fragmentManager.beginTransaction()
            .hide(currentlyVisible!!)
            .show(previousFragment)
            .commit()

        return true
    }

    private fun handleShowTab(
        newTab: Tab,
        previousTab: Tab?,
        currentlyVisible: Fragment?,
        newFragment: Fragment,
        tabRoot: Class<Fragment>,
        tabStack: MutableList<Fragment>
    ) {
        val currentIsTabRoot = currentlyVisible?.let { tabRoot.isInstance(it) } ?: false
        when (currentIsTabRoot) {
            true -> when (tabRoot.isInstance(newFragment)) {
                true -> {
                    // noOp
                }
                false -> {
                    visitOrderStack.add(newTab)
                    tabStack.add(newFragment)
                    check(currentlyVisible != newFragment)
                    fragmentManager.beginTransaction()
                        .hide(currentlyVisible!!)
                        .add(R.id.fragmentPane, newFragment)
                        .commit()
                }
            }
            false -> {
                if (tabRoot.isInstance(newFragment)) {
                    if (previousTab == newTab || tabStack.isEmpty()) {
                        visitOrderStack.add(newTab)
                        tabStack.add(newFragment)
                        check(currentlyVisible != newFragment)
                        fragmentManager.beginTransaction()
                            .hideNullable(currentlyVisible)
                            .add(R.id.fragmentPane, newFragment)
                            .commit()
                    } else {
                        val currentTabTop = tabStack.last()
                        visitOrderStack.add(newTab)
                        tabStack.add(currentTabTop)
                        check(currentlyVisible != currentTabTop)
                        fragmentManager.beginTransaction()
                            .hideNullable(currentlyVisible)
                            .show(currentTabTop)
                            .commit()
                    }
                } else {
                    visitOrderStack.add(newTab)
                    tabStack.add(newFragment)
                    check(currentlyVisible != newFragment)
                    fragmentManager.beginTransaction()
                        .hideNullable(currentlyVisible)
                        .add(R.id.fragmentPane, newFragment)
                        .commit()
                }
            }
        }
    }

    private fun FragmentTransaction.hideNullable(fragment: Fragment?): FragmentTransaction {
        return this.apply {
            fragment?.let {
                this.hide(it)
            }
        }
    }
}

class MainActivity : BaseActivity(), TabbedControlledActivity, TabbedActivity, AccountController {

    private val navigationStack = NavigationStack(supportFragmentManager)

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val (tab, fragment) = when (item.itemId) {
            R.id.navigation_equipment -> Tab.Equipment to MyEquipmentsListFragment()
            R.id.navigation_resources -> Tab.Resources to CategoriesFragment()
            R.id.navigation_dealers -> Tab.Dealers to DealersFragment()
            R.id.navigation_profile -> Tab.Profile to ProfileFragment()
            else -> TODO()
        }
        navigationStack.show(tab, fragment)
        true
    }

    private lateinit var listener: ViewTreeObserver.OnGlobalLayoutListener
    private lateinit var currentTab: Tab
    private lateinit var rootView: View
    private lateinit var fragmentParentLayout: CoordinatorLayout

    private val equipmentListViewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this)
    }

    private val equipmentCategoriesViewModel: EquipmentCategoriesViewModel by lazy {
        EquipmentCategoriesViewModel.instance(owner = this)
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearBackStack()

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

        navigation.setOnNavigationItemSelectedListener(navListener)

        if (savedInstanceState == null) {
            currentTab = Tab.Equipment

            navigationStack.show(Tab.Equipment, MyEquipmentsListFragment())
        } else {
            when (savedInstanceState.getInt(SELECTED_TAB, R.id.navigation_equipment)) {
                R.id.navigation_dealers -> {
                    currentTab = Tab.Dealers
                    navigation.selectedItemId = R.id.navigation_dealers
                }
                R.id.navigation_resources -> {
                    currentTab = Tab.Resources
                    navigation.selectedItemId = R.id.navigation_resources
                }
                R.id.navigation_profile -> {
                    currentTab = Tab.Profile
                    navigation.selectedItemId = R.id.navigation_profile
                }
                else -> {
                    currentTab = Tab.Equipment
                    navigation.selectedItemId = R.id.navigation_equipment
                }
            }
        }

        AppProxy.proxy.accountManager.isAuthenticated.observe(this, Observer {
            this.equipmentListViewModel.updateData(this)
        })

        this.equipmentListViewModel.updateData(this)
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

    override fun clearBackStack() {
        supportFragmentManager.beginTransaction().let { transaction ->
            supportFragmentManager.fragments.fold(transaction) { acc, next ->
                acc.remove(next)
            }
        }.commit()
    }

    override fun addFragmentToBackStack(fragment: Fragment) {
        navigationStack.show(getCurrentTab(), fragment)
    }

    override fun onBackPressed() {
        if (isKeyboardOpen()) {
            rootView.hideKeyboard()
        }

        navigation.setOnNavigationItemSelectedListener(null)
        val didGoBack = navigationStack.tryToGoBackOrFalse()
        navigationStack.currentTab()?.let {
            navigation.selectedItemId = when(it) {
                Tab.Equipment -> R.id.navigation_equipment
                Tab.Resources -> R.id.navigation_resources
                Tab.Dealers -> R.id.navigation_dealers
                Tab.Profile -> R.id.navigation_profile
            }
        }
        navigation.setOnNavigationItemSelectedListener(navListener)

        if (!didGoBack) {
            finish()
        }
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

}

enum class Tab {
    Equipment,
    Resources,
    Dealers,
    Profile,
}
