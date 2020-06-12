package com.android.kubota.ui.geofence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.kubota.service.domain.EquipmentUnit

class GeofenceEquipmentListFragment(
    private val data: List<UIEquipmentUnit>,
    private val listener: GeoView.OnClickListener
): RecyclerView.Adapter<GeoView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GeoView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_geofence_equipment_list_item, viewGroup, false)
        return GeoView(view, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: GeoView, position: Int) {
        holder.onBind(data[position])
    }
}

class GeoView (
    itemView: View,
    private val listener: OnClickListener
): RecyclerView.ViewHolder(itemView) {
    private val iconView: ImageView = itemView.findViewById(R.id.marker)
    private val numberView: TextView = itemView.findViewById(R.id.number)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.addressLine1)
//    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)

    fun onBind(equipment: UIEquipmentUnit) {
        nameTextView.text = equipment.equipment.nickName ?: equipment.equipment.model
        addressLine1TextView.text = equipment.address
//        addressLine2TextView.text = equipment.address2
        distanceTextView.text = equipment.distance
        numberView.text = equipment.index.toString()
        itemView.setOnClickListener {
            this.listener.onClicked(equipment)
        }
    }

    interface OnClickListener {
        fun onClicked(equipment: UIEquipmentUnit)
    }
}
