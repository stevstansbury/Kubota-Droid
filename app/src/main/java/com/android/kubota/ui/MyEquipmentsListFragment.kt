package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
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
    private val viewAdapter: MyEquipmentListAdapter = MyEquipmentListAdapter(mutableListOf())
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
        recyclerListView = view.findViewById<RecyclerView>(R.id.equipmentList).apply {
            setHasFixedSize(true)
            adapter = viewAdapter
        }

        enableSwipeToDelete()

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

        return view
    }

    private fun enableSwipeToDelete() {
        val actionDrawable = requireContext().getDrawable(R.drawable.ic_action_delete) as Drawable
        val swipeAction = SwipeAction(actionDrawable, ContextCompat.getColor(requireContext(), R.color.delete_swipe_action_color))

        val callback = object : SwipeActionCallback(requireContext(), swipeAction, swipeAction) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p1: Int) {
                val position = viewHolder.adapterPosition
                val uiModel = viewAdapter.getData()[position]
                val snackbar = Snackbar.make(recyclerListView, R.string.equipment_removed_action, Snackbar.LENGTH_SHORT)
                snackbar.setAction(R.string.undo_action, object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        viewAdapter.restoreItem(uiModel, position)
                    }

                })
                snackbar.show()
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

    fun onBind(model: UIModel) {
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
    }
}

private class MyEquipmentListAdapter(private val data: MutableList<UIModel>): RecyclerView.Adapter<MyEquipmentView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyEquipmentView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.my_equipment_view, null)

        return MyEquipmentView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: MyEquipmentView, position: Int) {
        holder.onBind(data[position])
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