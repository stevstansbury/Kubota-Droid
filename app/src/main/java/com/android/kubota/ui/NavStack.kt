package com.android.kubota.ui

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.android.kubota.R
import com.android.kubota.ui.dealer.DealersFragment
import com.android.kubota.ui.equipment.MyEquipmentsListFragment
import com.android.kubota.ui.resources.CategoriesFragment
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FragmentData(
    val fragmentName: String,
    val tag: String,
    var savedState: Fragment.SavedState?,
    var shownAt: MutableList<Long>,
    val initialArgs: Bundle?
) : Parcelable {
    fun newInstance(): Fragment = Class.forName(fragmentName).newInstance() as Fragment
}


class NavStack(
    private val supportFragmentManager: FragmentManager,
    private val toolbarController: MainToolbarController
) {

    private val equipment = mutableListOf<FragmentData>()
    private val resource = mutableListOf<FragmentData>()
    private val dealer = mutableListOf<FragmentData>()
    private val profile = mutableListOf<FragmentData>()

    private fun tabOrdered(): List<FragmentData> {
        return listOf(equipment, resource, dealer, profile).flatten().sortedBy { it.shownAt.last() }
    }

    fun getSavedState(): Bundle {
        val currentlyVisible = tabOrdered().lastOrNull()
        val currentScreenState = supportFragmentManager
            .findFragmentByTag(currentlyVisible?.tag)
            ?.let { supportFragmentManager.saveFragmentInstanceState(it) }

        currentlyVisible?.savedState = currentScreenState

        return Bundle().apply {
            putParcelableArrayList(Tab.Equipment.name, ArrayList(equipment))
            putParcelableArrayList(Tab.Resources.name, ArrayList(resource))
            putParcelableArrayList(Tab.Dealers.name, ArrayList(dealer))
            putParcelableArrayList(Tab.Profile.name, ArrayList(profile))
        }
    }

    fun resumeSavedState(bundle: Bundle) {
        equipment.addAll(bundle.getParcelableArrayList(Tab.Equipment.name)!!)
        resource.addAll(bundle.getParcelableArrayList(Tab.Resources.name)!!)
        dealer.addAll(bundle.getParcelableArrayList(Tab.Dealers.name)!!)
        profile.addAll(bundle.getParcelableArrayList(Tab.Profile.name)!!)
        visibleTab()?.let { showTab(it, false) }
    }

    fun addToBackStack(fragment: Fragment, tab: Tab) {
        val currentlyVisible = tabOrdered().lastOrNull()

        val newFragmentData = fragment.toFragmentData()
        when (tab) {
            Tab.Equipment -> equipment.add(newFragmentData)
            Tab.Resources -> resource.add(newFragmentData)
            Tab.Dealers -> dealer.add(newFragmentData)
            Tab.Profile -> profile.add(newFragmentData)
        }

        render(currentlyVisible, newFragmentData, fragment, true)
    }

    fun showTab(tab: Tab, overWrite: Boolean) {
        val currentlyVisible = tabOrdered().lastOrNull()

        fun FragmentData.isRootFragment(): Boolean {
            return listOf(
                MyEquipmentsListFragment::class.java.canonicalName!!,
                CategoriesFragment::class.java.canonicalName!!,
                DealersFragment::class.java.canonicalName!!,
                ProfileFragment::class.java.canonicalName!!
            ).contains(this.fragmentName)
        }

        fun newRootFragmentInstance(): Fragment {
            return when (tab) {
                Tab.Equipment -> MyEquipmentsListFragment()
                Tab.Resources -> CategoriesFragment()
                Tab.Dealers -> DealersFragment()
                Tab.Profile -> ProfileFragment()
            }
        }

        when (visibleTab() == tab) {
            true -> {
                val isRoot = currentlyVisible?.isRootFragment() == true
                if (!overWrite || isRoot) {
                    when(isRoot) {
                        true -> toolbarController.showRootToolbar(tab)
                        false -> toolbarController.showSubScreenToolbar()
                    }
                    return
                }

                val newRootInstance = newRootFragmentInstance()
                val new = when (tab) {
                    Tab.Equipment -> newRootInstance.toFragmentData().also { equipment.add(it) }
                    Tab.Resources -> newRootInstance.toFragmentData().also { resource.add(it) }
                    Tab.Dealers -> newRootInstance.toFragmentData().also { dealer.add(it) }
                    Tab.Profile -> newRootInstance.toFragmentData().also { profile.add(it) }
                }

                render(currentlyVisible, new, newRootInstance, true)
            }
            false -> {
                var instanceIfMade: Fragment? = null

                fun MutableList<FragmentData>.handleNewRootInstance(): FragmentData {
                    return newRootFragmentInstance()
                        .also { instanceIfMade = it }
                        .toFragmentData()
                        .also { this.add(it) }
                }

                val older = when (tab) {
                    Tab.Equipment -> equipment.lastOrNull() ?: equipment.handleNewRootInstance()
                    Tab.Resources -> resource.lastOrNull() ?: resource.handleNewRootInstance()
                    Tab.Dealers -> dealer.lastOrNull() ?: dealer.handleNewRootInstance()
                    Tab.Profile -> profile.lastOrNull() ?: profile.handleNewRootInstance()
                }

                render(currentlyVisible, older, instanceIfMade, true)
            }
        }
    }

    fun goBack(): Boolean {
        val sorted = tabOrdered()
        val currentlyVisible = sorted.lastOrNull()
        sorted.getOrNull(sorted.size - 2) ?: return false

        fun isSameByTime(): Boolean {
            return listOf(equipment, resource, dealer, profile)
                .flatten()
                .flatMap { sup ->  sup.shownAt.map { sup.tag to it } }
                .sortedBy { it.second }
                .asReversed()
                .take(2)
                .let { it.first().first == it.last().first }
        }

        fun MutableList<FragmentData>.removeTimeAndRemoveAllIfNoTimes() {
            val isSameByTime = isSameByTime()
            val shownAt = this.last().shownAt.apply { removeLast() }
            if (shownAt.isEmpty() || isSameByTime) {
                this.removeLast()
            }
        }

        when {
            equipment.contains(currentlyVisible) -> equipment.removeTimeAndRemoveAllIfNoTimes()
            resource.contains(currentlyVisible) -> resource.removeTimeAndRemoveAllIfNoTimes()
            dealer.contains(currentlyVisible) -> dealer.removeTimeAndRemoveAllIfNoTimes()
            profile.contains(currentlyVisible) -> profile.removeTimeAndRemoveAllIfNoTimes()
        }

        render(currentlyVisible, tabOrdered().last(), null, false)

        return true
    }

    fun goUp() {
        val (currentlyVisible, upFragment) = when (visibleTab()) {
            Tab.Equipment -> equipment.removeLast() to equipment.last()
            Tab.Resources -> resource.removeLast() to resource.last()
            Tab.Dealers -> dealer.removeLast() to dealer.last()
            Tab.Profile -> profile.removeLast() to profile.last()
            else -> return
        }

        render(currentlyVisible, upFragment, null, true)
    }

    private fun render(
        visibleFragmentData: FragmentData?,
        newFragmentData: FragmentData,
        newFragmentInstance: Fragment?,
        updateShownAt: Boolean
    ) {
        val newFrag: Fragment = newFragmentInstance ?: newFragmentData.newInstance().apply {
            this.arguments = newFragmentData.initialArgs
            setInitialSavedState(newFragmentData.savedState)
        }

        if (updateShownAt) {
            newFragmentData.shownAt.add(System.currentTimeMillis())
        }

        visibleFragmentData?.also {
            val visibleFragment = supportFragmentManager
                .findFragmentByTag(it.tag)
                ?: return@also

            it.savedState = supportFragmentManager.saveFragmentInstanceState(visibleFragment)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, newFrag, newFragmentData.tag)
            .commit()

        when (newFragmentData.fragmentName) {
            MyEquipmentsListFragment::class.java.canonicalName!! -> toolbarController.showRootToolbar(Tab.Equipment)
            CategoriesFragment::class.java.canonicalName!! -> toolbarController.showRootToolbar(Tab.Resources)
            DealersFragment::class.java.canonicalName!! -> toolbarController.showRootToolbar(Tab.Dealers)
            ProfileFragment::class.java.canonicalName!! -> toolbarController.showRootToolbar(Tab.Profile)
            else -> toolbarController.showSubScreenToolbar()
        }
    }

    private fun Fragment.toFragmentData(): FragmentData {
        return FragmentData(
            fragmentName = this::class.java.canonicalName!!,
            tag = UUID.randomUUID().toString(),
            savedState = null,
            shownAt = mutableListOf(),
            initialArgs = this.arguments
        )
    }

    fun visibleTab(): Tab? {
        return when (tabOrdered().lastOrNull()) {
            in equipment -> Tab.Equipment
            in resource -> Tab.Resources
            in dealer -> Tab.Dealers
            in profile -> Tab.Profile
            else -> null
        }
    }

    // for handling locale change
    fun clearResourceStack() {
        val showResources = visibleTab() == Tab.Resources

        val transaction = supportFragmentManager.beginTransaction()
        resource
            .mapNotNull { supportFragmentManager.findFragmentByTag(it.tag) }
            .forEach { transaction.remove(it) }

        transaction.commit()
        resource.clear()

        if (showResources) {
            showTab(Tab.Resources, false)
        }
    }
}

fun Activity.popCurrentTabStack() {
    when (this is TabbedActivity) {
        true -> this.popCurrentTabStack()
        else -> this.onBackPressed()
    }
}
