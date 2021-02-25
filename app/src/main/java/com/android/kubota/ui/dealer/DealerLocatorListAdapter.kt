package com.android.kubota.ui.dealer

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.android.kubota.viewmodel.dealers.SearchDealer
import com.google.android.gms.maps.model.LatLng
import com.kubota.service.domain.Dealer
import com.kubota.service.domain.preference.MeasurementUnitType

class DealerLocatorListAdapter(
    private val data: List<SearchDealer>,
    private val viewModel: DealerViewModel,
    private val listener: DealerView.OnClickListener
): RecyclerView.Adapter<DealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): DealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_dealer_locator_list_item, viewGroup, false)
        return DealerView(view, viewModel, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: DealerView, position: Int) {
        holder.onBind(data[position])
    }
}

class DealerView(
    itemView: View,
    private val viewModel: DealerViewModel,
    private val listener: OnClickListener
): RecyclerView.ViewHolder(itemView) {
    private val starView: ImageView = itemView.findViewById(R.id.star)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.addressLine1)
    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)
    private val phoneView: TextView = itemView.findViewById(R.id.phone)

    private val callView: ImageView = itemView.findViewById(R.id.call)
    private val webView: ImageView = itemView.findViewById(R.id.web)
    private val dirView: ImageView = itemView.findViewById(R.id.dir)

    fun onBind(dealer: SearchDealer) {
        onBind(dealer.toDealer())

        distanceTextView.text = dealer.distance
    }

    fun onBind(dealer: Dealer) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.address.street
        addressLine2TextView.text = addressLine2TextView.resources.getString(R.string.city_state_postal_code_fmt, dealer.address.city, dealer.address.stateCode, dealer.address.zip)
        phoneView.text = dealer.phone
        starView.setImageResource(if (viewModel.isFavorited(dealer)) R.drawable.ic_star_filled else R.drawable.ic_star_unfilled )

        itemView.setOnClickListener { listener.onSelected(dealer) }
        callView.setOnClickListener { listener.onCallClicked(dealer.phone) }
        webView.setOnClickListener { listener.onWebClicked(dealer.website) }
        dirView.setOnClickListener { listener.onDirClicked("${dealer.address.street} ${dealer.address.city} ${dealer.address.stateCode} ${dealer.address.zip}") }
        starView.setOnClickListener {
            val opposite = when(viewModel.isFavorited(dealer)) {
                true -> R.drawable.ic_star_unfilled
                false -> R.drawable.ic_star_filled
            }

            listener.onStarClicked(dealer)
            starView.setImageResource(opposite)
        }
    }

    interface OnClickListener {
        fun onStarClicked(dealer: Dealer)
        fun onSelected(dealer: Dealer)
        fun onCallClicked(number: String)
        fun onWebClicked(url: String)
        fun onDirClicked(loc: LatLng)
        fun onDirClicked(addr: String)
    }
}
