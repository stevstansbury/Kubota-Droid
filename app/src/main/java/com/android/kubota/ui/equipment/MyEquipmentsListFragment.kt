package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import android.graphics.drawable.Drawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.imageResId
import com.android.kubota.ui.*
import com.android.kubota.ui.equipment.viewmodel.EquipmentListViewModel
import com.android.kubota.utility.MultiSelectorActionCallback
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class MyEquipmentsListFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_my_equipment_list

    private lateinit var emptyView: View
    private lateinit var recyclerListView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEquipmentButton: FloatingActionButton
    private var dialog: AlertDialog? = null
    private var actionMode: ActionMode? = null
    private var cabModeEnabled = false
        set(value) {
            field = value

            viewAdapter.isEditMode = value
            if (value) {
                addEquipmentButton.hide()
            } else {
                addEquipmentButton.show()
            }
        }

    private val viewModel: EquipmentListViewModel by lazy {
        EquipmentListViewModel.instance(owner = this.requireActivity()) { this.signInAsync() }
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
                    val action = viewModel.createMultiDeleteAction(list)

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

                override fun onClick(equipment: EquipmentUnit) {
                    val fragment =
                        EquipmentDetailFragment.createInstance(
                            equipment.id
                        )
                    flowActivity?.addFragmentToBackStack(fragment)
                }
            })

    override fun initUi(view: View) {
        emptyView = view.findViewById(R.id.emptyLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
        }
        emptyView.visibility = View.VISIBLE
        addEquipmentButton = view.findViewById<FloatingActionButton>(R.id.fab).apply {
            setOnClickListener {
                //TODO(JC): Add new fragment for adding new Equipment.
            }
        }

        enableSwipeToDelete()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.updateEquipmentList()
        }
    }

    override fun loadData() {
        AppProxy.proxy.accountManager.isAuthenticated.observe(viewLifecycleOwner, Observer {
            this.viewModel.updateEquipmentList()
        })

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
                val action = viewModel.createDeleteAction(uiModel)
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
    private fun resetActionMode(){
        actionMode?.finish()
    }

    /**
     * Starting the delete mode to enable the toolbar to handle
     * the contextual delete state
     */
    private fun startActionMode(){
        actionMode = (activity as AppCompatActivity).startSupportActionMode(deleteMode)
        //update the toolbar to indicate if any items are selected
        updateActionMode()
    }

    private fun updateActionMode(){
        actionMode?.let { actionMode->
            val size = viewAdapter.selectedEquipment.size
            if (size > 0) {
                actionMode.title = resources.getQuantityString(R.plurals.menu_items_selected, size, size)
            } else {
                resetActionMode()
            }
        }
    }

}

private class MyEquipmentListAdapter(private val data: MutableList<EquipmentUnit>, val listener: MyEquipmentListener): RecyclerView.Adapter<MyEquipmentListAdapter.MyEquipmentView>() {
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

    private fun clearSelectedQuestions(){
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
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.my_equipment_view, viewGroup, false)

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
        private val imageView: ImageView = itemView.findViewById(R.id.modelImage)
        private val nicknameTextView: TextView = itemView.findViewById(R.id.equipmentNickname)
        private val modelTextView: TextView = itemView.findViewById(R.id.modelName)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)
        private val arrow: ImageView = itemView.findViewById(R.id.arrow)
        private val equipmentCheckBox: CheckBox = itemView.findViewById(R.id.equipmentCheckBox)

        fun onBind(position: Int, equipment: EquipmentUnit, listener: MyEquipmentListener, editEnabled: Boolean) {
            if (equipment.imageResId != 0) {
                imageView.setImageResource(equipment.imageResId)
            }

            when {
                equipment.nickName.isNullOrBlank() -> nicknameTextView.text =
                    itemView.context.getString(R.string.no_equipment_name_fmt, equipment.model)
                else -> nicknameTextView.text = equipment.nickName
            }

            modelTextView.text = equipment.model

            if (equipment.serial == null || equipment.serial?.trim()?.count() == 0) {
                serialNumberTextView.visibility = View.GONE
            } else {
                serialNumberTextView.text = itemView.resources.getString(R.string.equipment_serial_number_fmt, equipment.serial)
                serialNumberTextView.visibility = View.VISIBLE
            }

            if(editEnabled){
                equipmentCheckBox.visibility = View.VISIBLE
                arrow.visibility = View.GONE
                equipmentCheckBox.isChecked = selectedEquipment.containsKey(position)
            }
            else{
                equipmentCheckBox.visibility = View.GONE
                arrow.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                if(!editEnabled){
                    listener.onClick(equipment)
                } else {
                    equipmentCheckBox.isChecked = !equipmentCheckBox.isChecked
                    updateEquipmentList(equipment, equipmentCheckBox.isChecked, position)
                }
            }

            itemView.setOnLongClickListener{
                if(!isEditMode){
                    //check the box for the row we just long pressed on
                    equipmentCheckBox.isChecked = true
                    //update our selected equipment equipment
                    updateEquipmentList(equipment, equipmentCheckBox.isChecked, position)
                    //let our fragment know we can start action mode
                    listener.onLongClick(equipment)
                }

                return@setOnLongClickListener true
            }
        }
    }

    interface MyEquipmentListener {
        fun onClick(equipment: EquipmentUnit)
        fun onLongClick(equipment: EquipmentUnit)
        fun onSelectedCountChanged()
    }
}