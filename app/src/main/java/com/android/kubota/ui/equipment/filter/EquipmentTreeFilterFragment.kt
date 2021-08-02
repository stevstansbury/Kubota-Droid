package com.android.kubota.ui.equipment.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.equipment.getTopCategoryAttachmentsSize
import com.android.kubota.ui.popCurrentTabStack
import com.android.kubota.ui.resources.EquipmentModelDetailFragment
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.internal.containsCategoryWithName

sealed class EquipmentTreeFilter {
    data class AttachmentsCompatibleWith(val machineModel: String) : EquipmentTreeFilter()
    data class Category(val category: String) : EquipmentTreeFilter()
    // TODO: Discontinued Filter
}

data class EquipmentTreeFilterViewData(
    val title: String,
    val modelTree: List<EquipmentModelTree>,
    val filters: List<EquipmentTreeFilter>
)

class EquipmentTreeFilterViewModel : ViewModel() {

    private val mViewData =
        MutableLiveData(EquipmentTreeFilterViewData("", emptyList(), emptyList()))
    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)

    val viewData: LiveData<EquipmentTreeFilterViewData> = mViewData
    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError

    fun init(filters: List<EquipmentTreeFilter>) {
        val title = filters
            .firstNotNullOfOrNull { it as? EquipmentTreeFilter.Category }
            ?.category ?: ""

        mViewData.value = EquipmentTreeFilterViewData(title, emptyList(), filters)
        updateModelTree(filters)
    }

    fun addCategoryFilter(category: String) {
        val existing = viewData.value?.filters ?: emptySet()
        updateModelTree(existing + EquipmentTreeFilter.Category(category))
    }

    fun removeCategoryFilter(category: String) {
        val existing = viewData.value?.filters ?: emptySet()
        updateModelTree(existing - EquipmentTreeFilter.Category(category))
    }

    private fun updateModelTree(filters: List<EquipmentTreeFilter>) {
        mIsLoading.value = true
        val equipmentService = AppProxy.proxy.serviceManager.equipmentService

        val categoryFilters = filters
            .mapNotNull { it as? EquipmentTreeFilter.Category }
            .map { it.category }

        val compatibleWithMachineFilter = filters
            .firstNotNullOfOrNull { it as? EquipmentTreeFilter.AttachmentsCompatibleWith }
            ?.machineModel

        val untrimmedTree = when (compatibleWithMachineFilter) {
            null -> equipmentService.getEquipmentTree(
                modelFilters = emptyList(),
                categoryFilters = categoryFilters
            )
            else -> equipmentService.getEquipmentTree(
                compatibleWithModel = compatibleWithMachineFilter,
                categoryFilters = categoryFilters
            )
        }

        untrimmedTree
            .map(on = DispatchExecutor.global) { untrimmed ->
                EquipmentTreeFilterViewData(
                    title = untrimmed.getTitle(current = "root"),
                    modelTree = untrimmed.removeParentCategories(categoryFilters),
                    filters = filters
                )
            }
            .done { mViewData.value = it }
            .ensure { mIsLoading.postValue(false) }
            .catch { mError.postValue(it) }
    }

    /**
     * if the tree starts to look like a linked list, remove the parent
     * nodes until the tree has multiple branches again
     */
    private fun List<EquipmentModelTree>.removeParentCategories(
        categoryFilters: List<String>
    ): List<EquipmentModelTree> {
        if (this.size == 1 && this.first() is EquipmentModelTree.Category) {
            val categoryWrapper = this.first() as EquipmentModelTree.Category
            val categoryName = categoryWrapper.category.category

            val shouldTrim = categoryName in categoryFilters ||
                categoryWrapper.containsCategoryWithName(categoryFilters.toList()) ||
                categoryWrapper.category.parentCategory == null // skip showing top level category

            return when (shouldTrim) {
                true -> categoryWrapper.items.removeParentCategories(categoryFilters)
                false -> this
            }
        }

        return this
    }

    private tailrec fun List<EquipmentModelTree>.getTitle(current: String): String {
        if (this.size == 1 && this.first() is EquipmentModelTree.Category) {
            val categoryWrapper = this.first() as EquipmentModelTree.Category
            return categoryWrapper.items.getTitle(categoryWrapper.category.category)
        }

        return current
    }
}


