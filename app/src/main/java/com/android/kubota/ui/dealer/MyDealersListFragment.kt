package com.android.kubota.ui.dealer

import androidx.lifecycle.Observer
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.MyDealersViewModel
import com.android.kubota.viewmodel.UIDealer

class MyDealersListFragment : Fragment() {

    private lateinit var viewModel: MyDealersViewModel
    private lateinit var recyclerListView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val viewAdapter: MyDealersListAdapter =
        MyDealersListAdapter(mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideMyDealersViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)
            .get(MyDealersViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_my_dealers_list, null)
        emptyView = view.findViewById(R.id.emptyLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
        }

        enableSwipeToDelete()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.getUpdatedDealersList()
        }

        viewModel.preferenceDealersList.observe(viewLifecycleOwner, Observer {dealerList ->
            viewAdapter.removeAll()
            if (dealerList == null || dealerList.isEmpty()) {
                recyclerListView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerListView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                viewAdapter.addAll(dealerList)
            }

        })

        return view
    }

    private fun enableSwipeToDelete() {
        val actionDrawable = requireContext().getDrawable(R.drawable.ic_action_delete) as Drawable
        val swipeAction = SwipeAction(
            actionDrawable,
            ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color)
        )

        val callback = object : SwipeActionCallback(swipeAction, swipeAction) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p1: Int) {
                val position = viewHolder.adapterPosition
                val uiDealer = viewAdapter.getData()[position]
                val snackbar = Snackbar.make(recyclerListView, R.string.dealer_removed_action, Snackbar.LENGTH_SHORT)
                val action = viewModel.createDeleteAction(uiDealer)
                snackbar.setAction(R.string.undo_action) {
                    viewAdapter.restoreItem(uiDealer, position)
                    action.undo()
                }
                snackbar.show()
                action.commit()
                viewAdapter.removeItem(position)
            }

        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerListView)
    }

}

private class MyDealerView(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.telephoneImage)
    private val nameTextView: TextView = itemView.findViewById(R.id.dealerName)
    private val addressLine1TextView: TextView = itemView.findViewById(R.id.dealerAddress1)
    private val addressLine2TextView: TextView = itemView.findViewById(R.id.dealerAddress2)

    fun onBind(dealer: UIDealer) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.address
        addressLine2TextView.text = addressLine2TextView.resources.getString(R.string.city_state_postal_code_fmt, dealer.city, dealer.state, dealer.postalCode)

        imageView.setOnClickListener { imageView.context.startActivity(Intent(Intent.ACTION_DEFAULT, Uri.parse("tel:" + dealer.phone))) }
    }

    interface OnClickListener {
        fun onClick(dealer: UIDealer)
    }
}

private class MyDealersListAdapter(private val data: MutableList<UIDealer>): RecyclerView.Adapter<MyDealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyDealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_my_dealer, viewGroup, false)

        return MyDealerView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyDealerView, position: Int) {
        holder.onBind(data[position])
    }

    fun addAll(dealerList: List<UIDealer>) {
        data.addAll(dealerList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeAll() {
        data.clear()
        notifyDataSetChanged()
    }

    fun restoreItem(dealer: UIDealer, position: Int) {
        data.add(position, dealer)
        notifyItemInserted(position)
    }

    fun getData() = data

}