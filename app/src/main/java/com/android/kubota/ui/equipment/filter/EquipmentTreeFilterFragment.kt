package com.android.kubota.ui.equipment.filter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.equipment.getTopCategoryAttachmentsSize
import com.android.kubota.ui.popCurrentTabStack
import com.android.kubota.ui.resources.EquipmentModelDetailFragment
import com.android.kubota.viewmodel.equipment.EquipmentTreeFilter
import com.android.kubota.viewmodel.equipment.EquipmentTreeFilterViewModel
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel

class EquipmentTreeFilterFragment : BaseFragment(), BottomSheetDelegate {
    private val viewModel: EquipmentTreeFilterViewModel by viewModels()
    override val layoutResId = R.layout.fragment_equipment_tree_filter

    private lateinit var searchHintText: TextView
    private lateinit var containerActiveFilters: FlexboxLayout
    private lateinit var containerContent: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var bgOverlay: View

    private val searchFilterCallback = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val result = it.data
                ?.getParcelableExtra(EquipmentSearchActivity.KEY_SEARCH_RESULT) as Any

            when (result) {
                is EquipmentCategory -> viewModel.addFilter(EquipmentTreeFilter.Category(result.category))
                is EquipmentModel -> flowActivity?.addFragmentToBackStack(
                    fragment = EquipmentModelDetailFragment.instance(result)
                )
                else -> Unit
            }
        }
    }

    companion object {
        const val EQUIPMENT_MODEL = "EQUIPMENT_MODEL"
        const val CATEGORY_ARGS = "CATEGORY_ARGS"
        const val SELECTED_FILTERS = "SELECTED_FILTERS"

        fun instance(
            compatibleWithModel: String?,
            selectedCategories: List<String>
        ): EquipmentTreeFilterFragment {
            return EquipmentTreeFilterFragment().apply {
                arguments = bundleOf(
                    EQUIPMENT_MODEL to compatibleWithModel,
                    CATEGORY_ARGS to ArrayList(selectedCategories)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedFilters = savedInstanceState
            ?.getParcelableArrayList<EquipmentTreeFilter>(SELECTED_FILTERS)

        when (savedFilters) {
            null -> viewModel.init(
                compatibleWith = arguments?.getString(EQUIPMENT_MODEL),
                filters = (arguments?.getStringArrayList(CATEGORY_ARGS)
                    ?.map { EquipmentTreeFilter.Category(it) }
                    ?: emptyList())
            )
            else -> viewModel.init(
                compatibleWith = null, // not used when resuming
                filters = savedFilters
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.viewData.value?.filters?.let {
            outState.putParcelableArrayList(
                SELECTED_FILTERS,
                ArrayList(it)
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun initUi(view: View) {
        searchHintText = view.findViewById(R.id.searchHintText)
        containerActiveFilters = view.findViewById(R.id.container_active_filters)
        containerContent = view.findViewById(R.id.container_content)
        bgOverlay = view.findViewById(R.id.bg_overlay)

        setupBottomSheet(view)

        view.findViewById<CardView>(R.id.container_search).setOnClickListener {
            val intent = Intent(
                requireContext(),
                EquipmentSearchActivity::class.java
            ).apply {
                putParcelableArrayListExtra(
                    SELECTED_FILTERS,
                    ArrayList(viewModel.viewData.value?.filters ?: emptyList())
                )
            }
            searchFilterCallback.launch(intent)
        }

        bgOverlay.setOnClickListener {
            closeBottomSheet()
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

            loadFilters()
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
                is EquipmentTreeFilter.MachinesCompatibleWith -> addActiveFilterView(
                    filter = requireContext().getString(
                        R.string.model_compatible_with,
                        it.attachmentModel
                    ),
                    onClick = null
                )
                is EquipmentTreeFilter.AttachmentsCompatibleWith -> addActiveFilterView(
                    filter = requireContext().getString(
                        R.string.model_compatible_with,
                        it.machineModel
                    ),
                    onClick = null
                )
                is EquipmentTreeFilter.Category -> addActiveFilterView(
                    filter = it.category,
                    onClick = { viewModel.removeFilter(it) }
                )
                is EquipmentTreeFilter.Discontinued -> {
                    addActiveFilterView(filter = getString(R.string.equipment_tree_filter_discontinued),
                        onClick = { viewModel.removeFilter(it) }
                    )
                }
            }
        }
    }

    private fun displayModelTree(tree: List<EquipmentModelTree>) {
        val adapter = EquipmentTreeAdapter(
            items = tree,
            onCategoryClicked = {
                viewModel.addFilter(EquipmentTreeFilter.Category(it.category.category))
            },
            onModelClicked = {
                flowActivity?.addFragmentToBackStack(
                    fragment = EquipmentModelDetailFragment.instance(it.model)
                )
            })

        containerContent.adapter = adapter
    }

    private fun addActiveFilterView(filter: String, onClick: (() -> Unit)?) {
        val item = layoutInflater.inflate(
            R.layout.view_equipment_tree_active_filter,
            containerActiveFilters,
            false
        )

        if (onClick == null) {
            item.findViewById<View>(R.id.xButton).visibility = View.GONE
        }

        item.findViewById<TextView>(R.id.tv_active_filter).text = filter
        item.setOnClickListener {
            onClick?.invoke()
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
                    closeBottomSheet()
                }
                else -> Unit
            }
        }
    }

    private fun loadFilters() {
        val fragment = EquipmentFiltersFragment.instance(viewModel.viewData.value?.filters)
        fragment.delegate = this

        childFragmentManager.beginTransaction()
            .replace(R.id.bottom_sheet_filters, fragment)
            .commit()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> closeBottomSheet()
                    else -> Unit
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bgOverlay.isVisible = true
                bgOverlay.alpha = slideOffset
            }
        })
    }

    override fun closeBottomSheet() {
        bgOverlay.isVisible = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onFilterClicked(item: EquipmentTreeFilter) {
        viewModel.addFilter(item)
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
                .inflate(R.layout.view_equipment_tree_category, parent, false)

            EquipmentCategoryViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_equipment_tree_item, parent, false)

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
    private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)

    fun bind(
        item: EquipmentModelTree.Category,
        onCategoryClicked: (item: EquipmentModelTree.Category) -> Unit
    ) {
        tvTitle.text = item.category.category

        @SuppressLint("SetTextI18n")
        tvSize.text = "(${item.getTopCategoryAttachmentsSize()})"

        ivIcon.setImageResource(R.drawable.ic_construction_category_thumbnail)
        val imageUrl =
            item.category.imageResources?.fullUrl ?: item.category.imageResources?.iconUrl
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

interface BottomSheetDelegate {
    fun closeBottomSheet()
    fun onFilterClicked(item: EquipmentTreeFilter)
}