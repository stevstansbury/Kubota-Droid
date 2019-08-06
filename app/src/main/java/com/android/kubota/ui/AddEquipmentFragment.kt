package com.android.kubota.ui

import android.app.Activity
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import com.google.android.material.textfield.TextInputEditText
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.AddEquipmentViewModel
import com.android.kubota.viewmodel.EquipmentUIModel

private const val MODEL_KEY = "model"

class AddEquipmentFragment : BaseFragment() {
    private lateinit var viewModel: AddEquipmentViewModel
    private lateinit var model: EquipmentUIModel

    private lateinit var modelImageView: ImageView
    private lateinit var categoryTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberEditTextView: TextInputEditText

    private var softInputMode: Int? = null

    companion object {
        fun createInstance(model: EquipmentUIModel): AddEquipmentFragment {
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

        val model = arguments?.getParcelable(MODEL_KEY) as EquipmentUIModel?

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
                modelName = model.name,
                category = getString(model.categoryResId),
                serialNumber = serialNumberEditTextView.text?.toString() ?: ""
            )
            flowActivity?.clearBackStack()
        }
        updateUI(model)

        return view
    }

    private fun updateUI(model: EquipmentUIModel) {
        this.model = model

        if (model.categoryResId != 0) {
            val category = getString(model.categoryResId)
            categoryTextView.text = category
            activity?.title = getString(R.string.equipment_detail_title_fmt, category, model.name)
        } else {
            activity?.title = model.name
        }

        if (model.imageResId != 0) {
            modelImageView.setImageResource(model.imageResId)
        }

        modelTextView.text = model.name
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity?.apply {
            softInputMode = window.attributes.softInputMode
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun onDetach() {
        super.onDetach()
        softInputMode?.let { activity?.window?.setSoftInputMode(it) }
        
        // Hide keyboard
        val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
