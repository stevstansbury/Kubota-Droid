package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.databinding.FragmentMaintenanceDetailBinding
import com.android.kubota.viewmodel.MaintenanceInterval
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

private const val MAINTENANCE_KEY = "maintenance_key"

class MaintenanceDetailFragment : Fragment() {

    private var b: FragmentMaintenanceDetailBinding? = null
    private val binding get() = b!!

    private val maintenanceInterval: MaintenanceInterval by lazy {
        arguments?.getParcelable(MAINTENANCE_KEY) ?: MaintenanceInterval("", "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_maintenance_detail,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.maintenanceInterval = maintenanceInterval

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Pop back if we have nothing to show the user.
        if (maintenanceInterval.action.isEmpty()) {
            Snackbar
                .make(
                    view,
                    R.string.maintenance_detail_error,
                    BaseTransientBottomBar.LENGTH_SHORT
                )
                .show()
            parentFragmentManager.popBackStack()
        } else {
            binding.contactDealerButton.setOnClickListener {
                (activity as TabbedActivity)
                    .goToTab(Tabs.Dealers())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }

    companion object {
        fun createInstance(interval: MaintenanceInterval): MaintenanceDetailFragment {
            return MaintenanceDetailFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable(MAINTENANCE_KEY, interval)
                }
            }
        }
    }
}