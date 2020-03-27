package com.android.kubota.ui

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.Utils
import com.android.kubota.utility.MultiSelectorActionCallback
import com.android.kubota.viewmodel.MyEquipmentViewModel
import com.android.kubota.viewmodel.UIEquipment
import java.util.*

class MyEquipmentsListFragment : BaseFragment() {

    private lateinit var emptyView: View
    private lateinit var viewModel: MyEquipmentViewModel
    private lateinit var recyclerListView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addEquipmentButton: FloatingActionButton
    private var isUserLoggedIn: Boolean = false
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

    private val viewAdapter: MyEquipmentListAdapter = MyEquipmentListAdapter(mutableListOf(),
        object : MyEquipmentListAdapter.MyEquipmentListener {
            override fun onSelectedCountChanged() {
                updateActionMode()
            }

            override fun onLongClick(equipment: UIEquipment) {
                startActionMode()
            }

            override fun onClick(equipment: UIEquipment) {
            val fragment = EquipmentDetailFragment.createInstance(equipment.id)
            flowActivity?.addFragmentToBackStack(fragment)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideMyEquipmentViewModelFactory(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(MyEquipmentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.my_equipment_list_title)

        val view = inflater.inflate(R.layout.fragment_my_equipment_list, null)
        emptyView = view.findViewById(R.id.emptyLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
        }

        addEquipmentButton = view.findViewById<FloatingActionButton>(R.id.fab).apply {
            setOnClickListener {
                if (isUserLoggedIn.not() && viewAdapter.itemCount > 0) {
                    resetDialog()

                    dialog = Utils.createMustLogInDialog(requireContext(), Utils.LogInDialogMode.EQUIPMENT_MESSAGE)
                    dialog?.setOnCancelListener { resetDialog() }

                    dialog?.show()
                } else {
                    flowActivity?.addFragmentToBackStack(ChooseEquipmentFragment())
                }
            }
        }

        enableSwipeToDelete()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            viewModel.getUpdatedEquipmentList()
        }

        viewModel.isUserLoggedIn.observe(this, Observer { loggedIn ->
            isUserLoggedIn = loggedIn ?: false
        })

        viewModel.preferenceEquipmentList.observe(this, Observer { equipmentList ->
            viewAdapter.removeAll()
            if (equipmentList == null || equipmentList.isEmpty()) {
                recyclerListView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerListView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                viewAdapter.addAll(equipmentList)
            }

        })

        viewModel.isLoading.observe(this, Observer {loading ->
            if (loading == true) {
                flowActivity?.showProgressBar()
            } else {
                flowActivity?.hideProgressBar()
            }
        })

        return view
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
        val swipeAction = SwipeAction(actionDrawable, ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color))

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

private class MyEquipmentListAdapter(private val data: MutableList<UIEquipment>, val listener: MyEquipmentListener): RecyclerView.Adapter<MyEquipmentListAdapter.MyEquipmentView>() {
    @SuppressLint("UseSparseArrays")
    val selectedEquipment = HashMap<Int, UIEquipment>()

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

    private fun updateEquipmentList(equipment: UIEquipment, checked: Boolean, position: Int) {
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

    fun addAll(equipmentList: List<UIEquipment>) {
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

    fun restoreItem(equipment: UIEquipment, position: Int) {
        data.add(position, equipment)
        notifyItemInserted(position)
    }

    fun removeItems(deleteEquipments: List<UIEquipment>) {
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

        fun onBind(position: Int, equipment: UIEquipment, listener: MyEquipmentListener, editEnabled: Boolean) {
            if (equipment.imageResId != 0) {
                imageView.setImageResource(equipment.imageResId)
            }

            when {
                equipment.nickname.isNullOrBlank() -> nicknameTextView.text =
                    itemView.context.getString(R.string.no_equipment_name_fmt, equipment.model)
                else -> nicknameTextView.text = equipment.nickname
            }

            modelTextView.text = equipment.model

            if (equipment.serialNumber == null || equipment.serialNumber.trim().count() == 0) {
                serialNumberTextView.visibility = View.GONE
            } else {
                serialNumberTextView.text = itemView.resources.getString(R.string.equipment_serial_number_fmt, equipment.serialNumber)
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
        fun onClick(equipment: UIEquipment)
        fun onLongClick(equipment: UIEquipment)
        fun onSelectedCountChanged()
    }
}