package com.android.kubota.ui.geofence

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.kubota.service.domain.Geofence

class GeofenceListFragment(
    data: List<UIGeofence>,
    private val listener: GeofenceView.OnClickListener
): RecyclerView.Adapter<GeofenceView>() {

    private var mData: MutableList<UIGeofence>
    init {
        mData = data.toMutableList()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GeofenceView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_geofence_list_item, viewGroup, false)
        return GeofenceView(view, listener)
    }

    override fun getItemCount(): Int = mData.size

    override fun onBindViewHolder(holder: GeofenceView, position: Int) {
        holder.onBind(mData[position])
    }

    override fun getItemId(position: Int): Long {
        if (position >= 0 && position < mData.size) {
            return mData[position].geofence.id.toLong()
        }
        return RecyclerView.NO_ID
    }

    fun getItem(index: Int): UIGeofence? {
        return if (index >= 0 && index < mData.size) mData[index] else null
    }

    fun removeItem(index: Int) {
        if (index > 0 && index < mData.size) {
            mData.removeAt(index)
            this.notifyItemRemoved(index)
        }
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
//    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)

    fun onBind(geofence: UIGeofence) {
        nameTextView.text = geofence.geofence.description
        addressLine1TextView.text = geofence.address
//        addressLine2TextView.text = geofence.address2
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
