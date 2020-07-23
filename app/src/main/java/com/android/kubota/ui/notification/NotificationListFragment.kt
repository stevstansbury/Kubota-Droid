package com.android.kubota.ui.notification

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.databinding.FragmentNotificationListBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
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

    private val listAdapter = NotificationAdapter(mutableListOf()) {
        flowActivity?.addFragmentToBackStack(
            NotificationDetailFragment.createInstance(it)
        )
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
        binding.recyclerView.adapter = listAdapter
    }

    override fun loadData() {
        when(viewMode) {
            ALERTS_VIEW_MODE -> {
                viewModel.alerts.observe(this, Observer {
                    listAdapter.replace(it)
                    updateVisibility(it.isEmpty())
                })
                binding.noMessagesTextView.setText(R.string.no_alerts)
            }
            MESSAGES_VIEW_MODE -> {
                enableSwipeToDelete()
                viewModel.messages.observe(this, Observer {
                    listAdapter.replace(it)
                    updateVisibility(it.isEmpty())
                })
                binding.noMessagesTextView.setText(R.string.no_messages)
            }
        }
    }

    private fun updateVisibility(isListEmpty: Boolean) {
        binding.recyclerView.visibility = if (isListEmpty) View.GONE else View.VISIBLE
        binding.noMessagesTextView.visibility = if (isListEmpty) View.VISIBLE else View.GONE
    }

    private fun enableSwipeToDelete() {
        val actionDrawable = requireContext().getDrawable(R.drawable.ic_action_delete) as Drawable
        val swipeAction = SwipeAction(
            actionDrawable,
            ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color)
        )

        val callback = object : SwipeActionCallback(swipeAction, swipeAction) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p1: Int) {
                val position = viewHolder.adapterPosition
                viewModel.delete(delegate = authDelegate, message = listAdapter.getItem(position))
                listAdapter.removeItem(position)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
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