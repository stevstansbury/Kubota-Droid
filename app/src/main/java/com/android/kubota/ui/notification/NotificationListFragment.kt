package com.android.kubota.ui.notification

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.kubota.R
import com.android.kubota.databinding.FragmentNotificationListBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.viewmodel.notification.NotificationsViewModel

private val ALERTS_VIEW_MODE = 0
private val MESSAGES_VIEW_MODE = 1
private val KEY_VIEW_MODE = "viewMode"

class NotificationListFragment: BaseBindingFragment<FragmentNotificationListBinding, NotificationsViewModel>() {
    override val viewModel: NotificationsViewModel by activityViewModels()
    override val layoutResId = R.layout.fragment_notification_list

    private val viewMode: Int by lazy {
        requireArguments().getInt(KEY_VIEW_MODE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    override fun loadData() {
        when(viewMode) {
            ALERTS_VIEW_MODE -> {
                viewModel.alerts.observe(this, Observer {
                    binding.recyclerView.adapter = NotificationAdapter(it) {
                        flowActivity?.addFragmentToBackStack(
                            NotificationDetailFragment.createInstance(it)
                        )
                    }
                })
            }
            MESSAGES_VIEW_MODE -> {
                viewModel.messages.observe(this, Observer {
                    binding.recyclerView.adapter = NotificationAdapter(it) {
                        flowActivity?.addFragmentToBackStack(
                            NotificationDetailFragment.createInstance(it)
                        )
                    }
                })
            }
        }
    }

    companion object {
        fun createInstanceForAlerts() = createInstance(ALERTS_VIEW_MODE)

        fun createInstanceForMessages() = createInstance(MESSAGES_VIEW_MODE)

        private fun createInstance(viewMode: Int): NotificationListFragment {
            return NotificationListFragment().apply {
                arguments = Bundle(1).apply { putInt(KEY_VIEW_MODE, viewMode) }
            }
        }
    }
}