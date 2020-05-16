package com.android.kubota.ui.equipment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.ui.equipment.FaultCodesAdapter.FaultCodeViewHolder
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.FaultCode
import androidx.lifecycle.Observer
import java.util.*
import kotlin.collections.ArrayList


class FaultCodeResultsFragment : BaseEquipmentUnitFragment() {

    companion object {
        fun createInstance(equipmentId: UUID, codes: ArrayList<Int>): FaultCodeResultsFragment {
            return FaultCodeResultsFragment().apply {
                arguments = getBundle(equipmentId, codes)
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code_results

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var faultCodeResults: RecyclerView

    override fun initUi(view: View) {
        activity?.setTitle(R.string.fault_code_inquiry)
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        faultCodeResults = view.findViewById(R.id.faultCodeResults)

        faultCodeResults.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        faultCodeResults.setHasFixedSize(true)
        faultCodeResults.isNestedScrollingEnabled = false;
    }

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let { this.onBindData(it) }
        })

        this.viewModel.equipmentUnitFaultCodes.observe(viewLifecycleOwner, Observer { faultCodes ->
            faultCodes?.let {
                this.faultCodeResults.adapter = FaultCodesAdapter(it)
            }
        })
    }

    override fun showError(error: Throwable) {
        when (error) {
            is KubotaServiceError.NotFound ->
                super.showError(getString(R.string.fault_code_not_found))
            else ->
                super.showError(error)
        }
        parentFragmentManager.popBackStack()
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        equipmentNicknameTextView.text = display.nickname
        modelImageView.setImageResource(display.imageResId)
        modelTextView.text = display.modelName
        serialNumberTextView.text = display.serialNumber
    }
}

class FaultCodesAdapter(private val codes: List<FaultCode>) : RecyclerView.Adapter<FaultCodeViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaultCodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_fault_code_list_item, parent, false)
        return FaultCodeViewHolder(view)
    }

    override fun getItemCount(): Int {
        return codes.size
    }

    override fun onBindViewHolder(holder: FaultCodeViewHolder, position: Int) {
        val faultCode = codes[position]
        holder.bindItem(faultCode)
    }

    class FaultCodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val faultCodeItemDescription: TextView = itemView.findViewById(R.id.faultCodeItemDescription)
        private val faultCodeItemDescriptionMessage: TextView = itemView.findViewById(R.id.faultCodeItemDescriptionMessage)
        private val faultCodeItemAction: TextView = itemView.findViewById(R.id.faultCodeItemAction)
        private val faultCodeItemActionMessage: TextView = itemView.findViewById(R.id.faultCodeItemActionMessage)

        fun bindItem(faultCode: FaultCode) {
            val action = faultCode.provisionalMeasure
            if(action != null){
                faultCodeItemAction.visibility = View.VISIBLE
                faultCodeItemActionMessage.visibility = View.VISIBLE
                faultCodeItemActionMessage.text = action
            }
            else{
                faultCodeItemAction.visibility = View.GONE
                faultCodeItemActionMessage.visibility = View.GONE
            }

            faultCodeItemDescriptionMessage.text = faultCode.description
        }
    }
}