package com.android.kubota.ui.dealer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.kubota.R
import com.android.kubota.extensions.showDialog
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.PermissionRequestManager
import com.android.kubota.viewmodel.MyDealersViewModel
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.viewmodel.UIDealer
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover


class MyDealersListFragment : Fragment() {

    private lateinit var viewModel: MyDealersViewModel
    private lateinit var recyclerListView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val listener = object: DealerView.OnClickListener {

        override fun onStarClicked(dealer: SearchDealer) {
//            viewAdapter.removeItem()
        }

        override fun onWebClicked(url: String) {
            val addr = if (!url.startsWith("http", ignoreCase = true)) {
                "https://www.kubotausa.com/dealers/${url}"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(addr))
            startActivity(intent)
        }

        @SuppressLint("MissingPermission")
        override fun onCallClicked(number: String) {
            PermissionRequestManager
                .requestPermission(requireActivity(), Manifest.permission.CALL_PHONE)
                .map {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${number}"))
                    requireActivity().startActivity(intent)
                }
                .recover {
                    showDialog(message="Permission to make phone was not granted", positiveButton="Ok", cancelable=false)
                }
        }

        override fun onDirClicked(addr: String) {
            val uri: Uri = Uri.parse("google.navigation:q=$addr")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }

        override fun onDirClicked(loc: LatLng) {
            val uri: Uri = Uri.parse("google.navigation:q=${loc.latitude},${loc.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }
    }

    private val viewAdapter: MyDealersListAdapter =
        MyDealersListAdapter(mutableListOf(), listener)

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
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
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

private class MyDealersListAdapter(private val data: MutableList<UIDealer>, val listener: DealerView.OnClickListener): RecyclerView.Adapter<DealerView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): DealerView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_dealer_locator_list_item, viewGroup, false)
        return DealerView(view, listener)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: DealerView, position: Int) {
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