class EquipmentTreeFilterFragment : BaseFragment() {
    private val viewModel: EquipmentTreeFilterViewModel by viewModels()
    override val layoutResId = R.layout.fragment_equipment_tree_filter

    private lateinit var searchHintText: TextView
    private lateinit var containerActiveFilters: FlexboxLayout
    private lateinit var containerContent: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val searchFilterCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            TODO()
        }

    companion object {
        private const val EQUIPMENT_MODEL = "EQUIPMENT_MODEL"
        private const val SELECTED_CATEGORIES = "SELECTED_CATEGORIES"

        fun instance(
            compatibleWithMachine: String?,
            selectedCategories: List<String>
        ): EquipmentTreeFilterFragment {
            return EquipmentTreeFilterFragment().apply {
                arguments = Bundle(2)
                arguments?.putString(EQUIPMENT_MODEL, compatibleWithMachine)
                arguments?.putStringArrayList(SELECTED_CATEGORIES, ArrayList(selectedCategories))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val compatibleWithMachine = arguments?.getString(EQUIPMENT_MODEL)
            ?.let { listOf(EquipmentTreeFilter.AttachmentsCompatibleWith(it)) }
            ?: emptyList()

        val categoryFilters = savedInstanceState
            ?.getStringArrayList(SELECTED_CATEGORIES)
            ?: arguments?.getStringArrayList(SELECTED_CATEGORIES)
            ?: emptyList()

        val filters = compatibleWithMachine +
            categoryFilters.map { EquipmentTreeFilter.Category(it) }

        viewModel.init(filters = filters)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.viewData.value?.filters
            ?.mapNotNull { it as? EquipmentTreeFilter.Category }
            ?.map { it.category }
            ?.let { outState.putStringArrayList(SELECTED_CATEGORIES, ArrayList(it)) }
        super.onSaveInstanceState(outState)
    }

    override fun initUi(view: View) {
        searchHintText = view.findViewById(R.id.searchHintText)
        containerActiveFilters = view.findViewById(R.id.container_active_filters)
        containerContent = view.findViewById(R.id.container_content)

        setupBottomSheet(view)

        view.findViewById<CardView>(R.id.container_search).setOnClickListener {
            // TODO
            // val intent = Intent(
            //     requireContext(),
            //     SomeSearchActivity::class.java
            // )
            // searchFilterCallback.launch(intent)
        }
    }

    override fun loadData() {
        viewModel.viewData.observe(this) { treeData ->
            if (treeData.title == "root") {
                this.hideBlockingActivityIndicator()
                activity?.popCurrentTabStack()
                return@observe
            }

            activity?.title = treeData.title
            searchHintText.text = getString(R.string.search_hint, treeData.title)

            displayFilters(treeData.filters)
            displayModelTree(treeData.modelTree)
        }

        viewModel.isLoading.observe(this, { loading ->
            when (loading) {
                true -> this.showBlockingActivityIndicator()
                else -> this.hideBlockingActivityIndicator()
            }
        })

        viewModel.error.observe(this, { error ->
            error?.let { this.showError(it) }
        })
    }

    private fun displayFilters(filters: List<EquipmentTreeFilter>) {
        containerActiveFilters.removeAllViews()
        filters.forEach {
            when (it) {
                is EquipmentTreeFilter.AttachmentsCompatibleWith -> addActiveFilterView(
                    filter = requireContext().getString(
                        R.string.attachments_model_compatible,
                        it.machineModel
                    ),
                    onClick = null
                )
                is EquipmentTreeFilter.Category -> addActiveFilterView(
                    filter = it.category,
                    onClick = { filter -> viewModel.removeCategoryFilter(filter) }
                )
            }
        }
    }

    private fun displayModelTree(tree: List<EquipmentModelTree>) {
        val adapter = EquipmentTreeAdapter(
            items = tree,
            onCategoryClicked = {
                viewModel.addCategoryFilter(it.category.category)
            },
            onModelClicked = {
                flowActivity?.addFragmentToBackStack(
                    fragment = EquipmentModelDetailFragment.instance(it.model)
                )
            })

        containerContent.adapter = adapter
    }

    private fun addActiveFilterView(filter: String, onClick: ((filter: String) -> Unit)?) {
        val item = layoutInflater.inflate(
            R.layout.item_attachment_active_filter,
            containerActiveFilters,
            false
        )

        if (onClick == null) {
            item.findViewById<View>(R.id.xButton).visibility = View.GONE
        }

        item.findViewById<TextView>(R.id.tv_active_filter).text = filter
        item.setOnClickListener {
            onClick?.invoke(filter)
        }

        containerActiveFilters.addView(item)
    }

    private fun setupBottomSheet(view: View) {
        val filtersBottomSheet = view.findViewById<FrameLayout>(R.id.bottom_sheet_filters)
        bottomSheetBehavior = BottomSheetBehavior.from(filtersBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        view.findViewById<View>(R.id.btn_filters).setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED,
                BottomSheetBehavior.STATE_HIDDEN -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                else -> Unit
            }
        }
    }
}

