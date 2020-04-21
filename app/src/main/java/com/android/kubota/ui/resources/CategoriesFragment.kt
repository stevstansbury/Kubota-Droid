package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.ui.ItemDivider
import com.android.kubota.utility.CategoryUtils
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.KubotaEquipmentCategory
import kotlinx.coroutines.launch

class CategoriesFragment: BaseResourcesListFragment() {

    override val layoutResId: Int = R.layout.fragment_categories
    private lateinit var recentSearchesRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        loadData()

        return view
    }

    override fun initUi(view: View) {
        super.initUi(view)
        recentSearchesRecyclerView = view.findViewById<RecyclerView>(R.id.recentSearches).apply {
            setHasFixedSize(true)
            addItemDecoration(
                ItemDivider(requireContext(), R.drawable.divider)
            )
        }
    }

    override fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            refreshLayout.isRefreshing = true
            when (val results = viewModel.loadCategories()) {
                is CategorySyncResults.Success -> {
                    recyclerView.adapter =
                        CategoriesAdapter(results.results) {
                            flowActivity?.addFragmentToBackStack(if (it.hasSubCategories) {
                                EquipmentSubCategoryFragment
                                    .createSubCategoryInstance(it.category)
                            } else {
                                EquipmentSubCategoryFragment
                                    .createModelInstance(it.category)
                            })
                        }
                }
                is CategorySyncResults.ServerError -> flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
                is CategorySyncResults.IOException -> flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
            }
            refreshLayout.isRefreshing = false
        }
        //TODO(JC): Placeholder data. We are waiting on feedback from client on the format of the data for this section
        val data = listOf("L3560 LIMITED EDITION", "M5N-091 POWER KRAWLER", "M8540 NARROW POWER KRAWLER", "RTV-X1120")
        recentSearchesRecyclerView.adapter = RecentSearchesAdapter(data) {

        }
    }
}

class CategoriesViewHolder(view: View): BaseResourcesViewHolder<KubotaEquipmentCategory>(view) {

    override fun bind(
        data: KubotaEquipmentCategory,
        clickListener: (category: KubotaEquipmentCategory) -> Unit
    ) {
        super.bind(data, clickListener)
        title.text = title.context.getString(CategoryUtils.getEquipmentName(data.category))
        CategoryUtils.getEquipmentImage(data.category).let {
            if (it != 0) {
                image.setImageResource(it)
            }
        }
    }
}

class CategoriesAdapter(
    data: List<KubotaEquipmentCategory>,
    clickListener: (item: KubotaEquipmentCategory) -> Unit
): BaseResourcesAdapter<KubotaEquipmentCategory>(data, clickListener) {

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

    fun bind(modelName: String, clickListener: (item: String) -> Unit) {
        textView.text = modelName
        itemView.setOnClickListener {
            clickListener(modelName)
        }
    }
}

class HeaderViewHolder(view: View): RecyclerView.ViewHolder(view) { }

class RecentSearchesAdapter(
    private val data: List<String>,
    private val clickListener: (item: String) -> Unit
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