package com.android.kubota.ui.equipment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.kubota.service.domain.EquipmentUnit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.state.AddEquipmentScanState
import com.android.kubota.databinding.ViewItemFaultCodeSingleLineBinding
import com.android.kubota.ui.BaseFragment
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.FaultCode

class FaultCodeInquiryFragment: BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_fault_code_inquiry

    private lateinit var activeFaultCodes: RecyclerView
    private lateinit var activeFaultCodesGroup: View
    private lateinit var submitButton: Button
    private lateinit var listHeader: TextView
    private lateinit var faultCodeEditText: EditText

    private var equipmentUnit: EquipmentUnit? = null
    private var equipmentModel: EquipmentModel? = null

    private val modelName: String
        get() {
            return this.equipmentUnit?.model ?: this.equipmentModel?.model ?: ""
        }

    companion object {
        private const val EQUIPMENT_UNIT_KEY = "EQUIPMENT_UNIT_KEY"
        private const val EQUIPMENT_MODEL_KEY = "EQUIPMENT_MODEL_KEY"

        fun createInstance(equipmentUnit: EquipmentUnit): FaultCodeInquiryFragment {
            return FaultCodeInquiryFragment().apply {
                val data = Bundle(1)
                data.put(EQUIPMENT_UNIT_KEY, equipmentUnit)
                arguments = data
            }
        }

        fun createInstance(equipmentModel :EquipmentModel): FaultCodeInquiryFragment {
            return FaultCodeInquiryFragment().apply {
                val data = Bundle(1)
                data.put(EQUIPMENT_MODEL_KEY, equipmentModel)
                arguments = data
            }
        }
    }

    @CallSuper
    override fun hasRequiredArgumentData(): Boolean {
        this.equipmentUnit = arguments?.getT(EQUIPMENT_UNIT_KEY)
        this.equipmentModel = arguments?.getT(EQUIPMENT_MODEL_KEY)

        return this.equipmentUnit != null || this.equipmentModel != null
    }

    override fun initUi(view: View) {
        listHeader = view.findViewById(R.id.listHeader)
        activeFaultCodes = view.findViewById(R.id.recyclerView)
        activeFaultCodes.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        activeFaultCodesGroup = view.findViewById(R.id.group1)
        submitButton = view.findViewById(R.id.lookupButton)
        submitButton.setOnClickListener {
            this.lookupFaultCode()
        }

        faultCodeEditText = view.findViewById(R.id.faultCodeEditText)
        faultCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                submitButton.isEnabled = s != null &&
                        s.toString().isNotBlank() &&
                        s.toString().isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateUISubmitInquiry() {
        submitButton.setOnClickListener(null)
        submitButton.hideKeyboard()
        faultCodeEditText.isEnabled = false
    }

    @CallSuper
    override fun loadData() {
        if (this.equipmentUnit != null) {
            this.onBindData(unit = this.equipmentUnit!!)
        } else if (this.equipmentModel != null) {
            this.onBindData(model = this.equipmentModel!!)
        }
    }

    private fun onBindData(unit: EquipmentUnit) {
        activity?.title = getString(
            R.string.fault_code_screen_title,
            if (unit.nickName.isNullOrBlank()) unit.model else unit.nickName
        )

        activeFaultCodesGroup.visibility = if (unit.telematics?.faultCodes.isNullOrEmpty()) View.GONE else View.VISIBLE

        activeFaultCodes.adapter = FaultCodeAdapter(unit.telematics?.faultCodes ?: emptyList()) { code ->
            this.equipmentUnit?.let { unit ->
                val faultCode = unit.telematics?.faultCodes?.firstOrNull { it.code == code }
                faultCode?.let {
                    flowActivity?.addFragmentToBackStack(
                        FaultCodeResultsFragment.createInstance(it)
                    )
                }
            }
        }
    }

    private fun onBindData(model: EquipmentModel) {
        activity?.title = getString(
            R.string.fault_code_screen_title,
            model.model
        )
        activeFaultCodesGroup.visibility = View.GONE
    }

    private fun lookupFaultCode() {
        this.showBlockingActivityIndicator()
        val faultCode = faultCodeEditText.text.toString()
        AppProxy.proxy.serviceManager.equipmentService
                .getFaultCodes(model = this.modelName, codes = listOf(faultCode))
                .done {
                    flowActivity?.addFragmentToBackStack(
                        FaultCodeResultsFragment.createInstance(it.first())
                    )
                }
                .ensure {
                    this.hideBlockingActivityIndicator()
                }
                .catch {
                    val errorStringResID = when (it) {
                        is KubotaServiceError.NotFound -> R.string.fault_code_not_found
                        is KubotaServiceError.NetworkConnectionLost,
                        is KubotaServiceError.NotConnectedToInternet ->
                            R.string.connectivity_error_message
                        else -> R.string.server_error_message
                    }
                    MessageDialogFragment
                        .showSimpleMessage(
                            this.parentFragmentManager,
                            titleId = R.string.title_error,
                            messageId = errorStringResID
                        )
                }
    }

}

private class FaultCodeAdapter(
    val data: List<FaultCode>,
    val onClickListener: (faultCode: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ViewItemFaultCodeSingleLineBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.view_item_fault_code_single_line,
            parent,
            false
        )

        binding.root.tag = binding
        return BindingHolder(binding.root)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding: ViewDataBinding =
            holder.itemView.tag as ViewItemFaultCodeSingleLineBinding

        val faultCode = data[position]
        val description = holder.itemView.context.getString(
                        R.string.fault_code_description_single_line_fmt,
                        faultCode.code,
                        faultCode.description
                    )

        binding.setVariable(BR.faultCodeDescription, description)
        binding.root.findViewById<ConstraintLayout>(R.id.faultCodeItem).setOnClickListener {
            this.onClickListener(faultCode.code)
        }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}