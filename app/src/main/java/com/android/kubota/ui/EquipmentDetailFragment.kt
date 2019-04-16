package com.android.kubota.ui

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
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
import com.android.kubota.viewmodel.UIModel

private const val MODEL_KEY = "model"

class EquipmentDetailFragment: BaseFragment() {

    private lateinit var viewModel: EquipmentDetailViewModel
    private lateinit var model: UIModel

    private lateinit var modelImageView: ImageView
    private lateinit var categoryTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var manualsButton: Button
    private lateinit var guidesButton: Button
    private lateinit var editSerialNumber: ImageView

    companion object {
        private const val SERIAL_NUMBER_EDIT_REQUEST_CODE = 5

        fun createInstance(model: UIModel): EquipmentDetailFragment {
            val data = Bundle(1)
            data.putParcelable(MODEL_KEY, model)

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

        val model = arguments?.getParcelable(MODEL_KEY) as UIModel?

        if (model == null) {
            requireActivity().onBackPressed()
            return null
        }

        modelImageView = view.findViewById(R.id.equipmentImage)
        categoryTextView = view.findViewById(R.id.equipmentCategory)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        manualsButton = view.findViewById(R.id.manualsButton)
        guidesButton = view.findViewById(R.id.guidesButton)
        editSerialNumber = view.findViewById(R.id.editEquipmentIcon)

        updateUI(model)

        viewModel.loadModel(model).observe(this, Observer {
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
            viewModel.updateSerialNumber(model, newSerialNumber)

            return
        }

        return super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateUI(model: UIModel) {
        this.model = model

        if (model.categoryResId != 0) {
            val category = getString(model.categoryResId)
            categoryTextView.text = category
            activity?.title = getString(R.string.equipment_detail_title_fmt, category, model.modelName)
        } else {
            activity?.title = model.modelName
        }

        if (model.imageResId != 0) {
            modelImageView.setImageResource(model.imageResId)
        }

        modelTextView.text = model.modelName
        serialNumberTextView.text = if (model.serialNumber != null) {
            getString(R.string.equipment_serial_number_fmt, model.serialNumber)
        } else {
            getString(R.string.equipment_serial_number)
        }

        manualsButton.visibility = if (model.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ModelManualFragment.createInstance(model))
        }

        guidesButton.visibility = if (model.hasMaintenanceGuides) View.VISIBLE else View.GONE
        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(model))
            } else {
                val fragment = DisclaimerFragment.createInstance(DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED)
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        fragmentManager?.popBackStack()
                        flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(model))
                    }

                    override fun onDisclaimerDeclined() {
                        fragmentManager?.popBackStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        editSerialNumber.setOnClickListener {
            val dialogFragment = EditSerialNumberDialogFragment.createDialogFragmentInstance(model.serialNumber)
            dialogFragment.setTargetFragment(this, SERIAL_NUMBER_EDIT_REQUEST_CODE)
            dialogFragment.show(fragmentManager, EditSerialNumberDialogFragment.TAG)
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

    lateinit var serialNumberTextView: TextView

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
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                val data = Intent()
                val newSerialNumber = if (serialNumberTextView.text.isEmpty()) null else serialNumberTextView.text.toString()
                data.putExtra(SERIAL_NUMBER_KEY, newSerialNumber)
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, which ->
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
                dismiss()
            }
            .setCancelable(false)
            .show()
    }
}