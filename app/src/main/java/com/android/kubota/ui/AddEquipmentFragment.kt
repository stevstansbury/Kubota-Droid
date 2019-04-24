package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.AddEquipmentViewModel
import com.android.kubota.viewmodel.UIModel

private const val MODEL_KEY = "model"

class AddEquipmentFragment : BaseFragment() {
    private lateinit var viewModel: AddEquipmentViewModel
    private lateinit var model: UIModel

    private lateinit var modelImageView: ImageView
    private lateinit var categoryTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberEditTextView: TextInputEditText

    companion object {
        fun createInstance(model: UIModel): AddEquipmentFragment {
            val data = Bundle(1)
            data.putParcelable(MODEL_KEY, model)

            val fragment = AddEquipmentFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideAddEquipmentViewModel(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(AddEquipmentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_equipment, null)

        val model = arguments?.getParcelable(MODEL_KEY) as UIModel?

        if (model == null) {
            requireActivity().onBackPressed()
            return null
        }

        modelImageView = view.findViewById(R.id.equipmentImage)
        categoryTextView = view.findViewById(R.id.equipmentCategory)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberEditTextView = view.findViewById(R.id.serialNumberEditText)

        view.findViewById<Button>(R.id.addButton).setOnClickListener {
            viewModel.add(
                modelName = model.modelName,
                category = getString(model.categoryResId),
                serialNumber = serialNumberEditTextView.text?.toString() ?: ""
            )
            flowActivity?.clearBackStack()
        }
        updateUI(model)

        return view
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
    }
}
