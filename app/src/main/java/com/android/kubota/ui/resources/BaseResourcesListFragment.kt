package com.android.kubota.ui.resources

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.kubota.R
import com.android.kubota.ui.BaseFragment

abstract class BaseResourcesListFragment: BaseFragment() {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var refreshLayout: SwipeRefreshLayout

    override fun initUi(view: View) {
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            visibility = View.VISIBLE
        }
        refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout).apply {
            setOnRefreshListener {
                loadData()
            }
        }
    }
}

abstract class BaseResourcesViewHolder<T>(view: View): RecyclerView.ViewHolder(view) {
    protected val image = view.findViewById<ImageView>(R.id.imageView)
    protected val title = view.findViewById<TextView>(R.id.textView)

    open fun bind(data: T, clickListener: (item: T) -> Unit) {
        itemView.setOnClickListener {
            clickListener(data)
        }
    }
}

abstract class BaseResourcesAdapter<T>(
    private val data: List<T>,
    private val clickListener: (item: T) -> Unit
): RecyclerView.Adapter<BaseResourcesViewHolder<T>>() {

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: BaseResourcesViewHolder<T>, position: Int) {
        holder.bind(data[position], clickListener)
    }
}