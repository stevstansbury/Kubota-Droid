package com.android.kubota.ui.dealer

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.viewmodel.UIDealer
import com.google.android.gms.maps.model.LatLng

class DealerLocatorListAdapter(private val data: MutableList<SearchDealer>, private val listener: DealerView.OnClickListener): RecyclerView.Adapter<DealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): DealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_dealer_locator_list_item, viewGroup, false)

        return DealerView(view, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: DealerView, position: Int) {
        holder.onBind(data[position])
    }
}

class DealerView(itemView: View, private val listener: OnClickListener): RecyclerView.ViewHolder(itemView) {
    private val starView: ImageView = itemView.findViewById(R.id.star)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.addressLine1)
    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)
    private val phoneView: TextView = itemView.findViewById(R.id.phone)

    private val callView: ImageView = itemView.findViewById(R.id.call)
    private val webView: ImageView = itemView.findViewById(R.id.web)
    private val dirView: ImageView = itemView.findViewById(R.id.dir)

    fun onBind(dealer: UIDealer) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.address
        addressLine2TextView.text = addressLine2TextView.resources.getString(R.string.city_state_postal_code_fmt, dealer.city, dealer.state, dealer.postalCode)
        distanceTextView.text = "" //dealer.distance
        phoneView.text = dealer.phone
        starView.setImageResource(R.drawable.ic_star_filled )
        callView.setOnClickListener { listener.onCallClicked(dealer.phone) }
        webView.setOnClickListener { listener.onWebClicked(dealer.website) }
        dirView.setOnClickListener { listener.onDirClicked("${dealer.address} ${dealer.city} ${dealer.state} ${dealer.postalCode}") }
//        starView.setOnClickListener { listener.onStarClicked(dealer) }
    }

    fun onBind(dealer: SearchDealer) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.streetAddress
        addressLine2TextView.text = addressLine2TextView.resources.getString(R.string.city_state_postal_code_fmt, dealer.city, dealer.stateCode, dealer.postalCode)
        distanceTextView.text = dealer.distance
        phoneView.text = dealer.phone
        starView.setImageResource(if (dealer.isFavorited) R.drawable.ic_star_filled else R.drawable.ic_star_unfilled)
        callView.setOnClickListener { listener.onCallClicked(dealer.phone) }
        webView.setOnClickListener { listener.onWebClicked(dealer.webAddress) }
        dirView.setOnClickListener { listener.onDirClicked(dealer.position) }
        starView.setOnClickListener { listener.onStarClicked(dealer) }
    }

    interface OnClickListener {
        fun onStarClicked(dealer: SearchDealer)
        fun onCallClicked(number: String)
        fun onWebClicked(url: String)
        fun onDirClicked(loc: LatLng)
        fun onDirClicked(addr: String)
    }
}

class ItemDivider(context: Context, @DrawableRes resId: Int): RecyclerView.ItemDecoration() {
    private val divider: Drawable = ContextCompat.getDrawable(context, resId) as Drawable

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount

        for (i in 0..childCount) {
            val child = parent.getChildAt(i)
            child?.let {
                val params = it.layoutParams as RecyclerView.LayoutParams

                val top = it.bottom + params.bottomMargin
                val bottom = top + divider.intrinsicHeight

                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }
}