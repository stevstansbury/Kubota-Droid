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
): RecyclerView.Adapter<MaintenanceDetailView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MaintenanceDetailView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_maintenance_detail_list_item, viewGroup, false)
        return MaintenanceDetailView(view)
    }

    override fun getItemCount(): Int {
        val size = data.actions.size
        return size
    }


    override fun onBindViewHolder(holder: MaintenanceDetailView, position: Int) {
        holder.onBind(data.actions[position])
    }
}

class MaintenanceDetailView (
    itemView: View
): RecyclerView.ViewHolder(itemView) {
    private val actionView: TextView = itemView.findViewById(R.id.action)
    fun onBind(action: String) {
        actionView.text = action
    }
}

class MaintenanceDetailFragment : Fragment() {

    private lateinit var binding: FragmentMaintenanceDetailBinding

    private val maintenanceInterval: MaintenanceInterval by lazy {
        arguments?.getParcelable(MAINTENANCE_KEY) ?: MaintenanceInterval("", emptyList())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().setTitle(maintenanceInterval.interval)
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