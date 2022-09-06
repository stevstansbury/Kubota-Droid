package com.android.kubota.ui.equipment.maintenance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.R
import com.android.kubota.databinding.ItemMaintenanceChecklistBinding
import com.android.kubota.databinding.ItemMaintenanceChecklistCategoryBinding
import com.android.kubota.viewmodel.equipment.MaintenanceChecklistItem
import com.android.kubota.viewmodel.equipment.MaintenanceChecklistItemViewModel
import kotlinx.android.synthetic.main.view_machine_card.view.*

class MaintenanceChecklistAdapter(
    private val items: List<MaintenanceChecklistItem>,
    private val onClickListener: ((item: MaintenanceChecklistItemViewModel) -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = if (viewType == 0) {
            DataBindingUtil.inflate<ItemMaintenanceChecklistBinding>(
                LayoutInflater.from(parent.context),
                R.layout.item_maintenance_checklist,
                parent,
                false
            )
        } else {
            DataBindingUtil.inflate<ItemMaintenanceChecklistCategoryBinding>(
                LayoutInflater.from(parent.context),
                R.layout.item_maintenance_checklist_category,
                parent,
                false
            )
        }

        binding.root.tag = binding
        return BindingHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        val binding: ViewDataBinding = if (item is MaintenanceChecklistItemViewModel) {
            holder.itemView.tag as ItemMaintenanceChecklistBinding
        } else {
            holder.itemView.tag as ItemMaintenanceChecklistCategoryBinding
        }

        binding.setVariable(BR.item, item)

        if (binding is ItemMaintenanceChecklistBinding) {
            binding.checkbox.setOnClickListener {
                if (item is MaintenanceChecklistItemViewModel) {
                    onClickListener.invoke(item)
                }
            }
        }

        holder.itemView.setOnClickListener {
            if (item is MaintenanceChecklistItemViewModel) {
                onClickListener.invoke(item)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is MaintenanceChecklistItemViewModel) {
            0
        } else {
            1
        }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}