package com.android.kubota.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.Utils
import com.android.kubota.utility.MultiSelectorActionCallback
import com.android.kubota.viewmodel.MyEquipmentViewModel
import com.android.kubota.viewmodel.UIModel
import java.util.*

class MyEquipmentsListFragment() : BaseFragment() {

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
                    val snackBar = Snackbar.make(recyclerListView, R.string.equipment_removed_action, Snackbar.LENGTH_SHORT)
                    val action = viewModel.createMultiDeleteAction(list)

                    snackBar.setAction(R.string.undo_action) {
                        action.undo()
                        mapOfEquipment.forEach {
                            viewAdapter.restoreItem(it.value, it.key)
                        }
                    }
                    snackBar.show()
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

            override fun onLongClick(model: UIModel) {
                startActionMode()
            }

            override fun onClick(model: UIModel) {
            val fragment = EquipmentDetailFragment.createInstance(model)
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

        viewModel.preferenceModelList.observe(this, Observer {modelList ->
            viewAdapter.removeAll()
            if (modelList == null || modelList.isEmpty()) {
                recyclerListView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerListView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                viewAdapter.addAll(modelList)
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

        val callback = object : SwipeActionCallback(requireContext(), swipeAction, swipeAction) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p1: Int) {
                val position = viewHolder.adapterPosition
                val uiModel = viewAdapter.getData()[position]
                val snackbar = Snackbar.make(recyclerListView, R.string.equipment_removed_action, Snackbar.LENGTH_SHORT)
                val action = viewModel.createDeleteAction(uiModel)
                snackbar.setAction(R.string.undo_action) {
                    viewAdapter.restoreItem(uiModel, position)
                    action.undo()
                }
                snackbar.show()
                action.commit()
                viewAdapter.removeItem(position)
            }

            override fun isItemViewSwipeEnabled(): Boolean = !cabModeEnabled
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerListView)
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
                actionMode.title = getString(R.string.menu_items_selected, size)
            } else {
                resetActionMode()
            }
        }
    }

}

private class MyEquipmentListAdapter(private val data: MutableList<UIModel>, val listener: MyEquipmentListener): RecyclerView.Adapter<MyEquipmentListAdapter.MyEquipmentView>() {
    @SuppressLint("UseSparseArrays")
    val selectedEquipment = HashMap<Int, UIModel>()

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

    private fun updateEquipmentList(model: UIModel, checked: Boolean, position: Int) {
        when (checked){
            true-> selectedEquipment[position] = model
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

    fun addAll(modelList: List<UIModel>) {
        data.addAll(modelList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeAll() {
        data.clear()
    }

    fun restoreItem(model: UIModel, position: Int) {
        data.add(position, model)
        notifyItemInserted(position)
    }

    fun removeItems(deleteModels: List<UIModel>) {
        deleteModels.forEach {model->
            data.remove(model)
        }
        notifyDataSetChanged()
    }

    fun getData() = data

    private inner class MyEquipmentView(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.modelImage)
        private val categoryTextView: TextView = itemView.findViewById(R.id.modelCategory)
        private val modelTextView: TextView = itemView.findViewById(R.id.modelName)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)
        private val arrow: ImageView = itemView.findViewById(R.id.arrow)
        private val equipmentCheckBox: CheckBox = itemView.findViewById(R.id.equipmentCheckBox)

        fun onBind(position: Int, model: UIModel, listener: MyEquipmentListener, editEnabled: Boolean) {
            if (model.imageResId != 0) {
                imageView.setImageResource(model.imageResId)
            }

            if (model.categoryResId != 0) {
                categoryTextView.setText(model.categoryResId)
                categoryTextView.visibility = View.VISIBLE
            } else {
                categoryTextView.visibility = View.GONE
            }

            modelTextView.text = model.modelName

            if (model.serialNumber == null || model.serialNumber.trim().count() == 0) {
                serialNumberTextView.visibility = View.GONE
            } else {
                serialNumberTextView.text = itemView.resources.getString(R.string.equipment_serial_number_fmt, model.serialNumber)
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

            equipmentCheckBox.setOnClickListener{
                if (editEnabled) {
                    updateEquipmentList(model, equipmentCheckBox.isChecked, position)
                }
            }

            itemView.setOnClickListener {
                if(!editEnabled){
                    listener.onClick(model)
                }
            }

            itemView.setOnLongClickListener{
                if(!isEditMode){
                    //check the box for the row we just long pressed on
                    equipmentCheckBox.isChecked = true
                    //update our selected equipment model
                    updateEquipmentList(model, equipmentCheckBox.isChecked, position)
                    //let our fragment know we can start action mode
                    listener.onLongClick(model)
                }

                return@setOnLongClickListener true
            }
        }
    }

    interface MyEquipmentListener {
        fun onClick(model: UIModel)
        fun onLongClick(model: UIModel)
        fun onSelectedCountChanged()
    }
}