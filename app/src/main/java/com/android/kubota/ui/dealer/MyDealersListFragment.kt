package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.kubota.R
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
import com.android.kubota.utility.MessageDialogFragment
import com.android.kubota.utility.PermissionRequestManager
import com.android.kubota.utility.showMessage
import com.android.kubota.viewmodel.dealers.DealerViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.domain.Dealer
import java.lang.ref.WeakReference

class MyDealersListFragment : BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_my_dealers_list

    private lateinit var recyclerListView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val viewModel: DealerViewModel by lazy {
        DealerViewModel.instance(
            owner = this.requireActivity(),
            application = requireActivity().application,
            signInHandler = WeakReference { this.signInAsync() }
        )
    }

    private val listener by lazy {
        DealerViewListener(this,  viewModel)
    }

    private val viewAdapter: MyDealersListAdapter by lazy {
        MyDealersListAdapter(mutableListOf(), viewModel, listener)
    }

    override fun initUi(view: View) {
        emptyView = view.findViewById(R.id.emptyLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        enableSwipeToDelete()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.updateData()
        }
    }

    override fun loadData() {
        viewModel.favoriteDealers.observe(viewLifecycleOwner, Observer {dealers ->
            viewAdapter.removeAll()
            if (dealers.isEmpty()) {
                recyclerListView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerListView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                viewAdapter.addAll(dealers)
            }
        })
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
                val dealer = viewAdapter.getData()[position]
                val snackbar = Snackbar.make(recyclerListView, R.string.dealer_removed_action, Snackbar.LENGTH_SHORT)
                val action = viewModel.createDeleteAction(dealer)
                snackbar.setAction(R.string.undo_action) {
                    viewAdapter.restoreItem(dealer, position)
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

private class MyDealersListAdapter(
    private val data: MutableList<Dealer>,
    private val viewModel: DealerViewModel,
    val listener: DealerView.OnClickListener
) : RecyclerView.Adapter<DealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): DealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_dealer_locator_list_item, viewGroup, false)
        return DealerView(view, viewModel, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: DealerView, position: Int) {
        holder.onBind(data[position])
    }

    fun addAll(dealerList: List<Dealer>) {
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

    fun restoreItem(dealer: Dealer, position: Int) {
        data.add(position, dealer)
        notifyItemInserted(position)
    }

    fun getData() = data
}