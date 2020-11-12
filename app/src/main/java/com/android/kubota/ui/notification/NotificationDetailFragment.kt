package com.android.kubota.ui.notification

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.kubota.R
import com.android.kubota.databinding.FragmentNotificationDetailBinding
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.kubota.service.domain.InboxMessage
import com.kubota.service.domain.InboxMessageSource

private val KEY_NOTIFICATION = "notification"

class NotificationDetailFragment: Fragment() {

    private var b: FragmentNotificationDetailBinding? = null
    private val binding get() = b!!
    private val notification: InboxMessage by lazy {
        val temp: InboxMessage = requireArguments().getParcelable(KEY_NOTIFICATION)!!
        temp
    }
    private val viewModel: NotificationsViewModel by activityViewModels()

    private val authDelegate: AuthDelegate
        get() {
            return this.requireActivity() as AuthDelegate
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (this.requireActivity() !is AuthDelegate) {
            throw IllegalStateException("Fragment is not attached to an AuthDelegate Activity.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (notification.sourceFrom) {
            InboxMessageSource.ALERTS -> activity?.setTitle(R.string.alert)
            InboxMessageSource.MESSAGES -> activity?.setTitle(R.string.message)
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
            viewModel.markAsRead(authDelegate, notification)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }

    companion object {
        fun createInstance(notification: InboxMessage): NotificationDetailFragment {
            return NotificationDetailFragment().apply {
                arguments = Bundle(1).apply { putParcelable(KEY_NOTIFICATION, notification) }
            }
        }
    }
}