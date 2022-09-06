package com.android.kubota.ui.equipment.maintenance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.ui.equipment.maintenanceDate
import com.kubota.service.domain.EquipmentMaintenanceHistoryEntry

class MaintenanceHistoryAdapter(
    private val items: List<EquipmentMaintenanceHistoryEntry>,
    private val onItemClicked: (item: EquipmentMaintenanceHistoryEntry) -> Unit
) : RecyclerView.Adapter<MaintenanceHistoryViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MaintenanceHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance_history, parent, false)

        return MaintenanceHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MaintenanceHistoryViewHolder, position: Int) {
        holder.bind(item = items[position], onItemClicked = {
            onItemClicked(it)
        })
    }

    override fun getItemCount() = items.size
}

class MaintenanceHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvHours = itemView.findViewById<TextView>(R.id.tv_hours)
    private val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)

    fun bind(
        item: EquipmentMaintenanceHistoryEntry,
        onItemClicked: (item: EquipmentMaintenanceHistoryEntry) -> Unit
    ) {
        tvHours.text = when (item.intervalType) {
            "Every X Hours" -> {
                itemView.context.getString(
                    R.string.maintenance_item_hours,
                    item.intervalValue ?: 0
                )
            }
            "Every X Months" -> {
                itemView.context.getString(
                    R.string.maintenance_item_months,
                    item.intervalValue ?: 0
                )
            }
            "Every X Years" -> {
                itemView.context.getString(
                    R.string.maintenance_item_years,
                    item.intervalValue ?: 0
                )
            }
            "Annually" -> {
                itemView.context.getString(R.string.maintenance_item_anually)
            }
            "Daily Check" -> {
                itemView.context.getString(R.string.maintenance_item_daily)
            }
            "As Needed" -> {
                itemView.context.getString(R.string.maintenance_item_as_needed)
            }
            else -> {
                "Unknown"
            }
        }

        tvStatus.text = itemView.context.getString(
            R.string.maintenance_history_status,
            item.completedEngineHours ?: 0L,
            item.updatedDate?.maintenanceDate().orEmpty()
        )

        itemView.setOnClickListener {
            onItemClicked(item)
        }
    }
}