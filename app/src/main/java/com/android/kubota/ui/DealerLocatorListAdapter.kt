package com.android.kubota.ui

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
    private val imageView: ImageView = itemView.findViewById(R.id.star)
    private val nameTextView: TextView = itemView.findViewById(R.id.name)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.addressLine1)
    private val addressLine2TextView: TextView = itemView.findViewById(R.id.addressLine2)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distance)

    fun onBind(dealer: SearchDealer) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.streetAddress
        addressLine2TextView.text = addressLine2TextView.resources.getString(R.string.city_state_postal_code_fmt, dealer.city, dealer.stateCode, dealer.postalCode)
        distanceTextView.text = dealer.distance
        imageView.setImageResource(if (dealer.isFavorited) R.drawable.ic_star_filled else R.drawable.ic_star_unfilled)

        itemView.setOnClickListener { listener.onClick(dealer) }
        imageView.setOnClickListener { listener.onStarClicked(dealer) }
    }

    interface OnClickListener {
        fun onClick(dealer: SearchDealer)
        fun onStarClicked(dealer: SearchDealer)
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