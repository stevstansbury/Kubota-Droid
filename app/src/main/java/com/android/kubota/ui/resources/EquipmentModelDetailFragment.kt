package com.android.kubota.ui.resources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.databinding.FragmentModelDetailBinding
import com.android.kubota.extensions.displayName
import com.android.kubota.ui.*
import com.android.kubota.ui.equipment.FaultCodeFragment
import com.android.kubota.utility.showMessage
import com.android.kubota.viewmodel.resources.EquipmentModelViewModel
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.EquipmentModel


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

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden) {
            activity?.title = this.model.displayName
        }
    }

    private fun setupUI() {
        activity?.title = this.model.displayName

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
                FaultCodeFragment.createInstance(equipmentModel = this.model)
            )
        }

        binding?.manualsButton?.visibility = if (this.model.manualInfo.isEmpty()) View.GONE else View.VISIBLE
        binding?.manualsButton?.setOnClickListener {
            when (this.model.manualInfo.count() == 1) {
                true -> this.flowActivity?.let {
                    ManualsListFragment.pushManualToStack(it, this.model.manualInfo.first())
                }
                false -> this.flowActivity?.addFragmentToBackStack(
                    ManualsListFragment.createInstance(
                        modelName = this.model.model,
                        manualInfo = this.model.manualInfo
                    )
                )
            }
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
        this.model.warrantyUrl?.let { warrantyUrl ->
            binding?.warrantyInfoButton?.setOnClickListener {
                showMessage(titleId= R.string.leave_app_dialog_title, messageId= R.string.leave_app_kubota_usa_website_msg)
                    .map { idx ->
                        if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                        val url = warrantyUrl.toString()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
            }
        }

        binding?.instructionalVideoButton?.visibility = if (this.model.instructionalVideos.isEmpty()) View.GONE else View.VISIBLE
        binding?.instructionalVideoButton?.setOnClickListener{
            this.flowActivity?.addFragmentToBackStack(
                VideoListFragment.createInstance(
                    modelName = this.model.model,
                    videoInfo = this.model.instructionalVideos
                )
            )
        }
    }

}