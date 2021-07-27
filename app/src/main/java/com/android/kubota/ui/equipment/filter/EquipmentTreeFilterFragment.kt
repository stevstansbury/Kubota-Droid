package com.android.kubota.ui.equipment.filter

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.android.kubota.ui.equipment.getTopCategoryAttachmentsSize
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

data class TreeData(
    val modelTree: List<EquipmentModelTree>,
    val categoryFilters: Set<String>
)

class EquipmentTreeFilterViewModel : ViewModel() {

    private var model: String? = null
    private val categoryFilters = mutableSetOf<String>()
    private val mModelTreeData = MutableLiveData(TreeData(emptyList(), emptySet()))
    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)

    val modelTreeData: LiveData<TreeData> = mModelTreeData
    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError


    fun init(model: String?, categoryFilters: Set<String>) {
        this.model = model
        this.categoryFilters.addAll(categoryFilters)
        updateModelTree()
    }

    fun addCategoryFilter(category: String) {
        categoryFilters.add(category)
        updateModelTree()
    }

    fun removeCategoryFilter(category: String) {
        categoryFilters.remove(category)
        updateModelTree()
    }

    private fun updateModelTree() {
        val equipmentService = AppProxy.proxy.serviceManager.equipmentService

        mIsLoading.value = true
        val untrimmedTree = when (val model = model) {
            null -> equipmentService.getEquipmentTree(
                modelFilters = emptyList(),
                categoryFilters = categoryFilters.toList()
            )
            else -> equipmentService.getEquipmentTree(
                compatibleWithModel = model,
                categoryFilters = categoryFilters.toList()
            )
        }

        untrimmedTree
            .map(on = DispatchExecutor.global) {
                TreeData(it.removeParentCategories(), categoryFilters)
            }
            .done { mModelTreeData.value = it }
            .ensure { mIsLoading.postValue(false) }
            .catch { mError.postValue(it) }
    }

    /**
     * if the tree starts to look like a linked list, remove the parent
     * nodes until the tree has multiple branches again
     */
    private fun List<EquipmentModelTree>.removeParentCategories(): List<EquipmentModelTree> {
        if (this.size == 1 && this.first() is EquipmentModelTree.Category) {
            val categoryWrapper = this.first() as EquipmentModelTree.Category
            val categoryName = categoryWrapper.category.category

            val shouldTrim = categoryName in categoryFilters ||
                categoryWrapper.containsCategoryWithName(categoryFilters.toList()) ||
                categoryWrapper.category.parentCategory == null // skip showing top level category

            return when (shouldTrim) {
                true -> categoryWrapper.items.removeParentCategories()
                false -> this
            }
        }

        return this
    }
}


class EquipmentTreeFilterFragment : BaseFragment() {
    private val viewModel: EquipmentTreeFilterViewModel by viewModels()
    override val layoutResId = R.layout.fragment_equipment_tree_filter

    private lateinit var containerActiveFilters: FlexboxLayout
    private lateinit var containerContent: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val searchFilterCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            TODO()
        }

    companion object {
        private const val EQUIPMENT_MODEL = "EQUIPMENT_MODEL"
        private const val SELECTED_CATEGORIES = "SELECTED_CATEGORIES"

        fun instance(
            model: String?,
            selectedCategories: List<String>
        ): EquipmentTreeFilterFragment {
            return EquipmentTreeFilterFragment().apply {
                arguments = Bundle(2)
                arguments?.putString(EQUIPMENT_MODEL, model)
                arguments?.putStringArrayList(SELECTED_CATEGORIES, ArrayList(selectedCategories))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.title = getString(R.string.attachments_title)

        val categoryFilters = savedInstanceState
            ?.getStringArrayList(SELECTED_CATEGORIES)?.toSet()
            ?: arguments?.getStringArrayList(SELECTED_CATEGORIES)?.toSet()
            ?: emptySet()

        viewModel.init(
            model = arguments?.getString(EQUIPMENT_MODEL),
            categoryFilters = categoryFilters,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.modelTreeData.value?.categoryFilters?.toList()?.let {
            outState.putStringArrayList(SELECTED_CATEGORIES, ArrayList(it))
        }
        super.onSaveInstanceState(outState)
    }

    override fun initUi(view: View) {
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
        viewModel.modelTreeData.observe(this) { treeData ->
            containerContent.removeAllViews()
            displayModelTree(treeData.modelTree)

            containerActiveFilters.removeAllViews()
            addCompatibleWithFilterView()
            treeData.categoryFilters.forEach {
                addActiveCategoryFilterView(
                    filter = it,
                    onClick = { filter -> viewModel.removeCategoryFilter(filter) }
                )
            }
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

    private fun displayModelTree(tree: List<EquipmentModelTree>) {
        tree.forEach {
            when (it) {
                is EquipmentModelTree.Category -> addAttachmentCategory(it)
                is EquipmentModelTree.Model -> addEquipmentModel(it)
            }
        }
    }

    private fun addAttachmentCategory(categoryWrapper: EquipmentModelTree.Category) {
        val item = layoutInflater.inflate(
            R.layout.item_attachment_category,
            containerContent,
            false
        )

        val categoryName = categoryWrapper.category.category
        item.findViewById<TextView>(R.id.tv_category_title).text = categoryName
        item.findViewById<TextView>(R.id.tv_category_size).text = categoryWrapper
            .getTopCategoryAttachmentsSize().toString()

        item.setOnClickListener {
            viewModel.addCategoryFilter(categoryName)
        }

        containerContent.addView(item)
    }

    private fun addEquipmentModel(wrapper: EquipmentModelTree.Model) {
        val item = layoutInflater.inflate(
            R.layout.item_attachment,
            containerContent,
            false
        )

        val image = item.findViewById<ImageView>(R.id.iv_icon)

        val title = wrapper.model.model
        val description = wrapper.model.description
        val imageUrl = wrapper.model.imageResources?.fullUrl
            ?: wrapper.model.imageResources?.iconUrl

        item.findViewById<TextView>(R.id.tv_title).text = title
        item.findViewById<TextView>(R.id.tv_description).text = description
        image.setImageResource(R.drawable.ic_construction_category_thumbnail)

        if (imageUrl != null) {
            AppProxy.proxy.serviceManager.contentService
                .getBitmap(url = imageUrl)
                .done { bitmap ->
                    when (bitmap) {
                        null -> image.setImageResource(R.drawable.ic_construction_category_thumbnail)
                        else -> image.setImageBitmap(bitmap)
                    }
                }
        }

        item.setOnClickListener {
            flowActivity?.addFragmentToBackStack(
                fragment = EquipmentModelDetailFragment.instance(wrapper.model)
            )
        }

        containerContent.addView(item)
    }

    private fun addCompatibleWithFilterView() {
        arguments?.getString(EQUIPMENT_MODEL)?.let {
            val filter = requireContext().getString(R.string.attachments_model_compatible, it)
            addActiveCategoryFilterView(filter = filter, onClick = null)
        }
    }

    private fun addActiveCategoryFilterView(filter: String, onClick: ((filter: String) -> Unit)?) {
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