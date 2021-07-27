package com.android.kubota.ui.equipment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.kubota.R
import com.android.kubota.coordinator.flow.FlowCoordinatorActivity
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.ui.SwipeAction
import com.android.kubota.ui.SwipeActionCallback
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.ui.notification.NotificationMenuController
import com.android.kubota.ui.notification.NotificationTabFragment
import com.android.kubota.utility.showSimpleMessage
import com.android.kubota.viewmodel.equipment.EquipmentListDeleteError
import com.android.kubota.viewmodel.equipment.EquipmentListViewModel
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentUnit

class MyEquipmentsListFragment : AuthBaseFragment() {

    override val layoutResId: Int = R.layout.fragment_my_equipment_list

    private lateinit var emptyView: View
    private lateinit var recyclerListView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEquipmentButton: View
    private val menuController: NotificationMenuController by lazy {
        NotificationMenuController(requireActivity())
    }
    private var dialog: AlertDialog? = null

    private val viewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this.requireActivity())
    }

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

    private val viewAdapter: MyEquipmentListAdapter =
        MyEquipmentListAdapter(mutableListOf(),
            object : MyEquipmentListAdapter.MyEquipmentListener {
                override fun onClick(equipment: EquipmentUnit) {
                    val fragment = EquipmentDetailFragment.createInstance(equipment)
                    flowActivity?.addFragmentToBackStack(fragment)
                }

                override fun onLocationClicked(equipment: EquipmentUnit) {
                    flowActivity?.addFragmentToBackStack(GeofenceFragment.createInstance(equipment.telematics?.location))
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_tab_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menuController.onPrepareOptionsMenu(menu = menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.notifications -> {
                flowActivity?.addFragmentToBackStack(NotificationTabFragment())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initUi(view: View) {
        emptyView = view.findViewById(R.id.emptyLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
        }
        emptyView.visibility = View.VISIBLE
        addEquipmentButton = view.findViewById<View>(R.id.addEquipmentButton).apply {
            setOnClickListener {
                (requireActivity() as? FlowCoordinatorActivity)?.startAddEquipmentUnit()
            }
        }

        enableSwipeToDelete()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.updateData(this.authDelegate)
        }
    }

    override fun loadData() {
        this.viewModel.equipmentList.observe(viewLifecycleOwner, Observer { units ->
            this.viewAdapter.removeAll()
            if (units == null || units.isEmpty()) {
                this.recyclerListView.visibility = View.GONE
                this.emptyView.visibility = View.VISIBLE
            } else {
                this.recyclerListView.visibility = View.VISIBLE
                this.emptyView.visibility = View.GONE
                this.viewAdapter.addAll(units)
            }
        })

        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            when (loading) {
                true -> this.showProgressBar()
                else -> this.hideProgressBar()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                when (it) {
                    is EquipmentListDeleteError.CannotDeleteTelematicsEquipment ->
                        this.showSimpleMessage(
                            titleId = R.string.equipment_delete_error_title,
                            messageId = R.string.equipment_cannot_delete_error_message
                        )
                            .done {
                                viewAdapter.removeAll()
                                viewAdapter.addAll(viewModel.equipmentList.value ?: emptyList())
                            }
                    else ->
                        this.showError(it)
                }
                viewModel.clearError()
            }
        })

        this.notifyUpdateViewModel.unitUpdated.observe(viewLifecycleOwner, Observer { didUpdate ->
            if (didUpdate) {
                this.viewModel.updateData(this.authDelegate)
            }
        })

        this.viewModel.unreadNotifications.observe(this, menuController.unreadNotificationsObserver)
        this.viewModel.loadUnreadNotification(this.authDelegate)
    }

    override fun onPause() {
        super.onPause()
        resetDialog()
    }

    private fun resetDialog() {
        dialog?.dismiss()
        dialog = null
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
                val uiModel = viewAdapter.getData()[position]
                val action = viewModel.createDeleteAction(authDelegate, uiModel)
                showUndoSnackbar {
                    viewAdapter.restoreItem(uiModel, position)
                    action.undo()
                }
                action.commit()
                viewAdapter.removeItem(position)
            }

            override fun isItemViewSwipeEnabled() = true
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerListView)
    }

    private fun showUndoSnackbar(onUndo: () -> Unit) {
        flowActivity?.makeSnackbar()?.apply {
            setText(R.string.equipment_removed_action)
            setAction(R.string.undo_action) { onUndo() }
            show()
        }
    }

}

private class MyEquipmentListAdapter(
    private val data: MutableList<EquipmentUnit>,
    val listener: MyEquipmentListener
) : RecyclerView.Adapter<MyEquipmentListAdapter.MyEquipmentView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyEquipmentView {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(R.layout.my_equipment_view, viewGroup, false)

        return MyEquipmentView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyEquipmentView, position: Int) {
        holder.onBind(data[position], listener)
    }

    fun addAll(equipmentList: List<EquipmentUnit>) {
        data.addAll(equipmentList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeAll() {
        data.clear()
    }

    fun restoreItem(equipment: EquipmentUnit, position: Int) {
        data.add(position, equipment)
        notifyItemInserted(position)
    }

    fun removeItems(deleteEquipments: List<EquipmentUnit>) {
        deleteEquipments.forEach { equipment ->
            data.remove(equipment)
        }
        notifyDataSetChanged()
    }

    fun getData() = data

    private inner class MyEquipmentView(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val machineCardView: MachineCardView = itemView.findViewById(R.id.machineCardView)

        fun onBind(equipment: EquipmentUnit, listener: MyEquipmentListener) {
            machineCardView.setModel(equipment)
            machineCardView.setOnClickListener { listener.onClick(equipment) }
            machineCardView.setOnLocationViewClicked(object :
                MachineCardView.OnLocationViewClicked {
                override fun onClick() {
                    listener.onLocationClicked(equipment)
                }
            })
        }
    }

    interface MyEquipmentListener {
        fun onLocationClicked(equipment: EquipmentUnit)
        fun onClick(equipment: EquipmentUnit)
    }
}
