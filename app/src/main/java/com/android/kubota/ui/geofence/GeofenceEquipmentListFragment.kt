package com.android.kubota.ui.geofence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.ui.equipment.telematicsString

class GeofenceEquipmentListFragment(
    private val data: List<UIEquipmentUnit>,
    private val listener: GeoView.OnClickListener
): RecyclerView.Adapter<GeoView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GeoView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_geofence_equipment_list_item, viewGroup, false)
        return GeoView(view, listener)
    }

    override fun getItemCount(): Int = data.size

    fun itemAtIndex(index: Int): UIEquipmentUnit? {
        return if (index < 0 || index >= data.size) null else data[index]
    }

    override fun getItemId(position: Int): Long {
        if (position >= 0 && position < data.size) {
            return data[position].equipment.id.leastSignificantBits
        }
        return RecyclerView.NO_ID
    }

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
    private val timeTextView: TextView = itemView.findViewById(R.id.timeText)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)

    fun onBind(equipment: UIEquipmentUnit) {
        val iconId = if (equipment.inside) R.drawable.ic_numbered_geofence else R.drawable.ic_numbered_geofence_outside
        val drawable = ContextCompat.getDrawable(itemView.context, iconId)
        nameTextView.text = equipment.equipment.nickName ?: equipment.equipment.model
        addressLine1TextView.text = equipment.address
        timeTextView.text = equipment.equipment.telematics?.locationTime?.telematicsString(itemView.context.resources)
        distanceTextView.text = equipment.distance
        numberView.text = equipment.index.toString()
        iconView.setImageDrawable(drawable)
        itemView.setOnClickListener {
            this.listener.onClicked(equipment)
        }
    }

    interface OnClickListener {
        fun onClicked(equipment: UIEquipmentUnit)
    }
}
