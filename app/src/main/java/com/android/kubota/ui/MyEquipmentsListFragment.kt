package com.android.kubota.ui

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
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
import com.android.kubota.viewmodel.MyEquipmentViewModel
import com.android.kubota.viewmodel.UIModel


class MyEquipmentsListFragment() : BaseFragment() {

    private lateinit var viewModel: MyEquipmentViewModel
    private lateinit var recyclerListView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isUserLoggedIn: Boolean = false
    private var dialog: AlertDialog? = null

    private val viewAdapter: MyEquipmentListAdapter = MyEquipmentListAdapter(mutableListOf(),
        object : MyEquipmentView.OnClickListener {

        override fun onClick(model: UIModel) {
            val fragment = EquipmentDetailFragment.createInstance(model)
            flowActivity?.addFragmentToBackStack(fragment)
        }

    })

    private lateinit var emptyView: View

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

        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            if (isUserLoggedIn.not() && viewAdapter.itemCount > 0) {
                resetDialog()

                dialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.sign_in_modal_title)
                    .setMessage(R.string.sign_in_modal_message)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> resetDialog() }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requireContext().startActivity(Intent(requireContext(), SignUpActivity::class.java))
                    }
                    .create()

                dialog?.show()
            } else {
                flowActivity?.addFragmentToBackStack(ChooseEquipmentFragment())
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

        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerListView)
    }
}

private class MyEquipmentView(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.modelImage)
    private val categoryTextView: TextView = itemView.findViewById(R.id.modelCategory)
    private val modelTextView: TextView = itemView.findViewById(R.id.modelName)
    private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)

    fun onBind(model: UIModel, listener: OnClickListener) {
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

        itemView.setOnClickListener { listener.onClick(model) }
    }

    interface OnClickListener {
        fun onClick(model: UIModel)
    }
}

private class MyEquipmentListAdapter(private val data: MutableList<UIModel>, val clickListener: MyEquipmentView.OnClickListener): RecyclerView.Adapter<MyEquipmentView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyEquipmentView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.my_equipment_view, viewGroup, false)

        return MyEquipmentView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyEquipmentView, position: Int) {
        holder.onBind(data[position], clickListener)
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

    fun getData() = data

}