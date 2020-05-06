package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.FaultCodeInquiryViewModel
import com.kubota.repository.service.FaultCodeItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.extensions.showServerErrorSnackBar
import com.android.kubota.ui.FaultCodesAdapter.FaultCodeViewHolder
import com.android.kubota.viewmodel.UIEquipment
import com.kubota.repository.service.FaultCodeResponse

private const val EQUIPMENT_KEY = "equipment"
private const val FAULT_CODES_KEY = "fault_codes_key"

class FaultCodeResultsFragment : BaseFragment() {

    companion object {
        fun createInstance(equipmentId: Int, codes: ArrayList<Int>): FaultCodeResultsFragment {
            val data = Bundle()
            data.putInt(EQUIPMENT_KEY, equipmentId)
            data.putIntegerArrayList(FAULT_CODES_KEY, codes)

            val fragment = FaultCodeResultsFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code_results

    private lateinit var viewModel: FaultCodeInquiryViewModel

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var faultCodeResults: RecyclerView

    private var equipmentId: Int = 0
    private var faultCodes = emptyList<Int>()

    override fun hasRequiredArgumentData(): Boolean {
        equipmentId = arguments?.getInt(EQUIPMENT_KEY) ?: 0
        faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY) ?: emptyList()

        val factory = InjectorUtils.provideFaultCodeInquiryViewModel(requireContext(), equipmentId)
        viewModel = ViewModelProvider(this, factory)
            .get(FaultCodeInquiryViewModel::class.java)

        return equipmentId == 0 || faultCodes.isNullOrEmpty()
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

    override fun loadData() {
        viewModel.loadModel(equipmentId).observe(viewLifecycleOwner, Observer {equipment->
            equipment?.let {
                updateUI(it)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer {loading ->
            when (loading) {
                true -> flowActivity?.showProgressBar()
                else -> flowActivity?.hideProgressBar()
            }
        })

        viewModel.equipmentImage.observe(viewLifecycleOwner, Observer {
            if (it != 0) modelImageView.setImageResource(it)
        })

        viewModel.equipmentSerial.observe(viewLifecycleOwner, Observer {
            serialNumberTextView.text = if (it != null) {
                getString(R.string.equipment_serial_number_fmt, it)
            } else {
                getString(R.string.equipment_serial_number)
            }
        })

        viewModel.equipmentModel.observe(viewLifecycleOwner, Observer {
            modelTextView.text = it
        })

        viewModel.equipmentNickname.observe(viewLifecycleOwner, Observer {
            equipmentNicknameTextView.text =
                if (it.isNullOrBlank())
                    getString(R.string.no_equipment_name_fmt, viewModel.equipmentModel.value)
                else
                    it
        })

        viewModel.faultCodeResultsLiveData.observe(viewLifecycleOwner, Observer {response->
            when(response){
                is FaultCodeResponse.Success ->{
                    faultCodeResults.adapter = FaultCodesAdapter(response.codes)
                }
                is FaultCodeResponse.GenericError ->{
                    flowActivity?.showServerErrorSnackBar()
                }
                is FaultCodeResponse.IOError ->{
                    flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
                }
            }
        })

        viewModel.getEquipmentFaultCode(faultCodes)
    }

    private fun updateUI(equipment: UIEquipment) {
        val equipmentNickname =
            if (equipment.nickname.isNullOrBlank())
                getString(R.string.no_equipment_name_fmt, equipment.model)
            else
                equipment.nickname

        equipmentNicknameTextView.text = equipmentNickname

        if (equipment.imageResId != 0) {
            modelImageView.setImageResource(equipment.imageResId)
        }

        modelTextView.text = equipment.model
        serialNumberTextView.text = if (equipment.serialNumber != null) {
            getString(R.string.equipment_serial_number_fmt, equipment.serialNumber)
        } else {
            getString(R.string.equipment_serial_number)
        }
    }
}

class FaultCodesAdapter(private val codes: ArrayList<FaultCodeItem>) : RecyclerView.Adapter<FaultCodeViewHolder>(){
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

        fun bindItem(faultCode: FaultCodeItem) {
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