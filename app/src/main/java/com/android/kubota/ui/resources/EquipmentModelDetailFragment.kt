package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.databinding.FragmentModelDetailBinding
import com.android.kubota.ui.GuidesListFragment
import com.android.kubota.ui.ManualsListFragment
import com.android.kubota.viewmodel.resources.EquipmentModelViewModel
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.manualInfo

class EquipmentModelDetailFragment: Fragment() {

    companion object {
        private const val EQUIPMENT_MODEL = "EQUIPMENT_MODEL"

        fun instance(model: EquipmentModel): EquipmentModelDetailFragment {
            return EquipmentModelDetailFragment().apply {
                arguments = Bundle(1).apply { putString(EQUIPMENT_MODEL, model.model) }
            }
        }
    }

    private lateinit var model: String
    private var binding: FragmentModelDetailBinding? = null

    private val viewModel: EquipmentModelViewModel by lazy {
        EquipmentModelViewModel.instance(owner = this, model = this.model)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.model = arguments?.getString(EQUIPMENT_MODEL) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentModelDetailBinding
            .inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.equipmentModel.observe(viewLifecycleOwner, Observer { equipmentModel ->
            equipmentModel?.let { viewModel.saveRecentlyViewed(it) }
        })

        viewModel.updateData()
        setupUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    private fun setupUI() {
        activity?.title = this.model

        binding?.manualsButton?.setOnClickListener {
            viewModel.equipmentModel.value?.model?.let {
                this.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentPane, ManualsListFragment.createInstance(modelName=it, manualInfo = viewModel.equipmentModel.value?.manualInfo ?: emptyList()))
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding?.guidesButton?.setOnClickListener {
            viewModel.equipmentModel.value?.model?.let {
                this.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentPane, GuidesListFragment.createInstance(modelName=it))
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}