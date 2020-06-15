package com.android.kubota.ui.equipment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.databinding.FragmentInhibitRestartBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.ui.SESSION_EXPIRED_DIALOG_TAG
import com.android.kubota.ui.SessionExpiredDialogFragment
import com.android.kubota.viewmodel.equipment.InhibitRestartViewModel
import com.android.kubota.viewmodel.equipment.InhibitRestartViewModelFactory
import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentUnit
import java.lang.ref.WeakReference
import java.util.*

private const val UUID_KEY = "uuidKey"

class InhibitStarterFragment: BaseBindingFragment<FragmentInhibitRestartBinding, InhibitRestartViewModel>() {

    //TODO: Remove this once we have something in the Equipment we can check.
    private var isStarterEnabled = true
    private lateinit var equipmentUnit: EquipmentUnit

    override val viewModel: InhibitRestartViewModel by lazy {
        ViewModelProvider(
            this,
            InhibitRestartViewModelFactory(
                WeakReference { this.signInAsync() },
                UUID.fromString(requireArguments().getString(UUID_KEY, ""))
            )
        ).get(InhibitRestartViewModel::class.java)
    }

    override val layoutResId: Int
        get() = R.layout.fragment_inhibit_restart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        binding.viewModel = viewModel

        return view
    }

    override fun loadData() {
        viewModel.isLoading.observe(this, androidx.lifecycle.Observer {
            binding.actionButton.isEnabled = !it
        })

        viewModel.isProcessing.observe(this, androidx.lifecycle.Observer {isProcessingRequest ->
            binding.actionButton.setOnClickListener {
                if (isProcessingRequest) {
                    viewModel.cancelRequest()
                } else {
                    val dialogFragment = if (isStarterEnabled) {
                        ConfirmationDialogFragment.createInstanceForDisabling(unitNickname = equipmentUnit.nickName ?: equipmentUnit.model)
                    } else {
                        ConfirmationDialogFragment.createInstanceForEnabling(unitNickname = equipmentUnit.nickName ?: equipmentUnit.model)
                    }
                    dialogFragment.show(childFragmentManager, ConfirmationDialogFragment.TAG)

                }
            }
        })

        viewModel.equipmentUnit.observe(this, androidx.lifecycle.Observer {
            equipmentUnit = it
        })
    }

    fun onContinueClicked() {
        isStarterEnabled = !isStarterEnabled
        viewModel.toggleStarterState()
    }

    private fun signInAsync(): Promise<Unit> {
        SessionExpiredDialogFragment().show(parentFragmentManager, SESSION_EXPIRED_DIALOG_TAG)

        // FIXME: Need to start the AcccountSetupActivity and wait for result
        return Promise.value(Unit)
    }

    companion object {
        fun createInstance(equipmentId: UUID): InhibitStarterFragment {
            return InhibitStarterFragment()
                .apply {
                    arguments = Bundle(1)
                        .apply { putString(UUID_KEY, equipmentId.toString()) }
                }
        }
    }
}

class ConfirmationDialogFragment: DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false

        val view = inflater.inflate(R.layout.dialog_inhibit_confirmation, container, false)
        requireArguments().let {
            val nickname = it.getString(UNIT_NICKNAME_KEY, "")
            val titleResId = it.getInt(TITLE_STRING_RES_KEY)
            val bodyResId = it.getInt(BODY_STRING_RES_KEY)

            view.findViewById<TextView>(R.id.titleTextView).text = getString(titleResId, nickname)
            view.findViewById<TextView>(R.id.bodyTextView).text = getString(bodyResId, nickname)
            view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                this.dismiss()
            }
            view.findViewById<Button>(R.id.continueButton).setOnClickListener {
                dismiss()
                (parentFragment as InhibitStarterFragment).onContinueClicked()
            }
        }

        return view
    }

    companion object {
        const val TAG = "ConfirmationDialogFragment"

        private val TITLE_STRING_RES_KEY = "titleStringRes"
        private val BODY_STRING_RES_KEY = "bodyStringRes"
        private val UNIT_NICKNAME_KEY = "nickname"

        fun createInstanceForEnabling(unitNickname: String): ConfirmationDialogFragment {
            return createInstance(
                unitNickname = unitNickname,
                titleResId = R.string.disable_restart,
                bodyStringResId = R.string.confirm_disabling
            )
        }

        fun createInstanceForDisabling(unitNickname: String): ConfirmationDialogFragment {
            return createInstance(
                unitNickname = unitNickname,
                titleResId = R.string.enable_restart,
                bodyStringResId = R.string.confirm_enabling
            )
        }

        private fun createInstance(
            unitNickname: String,
            @StringRes titleResId: Int,
            @StringRes bodyStringResId: Int
        ): ConfirmationDialogFragment {
            return ConfirmationDialogFragment().apply {
                arguments = Bundle(3).apply {
                    putString(UNIT_NICKNAME_KEY, unitNickname)
                    putInt(TITLE_STRING_RES_KEY, titleResId)
                    putInt(BODY_STRING_RES_KEY, bodyStringResId)
                }
            }
        }
    }
}