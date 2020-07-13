package com.android.kubota.ui.notification

import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.kubota.R
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.ui.KubotaBadgeDrawables
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NotificationTabFragment: AuthBaseFragment() {
    override val layoutResId: Int = R.layout.fragment_notification_tab

    private val viewModel: NotificationsViewModel by activityViewModels()
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private lateinit var alertsTab: TabLayout.Tab
    private lateinit var messagesTab: TabLayout.Tab

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
            when(position) {
                0 -> {
                    alertsTab = tab
                    tab.text = alertTitle
                    tab.setCustomView(R.layout.view_notifications_tab)
                }
                else -> {
                    messagesTab = tab
                    tab.text = messagesTitle
                    tab.setCustomView(R.layout.view_notifications_tab)
                }
            }
        }.attach()
    }

    override fun loadData() {
        viewModel.updateData(this.authDelegate)
        val unreadAlertsCounter = KubotaBadgeDrawables(requireContext()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.notification_tab_unread_counter_color)
            setTextColor(R.color.notification_tab_unread_counter_text_color)
            setTextSize(R.dimen.notification_tab_unread_counter_text_size)
        }
        val unreadMessagesCounter = KubotaBadgeDrawables(requireContext()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.notification_tab_unread_counter_color)
            setTextColor(R.color.notification_tab_unread_counter_text_color)
            setTextSize(R.dimen.notification_tab_unread_counter_text_size)
        }

        viewModel.alerts.observe(this, Observer {
            unreadAlertsCounter.unreadCounter = it.filter{ !it.isRead }.size
            alertsTab.icon = unreadAlertsCounter
        })

        viewModel.messages.observe(this, Observer {
            unreadMessagesCounter.unreadCounter = it.filter{ !it.isRead }.size
            messagesTab.icon = unreadMessagesCounter
        })
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