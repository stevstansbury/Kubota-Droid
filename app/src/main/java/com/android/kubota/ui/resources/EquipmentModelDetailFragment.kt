package com.android.kubota.ui.resources

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.kubota.app.AppProxy
import com.android.kubota.databinding.FragmentModelDetailBinding
import com.android.kubota.ui.FlowActivity
import com.android.kubota.ui.GuidesListFragment
import com.android.kubota.ui.MaintenanceIntervalFragment
import com.android.kubota.ui.ManualsListFragment
import com.android.kubota.ui.equipment.FaultCodeInquiryFragment
import com.android.kubota.viewmodel.resources.EquipmentModelViewModel
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.manualInfo


class EquipmentModelDetailFragment: Fragment() {

    companion object {
        private const val EQUIPMENT_MODEL = "EQUIPMENT_MODEL"

        fun instance(model: EquipmentModel): EquipmentModelDetailFragment {
            return EquipmentModelDetailFragment().apply {
                val data = Bundle(1)
                data.put(EQUIPMENT_MODEL, model)
                arguments = data
            }
        }
    }

    private var flowActivity: FlowActivity? = null
    private lateinit var model: EquipmentModel
    private var binding: FragmentModelDetailBinding? = null

    private val viewModel: EquipmentModelViewModel by lazy {
        EquipmentModelViewModel.instance(owner = this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.flowActivity = context as? FlowActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.model = arguments?.getT(EQUIPMENT_MODEL)!!
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

        viewModel.saveRecentlyViewed(this.model)
        setupUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    private fun setupUI() {
        activity?.title = this.model.model

        this.model.imageResources?.heroUrl?.let { url ->
            AppProxy.proxy.serviceManager.contentService
                    .getBitmap(url)
                    .done { bitmap ->
                        bitmap?.let {
                            binding?.headerImage?.setImageBitmap(it)
                        }
                    }
        }

        binding?.faultCodeButton?.visibility = if (this.model.hasFaultCodes) View.VISIBLE else View.GONE
        binding?.faultCodeButton?.setOnClickListener {
            this.flowActivity?.addFragmentToBackStack(
                FaultCodeInquiryFragment.createInstance(equipmentModel = this.model)
            )
        }

        binding?.manualsButton?.visibility = if (this.model.manualUrls?.isEmpty() == true) View.GONE else View.VISIBLE
        binding?.manualsButton?.setOnClickListener {
            this.flowActivity?.addFragmentToBackStack(
                ManualsListFragment.createInstance(
                    modelName = this.model.model,
                    manualInfo = this.model.manualInfo
                )
            )
        }

        binding?.guidesButton?.visibility = if (this.model.guideUrl == null) View.GONE else View.VISIBLE
        binding?.guidesButton?.setOnClickListener {
            this.flowActivity?.addFragmentToBackStack(
                GuidesListFragment.createInstance(modelName=this.model.model)
            )
        }

        binding?.maintenanceSchedulesButton?.visibility = if (this.model.hasMaintenanceSchedules) View.VISIBLE else View.GONE
        binding?.maintenanceSchedulesButton?.setOnClickListener {
            flowActivity?.addFragmentToBackStack(
                MaintenanceIntervalFragment.createInstance(this.model.model)
            )
        }

        binding?.warrantyInfoButton?.visibility = if (this.model.warrantyUrl != null) View.VISIBLE else View.GONE
    }

}