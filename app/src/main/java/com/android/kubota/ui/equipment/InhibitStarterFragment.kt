package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.databinding.FragmentInhibitRestartBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.viewmodel.equipment.InhibitRestartViewModel
import com.android.kubota.viewmodel.equipment.InhibitRestartViewModelFactory
import com.android.kubota.viewmodel.equipment.STATE
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit

private const val EQUIPMENT_KEY = "equipment_unit"

class InhibitStarterFragment: BaseBindingFragment<FragmentInhibitRestartBinding, InhibitRestartViewModel>() {

    private lateinit var equipmentNickname: String

    override val viewModel: InhibitRestartViewModel by lazy {
        ViewModelProvider(
            this,
            InhibitRestartViewModelFactory(
                requireArguments().getParcelable(EQUIPMENT_KEY)!!
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
        viewModel.isLoading.observe(this, Observer {
            binding.actionButton.isEnabled = !it
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        })

        viewModel.currentState.observe(this, Observer { state ->
            binding.actionButton.setOnClickListener {
                when (state) {
                    STATE.STARTER_ENABLED -> ConfirmationDialogFragment
                        .createInstanceForDisabling(unitNickname = equipmentNickname)
                        .show(childFragmentManager, ConfirmationDialogFragment.TAG)
                    STATE.STARTER_DISABLED -> ConfirmationDialogFragment
                        .createInstanceForEnabling(unitNickname = equipmentNickname)
                        .show(childFragmentManager, ConfirmationDialogFragment.TAG)
                    else -> viewModel.cancelRequest(authDelegate)
                }
            }
        })

        viewModel.equipmentNickname.observe(this, Observer {
            equipmentNickname = it
            activity?.title = equipmentNickname
        })

        viewModel.error.observe(this, Observer { showError(it) })
    }

    override fun onResume() {
        super.onResume()
        this.viewModel.polling = true
    }

    override fun onPause() {
        super.onPause()
        this.viewModel.polling = false
    }

    fun onContinueClicked() {
        viewModel.toggleStarterState(this.authDelegate)
    }

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): InhibitStarterFragment {
            return InhibitStarterFragment()
                .apply {
                    arguments = Bundle(1)
                        .apply { putParcelable(EQUIPMENT_KEY, equipmentUnit) }
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
                titleResId = R.string.enable_restart,
                bodyStringResId = R.string.confirm_enabling
            )
        }

        fun createInstanceForDisabling(unitNickname: String): ConfirmationDialogFragment {
            return createInstance(
                unitNickname = unitNickname,
                titleResId = R.string.disable_restart,
                bodyStringResId = R.string.confirm_disabling
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