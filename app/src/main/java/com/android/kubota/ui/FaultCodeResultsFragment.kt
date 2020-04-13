package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.FaultCodeInquiryViewModel
import com.kubota.repository.service.FaultCodeItem
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.extensions.showServerErrorSnackBar
import com.android.kubota.ui.FaultCodesAdapter.FaultCodeViewHolder
import com.android.kubota.viewmodel.UIEquipment
import com.kubota.repository.service.FaultCodeResponse

private const val EQUIPMENT_KEY = "equipment"
private const val EQUIPMENT_MODEL = "equipment_model"
private const val FAULT_CODES_KEY = "fault_codes_key"
private const val EQUIPMENT_SERIAL_KEY = "equipment_serial_number"
private const val EQUIPMENT_NICKNAME_KEY = "equipment_nickname_key"

class FaultCodeResultsFragment : BaseFragment() {
    private lateinit var viewModel: FaultCodeInquiryViewModel

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var faultCodesTextView: TextView
    private lateinit var faultCodeResults: RecyclerView

    companion object {
        fun createInstance(equipmentId: Int, equipmentModel: String, equipmentSerial: String, equipmentNickname: String, codes: ArrayList<Int>): FaultCodeResultsFragment {
            val data = Bundle()
            data.putInt(EQUIPMENT_KEY, equipmentId)
            data.putString(EQUIPMENT_MODEL, equipmentModel)
            data.putString(EQUIPMENT_SERIAL_KEY, equipmentSerial)
            data.putString(EQUIPMENT_NICKNAME_KEY, equipmentNickname)
            data.putIntegerArrayList(FAULT_CODES_KEY, codes)

            val fragment = FaultCodeResultsFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fault_code_results, container, false)

        val equipmentId = arguments?.getInt(EQUIPMENT_KEY) ?: 0
        val model = arguments?.getString(EQUIPMENT_MODEL, null)
        val faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY)
        val serial = arguments?.getString(EQUIPMENT_SERIAL_KEY, "")
        val nickname = arguments?.getString(EQUIPMENT_NICKNAME_KEY, "")

        if (equipmentId == 0 || faultCodes.isNullOrEmpty() || model.isNullOrEmpty()) {
            fragmentManager?.popBackStack()
            return null
        }

        val factory = InjectorUtils.provideFaultCodeInquiryViewModel(requireContext(), equipmentId)
        viewModel = ViewModelProviders.of(this, factory).get(FaultCodeInquiryViewModel::class.java)

        activity?.title = getString(R.string.fault_code_inquiry)
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        faultCodesTextView = view.findViewById(R.id.faultCodesTextView)
        faultCodeResults = view.findViewById(R.id.faultCodeResults)

        faultCodeResults.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        faultCodeResults.setHasFixedSize(true)
        faultCodeResults.isNestedScrollingEnabled = false;

        equipmentNicknameTextView.text = nickname
        serialNumberTextView.text = serial
        faultCodesTextView.text = faultCodes.joinToString(separator = ", " ){"$it"}

        viewModel.loadModel(equipmentId).observe(this, Observer {equipment->
            equipment?.let {
                updateUI(it)
            }
        })

        viewModel.isLoading.observe(this, Observer {loading ->
            when (loading) {
                true -> flowActivity?.showProgressBar()
                else -> flowActivity?.hideProgressBar()
            }
        })

        viewModel.equipmentImage.observe(this, Observer {
            if (it != 0) modelImageView.setImageResource(it)
        })

        viewModel.equipmentSerial.observe(this, Observer {
            serialNumberTextView.text = if (it != null) {
                getString(R.string.equipment_serial_number_fmt, it)
            } else {
                getString(R.string.equipment_serial_number)
            }
        })

        viewModel.equipmentModel.observe(this, Observer {
            modelTextView.text = it
        })

        viewModel.equipmentNickname.observe(this, Observer {
            equipmentNicknameTextView.text =
                if (it.isNullOrBlank())
                    getString(R.string.no_equipment_name_fmt, viewModel.equipmentModel.value)
                else
                    it
        })

        viewModel.faultCodeResultsLiveData.observe(this, Observer {response->
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

        return view
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