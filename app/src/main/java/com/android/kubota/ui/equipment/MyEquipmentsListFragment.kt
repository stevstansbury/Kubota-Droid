package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.*
import com.android.kubota.viewmodel.equipment.EquipmentListViewModel
import com.android.kubota.utility.MultiSelectorActionCallback
import com.kubota.service.domain.EquipmentUnit
import androidx.lifecycle.Observer
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.ui.notification.NotificationTabFragment
import com.android.kubota.utility.Utils
import com.android.kubota.coordinator.flow.FlowCoordinatorActivity
import com.android.kubota.extensions.createNotificationDialog
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.done
import java.util.*

class MyEquipmentsListFragment : AuthBaseFragment() {

    override val layoutResId: Int = R.layout.fragment_my_equipment_list

    private lateinit var emptyView: View
    private lateinit var recyclerListView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEquipmentButton: View
    private var dialog: AlertDialog? = null
    private var actionMode: ActionMode? = null
    private var cabModeEnabled = false
        set(value) {
            field = value

            viewAdapter.isEditMode = value
            if (value) {
                addEquipmentButton.visibility = View.GONE
            } else {
                addEquipmentButton.visibility = View.VISIBLE
            }
        }

    private val viewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this.requireActivity())
    }

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity() )
    }

    private val deleteMode = object : MultiSelectorActionCallback() {
        /*
         * Called after startActionMode
         */
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(actionMode, menu)

            val inflater = actionMode.menuInflater
            inflater.inflate(R.menu.delete, menu)

            cabModeEnabled = true
            return true
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    val mapOfEquipment = viewAdapter.selectedEquipment.toSortedMap()
                    val list = mapOfEquipment.values.toList()
                    val action = viewModel.createMultiDeleteAction(authDelegate, list)

                    showUndoSnackbar {
                        action.undo()
                        mapOfEquipment.forEach {
                            viewAdapter.restoreItem(it.value, it.key)
                        }
                    }

                    action.commit()
                    viewAdapter.removeItems(list)
                    this@MyEquipmentsListFragment.actionMode?.finish()
                    return true
                }
                else -> return false
            }
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            cabModeEnabled = false
            this@MyEquipmentsListFragment.actionMode?.finish()
            super.onDestroyActionMode(actionMode)
        }
    }

    private val viewAdapter: MyEquipmentListAdapter =
        MyEquipmentListAdapter(mutableListOf(),
            object :
                MyEquipmentListAdapter.MyEquipmentListener {
                    override fun onSelectedCountChanged() {
                        updateActionMode()
                    }

                    override fun onLongClick(equipment: EquipmentUnit) {
                        startActionMode()
                    }

                    override fun onLocationClicked(equipment: EquipmentUnit) {
                        flowActivity?.addFragmentToBackStack(GeofenceFragment())
                    }

                    override fun onClick(equipment: EquipmentUnit) {
                        val fragment = EquipmentDetailFragment.createInstance(equipment.id)
                        flowActivity?.addFragmentToBackStack(fragment)
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

        menu.findItem(R.id.notifications)?.isVisible =
            AppProxy.proxy.accountManager.isAuthenticated.value ?: false
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
            error?.let { this.showError(it) }
        })

        this.notifyUpdateViewModel.unitUpdated.observe(viewLifecycleOwner, Observer { didUpdate ->
            if (didUpdate) {
                this.viewModel.updateData(this.authDelegate)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        resetDialog()
        resetActionMode()
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

            override fun isItemViewSwipeEnabled(): Boolean = !cabModeEnabled
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

    /**
     * we want to finish the action_mode
     * so that the context menu will dismiss upon
     * leaving this fragment
     */
    private fun resetActionMode() {
        actionMode?.finish()
    }

    /**
     * Starting the delete mode to enable the toolbar to handle
     * the contextual delete state
     */
    private fun startActionMode() {
        actionMode = (activity as AppCompatActivity).startSupportActionMode(deleteMode)
        //update the toolbar to indicate if any items are selected
        updateActionMode()
    }

    private fun updateActionMode() {
        actionMode?.let { actionMode ->
            val size = viewAdapter.selectedEquipment.size
            if (size > 0) {
                actionMode.title =
                    resources.getQuantityString(R.plurals.menu_items_selected, size, size)
            } else {
                resetActionMode()
            }
        }
    }

}

private class MyEquipmentListAdapter(
    private val data: MutableList<EquipmentUnit>,
    val listener: MyEquipmentListener
): RecyclerView.Adapter<MyEquipmentListAdapter.MyEquipmentView>() {

    @SuppressLint("UseSparseArrays")
    val selectedEquipment = HashMap<Int, EquipmentUnit>()

    // Control flag to display edit mode when true and clear out any selection when false
    var isEditMode = false
        set(value) {
            field = value
            if (!value) {
                clearSelectedQuestions()
            }
            notifyDataSetChanged()
        }

    private fun clearSelectedQuestions() {
        selectedEquipment.clear()
    }

    private fun updateEquipmentList(equipment: EquipmentUnit, checked: Boolean, position: Int) {
        when (checked){
            true-> selectedEquipment[position] = equipment
            else-> selectedEquipment.remove(position)
        }
        listener.onSelectedCountChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyEquipmentView {
        val view = LayoutInflater
            .from(viewGroup.context)
            .inflate(R.layout.my_equipment_view, viewGroup, false)

        return MyEquipmentView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyEquipmentView, position: Int) {
        holder.onBind(position, data[position], listener, isEditMode)
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
        deleteEquipments.forEach { equipment->
            data.remove(equipment)
        }
        notifyDataSetChanged()
    }

    fun getData() = data

    private inner class MyEquipmentView(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val machineCardView: MachineCardView = itemView.findViewById(R.id.machineCardView)

        fun onBind(position: Int, equipment: EquipmentUnit, listener: MyEquipmentListener, editEnabled: Boolean) {
            if (isEditMode) machineCardView.enterCABMode(selectedEquipment.containsKey(position)) else machineCardView.enterListMode()
            machineCardView.setModel(equipment, editEnabled && selectedEquipment.containsKey(position))

            machineCardView.setOnClickListener {
                if (!editEnabled){
                    listener.onClick(equipment)
                } else {
                    val newValue = !machineCardView.getSelectEquipment()
                    machineCardView.enterCABMode(newValue)
                    updateEquipmentList(equipment, newValue, position)
                }
            }

            machineCardView.setOnLongClickListener {
                if (!isEditMode) {
                    //check the box for the row we just long pressed on
                    machineCardView.enterCABMode(true)
                    machineCardView.enterCABMode(true)
                    //update our selected equipment equipment
                    updateEquipmentList(equipment, true, position)
                    //let our fragment know we can start action mode
                    listener.onLongClick(equipment)
                }

                return@setOnLongClickListener true
            }
        }
    }

    interface MyEquipmentListener {
        fun onLocationClicked(equipment: EquipmentUnit)
        fun onClick(equipment: EquipmentUnit)
        fun onLongClick(equipment: EquipmentUnit)
        fun onSelectedCountChanged()
    }
}
