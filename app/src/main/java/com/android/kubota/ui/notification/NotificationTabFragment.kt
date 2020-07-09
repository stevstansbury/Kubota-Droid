package com.android.kubota.ui.notification

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.kubota.R
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NotificationTabFragment: AuthBaseFragment() {
    override val layoutResId: Int = R.layout.fragment_notification_tab

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun initUi(view: View) {
        activity?.setTitle(R.string.notifications)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.pager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = NotificationTabAdapter(
            childFragmentManager,
            lifecycle
        )
        val alertTitle = getString(R.string.alerts)
        val messagesTitle = getString(R.string.messages)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> alertTitle
                else -> messagesTitle
            }
        }.attach()
    }

    override fun loadData() {
        viewModel.updateData(this.authDelegate)
    }

}

class NotificationTabAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> NotificationListFragment.createInstanceForAlerts()
            else -> NotificationListFragment.createInstanceForMessages()
        }
    }
}