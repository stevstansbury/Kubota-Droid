package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.MyDealersViewModel
import com.android.kubota.viewmodel.UIDealer

class MyDealersListFragment() : BaseFragment() {

    private lateinit var viewModel: MyDealersViewModel
    private lateinit var recyclerListView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val viewAdapter: MyDealersListAdapter = MyDealersListAdapter(mutableListOf(),
        object : MyDealerView.OnClickListener {

            override fun onClick(dealer: UIDealer) {
                flowActivity?.addFragmentToBackStack(DealerDetailFragment.createIntance(dealer = dealer))
            }

        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideMyDealersViewModelFactory(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(MyDealersViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.my_dealer_list_title)

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

        viewModel.isLoading.observe(this, Observer {loading ->
            if (loading == true) {
                flowActivity?.showProgressBar()
            } else {
                flowActivity?.hideProgressBar()
            }
        })

        viewModel.preferenceDealersList.observe(this, Observer {dealerList ->
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

        view.findViewById<View>(R.id.fab).setOnClickListener {
            flowActivity?.addFragmentToBackStack(DealerLocatorFragment())
        }

        return view
    }

    private fun enableSwipeToDelete() {
        val actionDrawable = requireContext().getDrawable(R.drawable.ic_action_delete) as Drawable
        val swipeAction = SwipeAction(actionDrawable, ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color))

        val callback = object : SwipeActionCallback(requireContext(), swipeAction, swipeAction) {

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

    fun onBind(dealer: UIDealer, listener: OnClickListener) {
        nameTextView.text = dealer.name
        addressLine1TextView.text = dealer.address
        addressLine2TextView.text = "${dealer.city}, ${dealer.state} ${dealer.postalCode}"

        imageView.setOnClickListener { imageView.context.startActivity(Intent(Intent.ACTION_DEFAULT, Uri.parse("tel:" + dealer.phone))) }
        itemView.setOnClickListener { listener.onClick(dealer) }
    }

    interface OnClickListener {
        fun onClick(dealer: UIDealer)
    }
}

private class MyDealersListAdapter(private val data: MutableList<UIDealer>, val clickListener: MyDealerView.OnClickListener): RecyclerView.Adapter<MyDealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyDealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_my_dealer, viewGroup, false)

        return MyDealerView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyDealerView, position: Int) {
        holder.onBind(data[position], clickListener)
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