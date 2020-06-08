package com.android.kubota.ui.dealer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.kubota.service.domain.Geofence

class GeofenceListFragment(
    private val data: List<UIGeofence>,
    private val listener: GeofenceView.OnClickListener
): RecyclerView.Adapter<GeofenceView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GeofenceView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_geofence_list_item, viewGroup, false)
        return GeofenceView(view, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: GeofenceView, position: Int) {
        holder.onBind(data[position])
    }
}

class GeofenceView (
    itemView: View,
    private val listener: OnClickListener
): RecyclerView.ViewHolder(itemView) {
    private val iconView: ImageView = itemView.findViewById(R.id.icon)
    private val numberView: TextView = itemView.findViewById(R.id.number)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val editView: ImageView = itemView.findViewById(R.id.edit)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.addressLine1)
    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)

    fun onBind(geofence: UIGeofence) {
        nameTextView.text = geofence.geofence.name
        addressLine1TextView.text = geofence.address1
        addressLine2TextView.text = geofence.address2
        distanceTextView.text = geofence.distance
        numberView.text = geofence.index.toString()

        editView.setOnClickListener {
            this.listener.onEditClicked(geofence)
        }

        itemView.setOnClickListener {
            this.listener.onClicked(geofence)
        }
    }

    interface OnClickListener {
        fun onClicked(geofence: UIGeofence)
        fun onEditClicked(geofence: UIGeofence)
    }
}
