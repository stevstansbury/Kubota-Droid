package com.android.kubota.ui

import android.app.Activity
import android.app.Dialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.EquipmentDetailViewModel
import com.android.kubota.viewmodel.UIEquipment

private const val EQUIPMENT_KEY = "equipment"

class EquipmentDetailFragment: BaseFragment() {

    private lateinit var viewModel: EquipmentDetailViewModel
    private lateinit var equipment: UIEquipment

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var manualsButton: View
    private lateinit var guidesButton: View
    private lateinit var editSerialNumber: ImageView

    companion object {
        private const val SERIAL_NUMBER_EDIT_REQUEST_CODE = 5

        fun createInstance(equipment: UIEquipment): EquipmentDetailFragment {
            val data = Bundle(1)
            data.putParcelable(EQUIPMENT_KEY, equipment)

            val fragment = EquipmentDetailFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideEquipmentDetailViewModel(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(EquipmentDetailViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_equipment_detail, null)

        val equipment = arguments?.getParcelable(EQUIPMENT_KEY) as UIEquipment?

        if (equipment == null) {
            requireActivity().onBackPressed()
            return null
        }

        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        manualsButton = view.findViewById(R.id.manualItem)
        guidesButton = view.findViewById(R.id.guidesItem)
        editSerialNumber = view.findViewById(R.id.editEquipmentIcon)

        updateUI(equipment)

        viewModel.loadModel(equipment).observe(this, Observer {
            if (it != null) {
                updateUI(it)
            } else {
                requireActivity().onBackPressed()
            }
        })

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SERIAL_NUMBER_EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val newSerialNumber = data?.getStringExtra(SERIAL_NUMBER_KEY)
            viewModel.updateSerialNumber(equipment, newSerialNumber)

            return
        }

        return super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateUI(equipment: UIEquipment) {
        this.equipment = equipment

        val equipmentNickname =
            if (equipment.nickname.isNullOrBlank())
                getString(R.string.no_equipment_name_fmt, equipment.model)
            else
                equipment.nickname

        equipmentNicknameTextView.text = equipmentNickname
        activity?.title = equipmentNickname

        if (equipment.imageResId != 0) {
            modelImageView.setImageResource(equipment.imageResId)
        }

        modelTextView.text = equipment.model
        serialNumberTextView.text = if (equipment.serialNumber != null) {
            getString(R.string.equipment_serial_number_fmt, equipment.serialNumber)
        } else {
            getString(R.string.equipment_serial_number)
        }

        manualsButton.visibility = if (equipment.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ModelManualFragment.createInstance(equipment))
        }

        guidesButton.visibility = if (equipment.hasMaintenanceGuides) View.VISIBLE else View.GONE
        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(equipment))
            } else {
                val fragment = DisclaimerFragment.createInstance(DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED)
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        fragmentManager?.popBackStack()
                        flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(equipment))
                    }

                    override fun onDisclaimerDeclined() {
                        fragmentManager?.popBackStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        editSerialNumber.setOnClickListener {
            fragmentManager?.let {
                val dialogFragment = EditSerialNumberDialogFragment.createDialogFragmentInstance(equipment.serialNumber)
                dialogFragment.setTargetFragment(this, SERIAL_NUMBER_EDIT_REQUEST_CODE)
                dialogFragment.show(it, EditSerialNumberDialogFragment.TAG)
            }
        }
    }
}

private const val SERIAL_NUMBER_KEY = "serialNumber"

class EditSerialNumberDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EditSerialNumberDialogFragment"

        fun createDialogFragmentInstance(serialNumber: String?): EditSerialNumberDialogFragment {
            val args = Bundle(1)
            args.putString(SERIAL_NUMBER_KEY, serialNumber)
            val fragment = EditSerialNumberDialogFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var serialNumberTextView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val bodyView = inflater.inflate(R.layout.dialog_edit_serial_number, null)
        serialNumberTextView =  bodyView.findViewById(R.id.serialNumber)
        val serialNumber = arguments?.getString(SERIAL_NUMBER_KEY)
        serialNumber?.let {
            serialNumberTextView.text = it
        }

        return AlertDialog.Builder(requireContext())
            .setView(bodyView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val data = Intent()
                val newSerialNumber = if (serialNumberTextView.text.isEmpty()) null else serialNumberTextView.text.toString()
                data.putExtra(SERIAL_NUMBER_KEY, newSerialNumber)
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
                dismiss()
            }
            .setCancelable(false)
            .show()
    }
}