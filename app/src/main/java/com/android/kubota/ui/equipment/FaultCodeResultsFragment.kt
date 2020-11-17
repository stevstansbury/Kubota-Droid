package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.View
import com.android.kubota.R
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.databinding.FragmentFaultCodeResultsBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.ui.TabbedActivity
import com.android.kubota.ui.Tab
import com.android.kubota.viewmodel.equipment.FaultCodeViewModel
import com.android.kubota.viewmodel.equipment.FaultCodeViewModelFactory
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.kubota.service.domain.FaultCode

class FaultCodeResultsFragment : BaseBindingFragment<FragmentFaultCodeResultsBinding, FaultCodeViewModel>() {

    companion object {
        private const val FAULT_CODE_KEY = "FAULT_CODE_KEY"

        fun createInstance(faultCode: FaultCode): FaultCodeResultsFragment {
            return FaultCodeResultsFragment().apply {
                arguments = Bundle(1).apply {
                    put(FAULT_CODE_KEY, faultCode)
                }
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code_results

    private val faultCode: FaultCode by lazy {
        val faultCode: FaultCode = arguments?.getT(FAULT_CODE_KEY)!!
        faultCode
    }

    override val viewModel: FaultCodeViewModel by lazy {
        ViewModelProvider(this, FaultCodeViewModelFactory(this.faultCode))
            .get(FaultCodeViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.actionButton.setOnClickListener {
            (flowActivity as TabbedActivity).goToTab(Tab.Dealers)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.title = getString(R.string.fault_code_results_title, faultCode.code)
        }
    }

    override fun loadData() {
        activity?.title = getString(R.string.fault_code_results_title, faultCode.code)
    }

}