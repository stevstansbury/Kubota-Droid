package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.displayNameStringRes
import com.android.kubota.extensions.equipmentImageResId
import com.android.kubota.extensions.toEquipmentModel
import com.android.kubota.ui.notification.NotificationMenuController
import com.android.kubota.ui.notification.NotificationTabFragment
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.viewmodel.resources.EquipmentCategoriesViewModel
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.RecentViewedItem

class CategoriesFragment: BaseResourcesListFragment() {

    override val layoutResId: Int = R.layout.fragment_categories
    private lateinit var recentSearchesRecyclerView: RecyclerView
    private val menuController: NotificationMenuController by lazy {
        NotificationMenuController(requireActivity())
    }

    private val viewModel: EquipmentCategoriesViewModel by lazy {
        EquipmentCategoriesViewModel.instance(owner = this.requireActivity())
    }

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
        super.initUi(view)

        recentSearchesRecyclerView = view.findViewById<RecyclerView>(R.id.recentSearches).apply {
            setHasFixedSize(true)
        }
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    override fun loadData() {
        this.viewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            when {
                loading > 0 -> this.showProgressBar()
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

        this.viewModel.unreadNotifications.observe(this, menuController.unreadNotificationsObserver)

        this.viewModel.loadUnreadNotification(activity as? AuthDelegate)
        this.viewModel.updateRecentViewed()
    }

    private fun onSelectCategory(category: EquipmentCategory) {
        flowActivity?.addFragmentToBackStack(EquipmentSubCategoryFragment.instance(category))
    }

    private fun onSelectRecentlyViewed(item: RecentViewedItem) {
        val model = item.toEquipmentModel() ?: return
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

        data.imageResources?.iconUrl?.let { url ->
            AppProxy.proxy.serviceManager.contentService
                    .getBitmap(url)
                    .done { bitmap ->
                        bitmap?.let { image.setImageBitmap(it) }
                    }
        }
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