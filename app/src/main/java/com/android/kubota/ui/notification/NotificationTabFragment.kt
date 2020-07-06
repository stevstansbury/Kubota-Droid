package com.android.kubota.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.kubota.R
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NotificationTabFragment: Fragment() {
    private val viewModel: NotificationsViewModel by activityViewModels()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.notifications)
        return inflater.inflate(R.layout.fragment_notification_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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