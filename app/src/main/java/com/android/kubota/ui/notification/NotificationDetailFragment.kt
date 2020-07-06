package com.android.kubota.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.kubota.R
import com.android.kubota.databinding.FragmentNotificationDetailBinding
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.kubota.service.domain.Notification
import com.kubota.service.domain.NotificationSource

private val KEY_NOTIFICATION = "notification"

class NotificationDetailFragment: Fragment() {

    private var b: FragmentNotificationDetailBinding? = null
    private val binding get() = b!!
    private val notification: Notification by lazy {
        val temp: Notification = requireArguments().getParcelable(KEY_NOTIFICATION)!!
        temp
    }
    private val viewModel: NotificationsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (notification.sourceFrom) {
            NotificationSource.ALERTS -> activity?.setTitle(R.string.alerts)
            NotificationSource.MESSAGES -> activity?.setTitle(R.string.messages)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_notification_detail,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.notification = notification

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null && !notification.isRead) {
            viewModel.markNotificationAsRead(notification)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }

    companion object {
        fun createInstance(notification: Notification): NotificationDetailFragment {
            return NotificationDetailFragment().apply {
                arguments = Bundle(1).apply { putParcelable(KEY_NOTIFICATION, notification) }
            }
        }
    }
}