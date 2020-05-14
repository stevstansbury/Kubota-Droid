package com.android.kubota.ui.equipment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.equipment.FaultCodesAdapter.FaultCodeViewHolder
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.FaultCode
import java.util.*
import kotlin.collections.ArrayList


class FaultCodeResultsFragment : BaseEquipmentUnitFragment() {

    companion object {
        private const val FAULT_CODES_KEY = "EQUIPMENT_FAULT_CODES_KEY"

        fun createInstance(equipmentId: UUID, codes: ArrayList<Int>): FaultCodeResultsFragment {
            return FaultCodeResultsFragment().apply {
                val bundle = getBundle(equipmentId)
                bundle.putIntegerArrayList(FAULT_CODES_KEY, codes)
                arguments = bundle
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code_results

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var faultCodeResults: RecyclerView

    private var faultCodes = emptyList<Int>()

    override fun hasRequiredArgumentData(): Boolean {
        if (super.hasRequiredArgumentData().not()) return false
        faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY) ?: emptyList()
        return true
    }

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

    override fun onDataLoaded(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        equipmentNicknameTextView.text = display.nickname
        modelImageView.setImageResource(display.imageResId)
        modelTextView.text = display.modelName
        serialNumberTextView.text = display.serialNumber

        this.showProgressBar()
        AppProxy.proxy.serviceManager.equipmentService.getFaultCodes(model = unit.model, codes = this.faultCodes.map { "$it" })
            .done { faultCodeResults.adapter = FaultCodesAdapter(it) }
            .ensure { this.hideProgressBar() }
            .catch {
                when (it) {
                    is KubotaServiceError.NotFound ->
                        this.showError(getString(R.string.fault_code_not_found))
                    else ->
                        this.showError(it)
                }
                parentFragmentManager.popBackStack()
            }
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