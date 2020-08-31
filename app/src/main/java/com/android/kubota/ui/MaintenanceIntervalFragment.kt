package com.android.kubota.ui

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.R
import com.android.kubota.databinding.FragmentMaintenanceIntervalBinding
import com.android.kubota.databinding.ViewMaintenanceIntervalItemBinding
import com.android.kubota.viewmodel.MaintenanceInterval
import com.android.kubota.viewmodel.MaintenanceIntervalViewModel
import com.android.kubota.viewmodel.MaintenanceIntervalViewModelFactory

private const val MODEL_KEY = "model"


class MaintenanceIntervalFragment : BaseBindingFragment<FragmentMaintenanceIntervalBinding, MaintenanceIntervalViewModel>() {

    private val model: String by lazy {
        arguments?.getString(MODEL_KEY) ?: ""
    }

    override val layoutResId: Int = R.layout.fragment_maintenance_interval
    override val viewModel: MaintenanceIntervalViewModel by lazy {
        ViewModelProvider(
            this,
            MaintenanceIntervalViewModelFactory(
                model = model,
                application = requireContext().applicationContext as Application
            )
        )
            .get(MaintenanceIntervalViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.maintenance_schedules)
        binding.lifecycleOwner = this
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    override fun loadData() {
        viewModel.loading.observe(viewLifecycleOwner, Observer {
            when (it) {
                true -> flowActivity?.showProgressBar()
                false -> flowActivity?.hideProgressBar()
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            flowActivity?.makeSnackbar()?.setText(it)?.show()
        })

        viewModel.maintenanceSchedule.observe(viewLifecycleOwner, Observer {
            binding.recyclerView.adapter = MaintenanceIntervalAdapter(it) {
                flowActivity?.addFragmentToBackStack(
                    MaintenanceDetailFragment.createInstance(
                        interval = it
                    )
                )
            }
        })
    }

    companion object {
        fun createInstance(model: String): MaintenanceIntervalFragment {
            return MaintenanceIntervalFragment().apply {
                arguments = Bundle(1).apply { putString(MODEL_KEY, model) }
            }
        }
    }
}

class MaintenanceIntervalAdapter(
    private val data: List<MaintenanceInterval>,
    private val onClickListener: ((maintenanceInterval: MaintenanceInterval) -> Unit)
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ViewMaintenanceIntervalItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.view_maintenance_interval_item,
            parent,
            false
        )

        binding.root.tag = binding
        return BindingHolder(binding.root)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding: ViewDataBinding =
            holder.itemView.tag as ViewMaintenanceIntervalItemBinding
        binding.setVariable(BR.maintenanceInterval, data[position].interval)
        holder.itemView.setOnClickListener {
            onClickListener.invoke(data[position])
        }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}

