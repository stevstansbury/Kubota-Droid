package com.android.kubota.ui.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.databinding.FragmentNotificationDetailBinding
import com.android.kubota.ui.FlowActivity
import com.android.kubota.ui.MaintenanceIntervalFragment
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import com.android.kubota.ui.equipment.FaultCodeFragment
import com.android.kubota.ui.equipment.FaultCodeResultsFragment
import com.android.kubota.ui.equipment.TelematicsFragment
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.notification.NotificationsViewModel
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.InboxMessage
import com.kubota.service.domain.InboxMessageSource
import kotlinx.android.synthetic.main.fragment_notification_detail.*
import java.util.*

private val KEY_NOTIFICATION = "notification"

class NotificationDetailFragment : Fragment() {

    private var b: FragmentNotificationDetailBinding? = null
    private val binding get() = b!!
    private val notification: InboxMessage by lazy {
        requireArguments().getParcelable(KEY_NOTIFICATION)!!
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

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            when (notification.sourceFrom) {
                InboxMessageSource.ALERTS -> activity?.setTitle(R.string.alert)
                InboxMessageSource.MESSAGES -> activity?.setTitle(R.string.message)
            }
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

        if (notification.deepLink.isEmpty()) {
            deepLinkText.visibility = View.GONE
        } else {
            deepLinkText.text = getLinkText(notification.deepLink["alertId"])
        }

        deepLinkText.setOnClickListener {
            showDeepLinkData(notification.deepLink)
        }

        if (notification.deepLink.containsKey("exposedEquipmentId")) {
            val equipmentId = UUID.fromString(notification.deepLink["exposedEquipmentId"])
            AppProxy.proxy.serviceManager.userPreferenceService
                .getEquipmentUnit(equipmentId)
                .map { equipment ->
                    equipment?.let {
                        binding.tvModel.isVisible = true
                        binding.tvModel.text =
                            requireContext().getString(R.string.alerts_model, it.model)

                        if (it.pin != null && it.pin!!.isNotEmpty()) {
                            binding.tvPinSerial.isVisible = true
                            binding.tvPinSerial.text =
                                requireContext().getString(R.string.alerts_pin, it.pin)
                        } else if (it.serial != null && it.serial!!.isNotEmpty()) {
                            binding.tvPinSerial.isVisible = true
                            binding.tvPinSerial.text =
                                requireContext().getString(R.string.alerts_serial, it.serial)
                        }
                    }
                }
        }

        binding.body.movementMethod = LinkMovementMethod.getInstance()
        binding.body.text = notification.body.parseNotifications { link, placeholder ->
            inSpans(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        startActivity(intent)
                    }
                },
                ForegroundColorSpan(requireContext().getColor(R.color.equipment_tree_filters_close))
            ) {
                append(placeholder)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }

    private fun showDeepLinkData(deepLink: Map<String, String>) {
        val equipmentId = UUID.fromString(deepLink["exposedEquipmentId"])
        val getEquipmentUnit = {
            AppProxy.proxy.serviceManager.userPreferenceService
                .getEquipmentUnit(equipmentId)
                .map { it!! }
        }
        when (deepLink["alertId"]) {
            "BAT-L-LOW",
            "COOL-T-HIGH",
            "DEF-L-LOW",
            "HYDR-T-HIGH",
            "FUEL-L-LOW" -> Promise.value(TelematicsFragment.createInstance(equipmentId))
            "FAULT-CODE" -> getEquipmentUnit().map {
                when (it.telematics?.faultCodes?.size) {
                    1 -> FaultCodeResultsFragment.createInstance(it.telematics!!.faultCodes.first())
                    else -> FaultCodeFragment.createInstance(it)
                }
            }
            "MAINTENANCE" -> getEquipmentUnit().map { MaintenanceIntervalFragment.createInstance(it.model) }
            "TRANSPORT", "GEOFENCE-OUT" -> getEquipmentUnit().map {
                GeofenceFragment.createInstance(
                    it.telematics?.location
                )
            }
            "WARRANTY" -> getEquipmentUnit().map { EquipmentDetailFragment.createInstance(it) }
            else -> activity?.onBackPressed().let { null }
        }?.map { fragment -> (activity as? FlowActivity)?.addFragmentToBackStack(fragment) }
    }

    private fun getLinkText(alertId: String?): String? {
        return context?.getString(
            when (alertId) {
                "BAT-L-LOW" -> R.string.view_battery_level
                "COOL-T-HIGH" -> R.string.view_coolant_temp
                "DEF-L-LOW" -> R.string.view_def_level
                "HYDR-T-HIGH" -> R.string.view_hydraulic_temp
                "FUEL-L-LOW" -> R.string.view_fuel_level
                "FAULT-CODE" -> R.string.view_fault_code
                "MAINTENANCE" -> R.string.view_maintenance_schedule
                "TRANSPORT", "GEOFENCE-OUT" -> R.string.view_location
                "WARRANTY" -> R.string.view_warranty_info
                else -> return null
            }
        )
    }

    companion object {
        fun createInstance(notification: InboxMessage): NotificationDetailFragment {
            return NotificationDetailFragment().apply {
                arguments = Bundle(1).apply { putParcelable(KEY_NOTIFICATION, notification) }
            }
        }
    }
}

fun String.parseNotifications(spanBuilder: SpannableStringBuilder.(link: String, placeholder: String) -> SpannableStringBuilder): SpannedString {
    var text = this
    return buildSpannedString {
        while (text.contains("](http")) {
            val startOfPlaceholder = text.indexOf("[")
            val endOfPlaceholder = text.indexOf("]")

            val placeholder = text.substring(startOfPlaceholder + 1, endOfPlaceholder)

            val startOfLink = text.indexOf("(")
            val endOfLink = text.indexOf(")")

            val link = text.substring(startOfLink + 1, endOfLink)
            append(text.substring(0, startOfPlaceholder))

            spanBuilder(link, placeholder)

            text = text.substring(endOfLink + 1, text.length)
        }

        append(text.substring(0, text.length))
    }
}