package com.android.kubota.ui.resources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.extensions.displayNameStringRes
import com.android.kubota.extensions.equipmentImageResId
import com.android.kubota.extensions.toEquipmentMode
import com.android.kubota.viewmodel.resources.EquipmentCategoriesViewModel
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.RecentViewedItem

class CategoriesFragment: BaseResourcesListFragment() {

    override val layoutResId: Int = R.layout.fragment_categories
    private lateinit var recentSearchesRecyclerView: RecyclerView

    private val viewModel: EquipmentCategoriesViewModel by lazy {
        EquipmentCategoriesViewModel.instance(owner = this.requireActivity())
    }

    override fun initUi(view: View) {
        super.initUi(view)

        recentSearchesRecyclerView = view.findViewById<RecyclerView>(R.id.recentSearches).apply {
            setHasFixedSize(true)
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun loadData() {
        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            when (loading) {
                true -> this.showProgressBar()
                else -> {
                    this.refreshLayout.isRefreshing = false
                    this.hideProgressBar()
                }
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })

        this.viewModel.equipmentCategories.observe(viewLifecycleOwner, Observer { categories ->
            recyclerView.adapter = CategoriesAdapter(categories) { onSelectCategory(it) }
        })

        this.viewModel.recentViewedItems.observe(viewLifecycleOwner, Observer { items ->
            recentSearchesRecyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            recentSearchesRecyclerView.adapter = RecentSearchesAdapter(items) { onSelectRecentlyViewed(it) }
        })

        this.viewModel.updateRecentViewed()
    }

    private fun onSelectCategory(category: EquipmentCategory) {
        flowActivity?.addFragmentToBackStack(EquipmentSubCategoryFragment.instance(category))
    }

    private fun onSelectRecentlyViewed(item: RecentViewedItem) {
        val model = item.toEquipmentMode() ?: return
        flowActivity?.addFragmentToBackStack(EquipmentModelDetailFragment.instance(model))
    }

}

class CategoriesViewHolder(view: View): BaseResourcesViewHolder<EquipmentCategory>(view) {

    override fun bind(
        data: EquipmentCategory,
        clickListener: (category: EquipmentCategory) -> Unit
    ) {
        super.bind(data, clickListener)
        title.text = data.displayNameStringRes?.let { title.context.getString(it) } ?: data.category
        data.equipmentImageResId?.let { image.setImageResource(it) }
    }
}

class CategoriesAdapter(
    data: List<EquipmentCategory>,
    clickListener: (item: EquipmentCategory) -> Unit
): BaseResourcesAdapter<EquipmentCategory>(data, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.view_equipment_category,
                parent,
                false
            )
        return CategoriesViewHolder(view)
    }
}

class RecentlyViewedViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val textView: TextView = view.findViewById(R.id.recentModel)

    fun bind(item: RecentViewedItem, clickListener: (item: RecentViewedItem) -> Unit) {
        textView.text = item.title
        itemView.setOnClickListener {
            clickListener(item)
        }
    }
}

class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view) { }

class RecentSearchesAdapter(
    private val data: List<RecentViewedItem>,
    private val clickListener: (item: RecentViewedItem) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0 -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater
                    .from(parent.context)
                    .inflate(
                        R.layout.view_recent_model_header,
                        parent,
                        false
                    )
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater
                    .from(parent.context)
                    .inflate(
                        R.layout.view_recent_model,
                        parent,
                        false
                    )
                RecentlyViewedViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int = data.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RecentlyViewedViewHolder) {
            holder.bind(data[position-1], clickListener)
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}