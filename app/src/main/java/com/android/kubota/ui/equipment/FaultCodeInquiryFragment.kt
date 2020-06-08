package com.android.kubota.ui.equipment

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.kubota.service.domain.EquipmentUnit
import java.util.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.databinding.ViewItemFaultCodeSingleLineBinding
import com.kubota.service.domain.FaultCode

class FaultCodeInquiryFragment: BaseEquipmentUnitFragment() {
    override val layoutResId: Int = R.layout.fragment_fault_code_inquiry

    private lateinit var activeFaultCodes: RecyclerView
    private lateinit var activeFaultCodesGroup: View
    private lateinit var submitButton: Button
    private lateinit var faultCodeEditText: EditText
    private var faultCode: Int? = null

    companion object {
        fun createInstance(equipmentId: UUID): FaultCodeInquiryFragment {
            return FaultCodeInquiryFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override fun initUi(view: View) {
        activeFaultCodes = view.findViewById(R.id.recyclerView)
        activeFaultCodes.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        activeFaultCodesGroup = view.findViewById(R.id.group1)
        submitButton = view.findViewById(R.id.lookupButton)
        faultCodeEditText = view.findViewById(R.id.faultCodeEditText)

        faultCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s == null || s.toString().isBlank() || s.toString().isEmpty()) {
                    submitButton.isEnabled = false
                    return
                }

                val newFaultCode = s.toString().toInt()
                if (newFaultCode != faultCode) {
                    faultCode = newFaultCode
                    submitButton.isEnabled = true
                } else {
                    submitButton.isEnabled = false
                }
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

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let { this.onBindData(it) }
        })

        this.viewModel.equipmentUnitFaultCodes.observe(viewLifecycleOwner, Observer {
            activeFaultCodesGroup.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE

            activeFaultCodes.adapter = FaultCodeAdapter(it ?: emptyList()) { code ->
                this.viewModel.equipmentUnit.value?.let { unit ->
                    this.flowActivity?.addFragmentToBackStack(
                        FaultCodeResultsFragment.createInstance(unit.model, code)
                    )
                }
            }
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        activity?.title = getString(
            R.string.fault_code_screen_title,
            if (unit.nickName.isNullOrBlank()) unit.model else unit.nickName
        )

        submitButton.setOnClickListener {
            faultCode?.let {code->
                updateUISubmitInquiry()
                flowActivity?.addFragmentToBackStack(
                    FaultCodeResultsFragment.createInstance(unit.model, code)
                )
            }
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