package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.databinding.FragmentMaintenanceDetailBinding
import com.android.kubota.viewmodel.MaintenanceInterval

private const val MAINTENANCE_KEY = "maintenance_key"


class MaintenanceDetailListFragment(
    private val data: MaintenanceInterval
) : RecyclerView.Adapter<MaintenanceDetailView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MaintenanceDetailView {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.view_maintenance_detail_list_item, viewGroup, false)
        return MaintenanceDetailView(view)
    }

    override fun getItemCount(): Int {
        return data.actions.size
    }


    override fun onBindViewHolder(holder: MaintenanceDetailView, position: Int) {
        holder.onBind(data.actions[position])
    }
}

class MaintenanceDetailView(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    private val actionView: TextView = itemView.findViewById(R.id.action)
    fun onBind(action: String) {
        actionView.text = action
    }
}

class MaintenanceDetailFragment : Fragment() {

    private lateinit var binding: FragmentMaintenanceDetailBinding

    private val maintenanceInterval: MaintenanceInterval by lazy {
        arguments?.getParcelable(MAINTENANCE_KEY)
            ?: MaintenanceInterval("", 0,  emptyList(), 0, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().title = when (maintenanceInterval.intervalType) {
            "Every X Hours" -> {
                requireContext().getString(
                    R.string.maintenance_item_hours,
                    maintenanceInterval.intervalValue
                )
            }
            "Every X Months" -> {
                requireContext().getString(
                    R.string.maintenance_item_months,
                    maintenanceInterval.intervalValue
                )
            }
            "Every X Years" -> {
                requireContext().getString(
                    R.string.maintenance_item_years,
                    maintenanceInterval.intervalValue
                )
            }
            "Annually" -> {
                requireContext().getString(R.string.maintenance_item_anually)
            }
            "Daily Check" -> {
                requireContext().getString(R.string.maintenance_item_daily)
            }
            "As Needed" -> {
                requireContext().getString(R.string.maintenance_item_as_needed)
            }
            else -> {
                "Unknown"
            }
        }

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_maintenance_detail,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.maintenance.adapter = MaintenanceDetailListFragment(maintenanceInterval)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contactDealerButton.setOnClickListener {
            (activity as TabbedActivity).goToTab(Tab.Dealers)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            requireActivity().title = when (maintenanceInterval.intervalType) {
                "Every X Hours" -> {
                    requireContext().getString(
                        R.string.maintenance_item_hours,
                        maintenanceInterval.intervalValue
                    )
                }
                "Every X Months" -> {
                    requireContext().getString(
                        R.string.maintenance_item_months,
                        maintenanceInterval.intervalValue
                    )
                }
                "Every X Years" -> {
                    requireContext().getString(
                        R.string.maintenance_item_years,
                        maintenanceInterval.intervalValue
                    )
                }
                "Annually" -> {
                    requireContext().getString(R.string.maintenance_item_anually)
                }
                "Daily Check" -> {
                    requireContext().getString(R.string.maintenance_item_daily)
                }
                "As Needed" -> {
                    requireContext().getString(R.string.maintenance_item_as_needed)
                }
                else -> {
                    "Unknown"
                }
            }
        }
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