private class EquipmentTreeAdapter(
    private val items: List<EquipmentModelTree>,
    private val onCategoryClicked: (item: EquipmentModelTree.Category) -> Unit,
    private val onModelClicked: (item: EquipmentModelTree.Model) -> Unit
) : RecyclerView.Adapter<EquipmentTreeViewHolder>() {

    enum class Type {
        Category,
        Model
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentTreeViewHolder {
        return if (viewType == Type.Category.ordinal) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment_category, parent, false)

            EquipmentCategoryViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment, parent, false)

            EquipmentModelViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: EquipmentTreeViewHolder, position: Int) {
        if (holder is EquipmentCategoryViewHolder) {
            holder.bind(item = items[position] as EquipmentModelTree.Category,
                onCategoryClicked = { onCategoryClicked(it) }
            )
        } else {
            (holder as EquipmentModelViewHolder).bind(
                item = items[position] as EquipmentModelTree.Model,
                onModelClicked = { onModelClicked(it) }
            )
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is EquipmentModelTree.Category) {
            Type.Category.ordinal
        } else {
            Type.Model.ordinal
        }
    }
}

sealed class EquipmentTreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

private class EquipmentCategoryViewHolder(itemView: View) : EquipmentTreeViewHolder(itemView) {

    private val tvTitle: TextView = itemView.findViewById(R.id.tv_category_title)
    private val tvSize: TextView = itemView.findViewById(R.id.tv_category_size)

    fun bind(
        item: EquipmentModelTree.Category,
        onCategoryClicked: (item: EquipmentModelTree.Category) -> Unit
    ) {
        tvTitle.text = item.category.category
        tvSize.text = item.getTopCategoryAttachmentsSize().toString()

        itemView.setOnClickListener {
            onCategoryClicked(item)
        }
    }
}

private class EquipmentModelViewHolder(itemView: View) : EquipmentTreeViewHolder(itemView) {

    private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
    private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
    private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)

    fun bind(
        item: EquipmentModelTree.Model,
        onModelClicked: (item: EquipmentModelTree.Model) -> Unit
    ) {
        tvTitle.text = item.model.model
        tvDescription.text = item.model.description
        ivIcon.setImageResource(R.drawable.ic_construction_category_thumbnail)

        val imageUrl = item.model.imageResources?.fullUrl ?: item.model.imageResources?.iconUrl
        if (imageUrl != null) {
            AppProxy.proxy.serviceManager.contentService
                .getBitmap(url = imageUrl)
                .done { bitmap ->
                    when (bitmap) {
                        null -> ivIcon.setImageResource(R.drawable.ic_construction_category_thumbnail)
                        else -> ivIcon.setImageBitmap(bitmap)
                    }
                }
        }

        itemView.setOnClickListener {
            onModelClicked(item)
        }
    }
}
