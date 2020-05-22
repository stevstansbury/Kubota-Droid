package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.android.kubota.R
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.databinding.FragmentFaultCodeResultsBinding
import com.android.kubota.ui.BaseBindingFragment
import com.android.kubota.ui.TabbedActivity
import com.android.kubota.ui.Tabs
import com.android.kubota.viewmodel.equipment.FaultCodeViewModel
import com.android.kubota.viewmodel.equipment.FaultCodeViewModelFactory
import com.kubota.service.api.KubotaServiceError

private const val MODEL_KEY = "model"
private const val FAULT_CODE_KEY = "fault_code"

class FaultCodeResultsFragment : BaseBindingFragment<FragmentFaultCodeResultsBinding, FaultCodeViewModel>() {

    companion object {
        fun createInstance(model: String, faultCode: Int): FaultCodeResultsFragment {
            return FaultCodeResultsFragment().apply {
                arguments = Bundle(2).apply {
                    putString(MODEL_KEY, model)
                    putInt(FAULT_CODE_KEY, faultCode)
                }
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code_results

    private val unitModel: String by lazy {
        arguments?.getString(MODEL_KEY) ?: ""
    }

    private val faultCode: Int by lazy {
        arguments?.getInt(FAULT_CODE_KEY) ?: 0
    }

    override val viewModel: FaultCodeViewModel by lazy {
        ViewModelProvider(this, FaultCodeViewModelFactory(unitModel, faultCode))
            .get(FaultCodeViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.actionButton.setOnClickListener {
            (flowActivity as TabbedActivity).goToTab(Tabs.Dealers())
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun loadData() {
        activity?.title = getString(R.string.fault_code_results_title, faultCode)

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> flowActivity?.showProgressBar()
                false -> flowActivity?.hideProgressBar()
            }
        })
    }

    private fun showError(error: Throwable) {
        val errorStringResID = when (error) {
            is KubotaServiceError.NotFound -> R.string.fault_code_not_found
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                R.string.connectivity_error_message
            else -> R.string.server_error_message
        }
        flowActivity?.makeSnackbar()?.setText(errorStringResID)?.show()
        parentFragmentManager.popBackStack()
    }
}