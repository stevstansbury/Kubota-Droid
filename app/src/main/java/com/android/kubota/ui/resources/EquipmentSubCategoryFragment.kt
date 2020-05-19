package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.kubota.R
import com.android.kubota.extensions.equipmentImageResId
import com.android.kubota.viewmodel.resources.EquipmentSubCategoriesViewModel
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel

class EquipmentSubCategoryFragment: BaseResourcesListFragment() {

    companion object {
        private const val DISPLAY_MODE = "DISPLAY_MODE"
        const val SUB_CATEGORIES_VIEW_MODE = 0
        const val MODELS_VIEW_MODE = 1
        private const val PARENT_CATEGORY = "PARENT_CATEGORY"
        private const val PARENT_CATEGORY_TITLE = "PARENT_CATEGORY_TITLE"

        fun instance(parentCategory: EquipmentCategory): EquipmentSubCategoryFragment {
            return EquipmentSubCategoryFragment()
                        .apply {
                            arguments = Bundle(1).apply {
                                putInt(DISPLAY_MODE, if (parentCategory.hasSubCategories) SUB_CATEGORIES_VIEW_MODE else MODELS_VIEW_MODE)
                                putString(PARENT_CATEGORY, parentCategory.category)
                                putString(PARENT_CATEGORY_TITLE, parentCategory.title)
                            }
                        }
        }
    }

    private lateinit var parentCategory: String
    private lateinit var parentCategoryTitle: String
    private var viewMode = SUB_CATEGORIES_VIEW_MODE
    override val layoutResId: Int = R.layout.fragment_subcategories

    private val viewModel: EquipmentSubCategoriesViewModel by lazy {
        EquipmentSubCategoriesViewModel.instance(
            owner = this,
            parentCategory = this.parentCategory,
            viewMode = this.viewMode
        )
    }

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.let { bundle ->
            viewMode = bundle.getInt(DISPLAY_MODE, SUB_CATEGORIES_VIEW_MODE)
            parentCategory = bundle.getString(PARENT_CATEGORY) ?: return false
            parentCategoryTitle = bundle.getString(PARENT_CATEGORY_TITLE) ?: return false
            return true
        } ?: false
    }

    override fun initUi(view: View) {
        super.initUi(view)
        activity?.title = this.parentCategoryTitle
        recyclerView.addItemDecoration( DividerItemDecoration(context, DividerItemDecoration.VERTICAL) )
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

        when(this.viewMode) {
            SUB_CATEGORIES_VIEW_MODE ->
                this.viewModel.equipmentCategories.observe(viewLifecycleOwner, Observer { categories ->
                    recyclerView.adapter = SubCategoriesAdapter(categories) { onSelectCategory(it) }
                })
            MODELS_VIEW_MODE ->
                this.viewModel.equipmentModels.observe(viewLifecycleOwner, Observer { models ->
                    recyclerView.adapter = ModelAdapter(models) { onSelectModel(it) }
                })
        }
    }

    private fun onSelectCategory(category: EquipmentCategory) {
        flowActivity?.addFragmentToBackStack(EquipmentSubCategoryFragment.instance(category))
    }

    private fun onSelectModel(model: EquipmentModel) {
        flowActivity?.addFragmentToBackStack(EquipmentModelDetailFragment.instance(model))
    }

}

class SubCategoryViewHolder(view: View): BaseResourcesViewHolder<EquipmentCategory>(view) {

    override fun bind(data: EquipmentCategory, clickListener: (category: EquipmentCategory) -> Unit) {
        super.bind(data, clickListener)
        title.text = data.title
        data.equipmentImageResId?.let { image.setImageResource(it) }
    }

}

class SubCategoriesAdapter(
    data: List<EquipmentCategory>,
    clickListener: (item: EquipmentCategory) -> Unit
): BaseResourcesAdapter<EquipmentCategory>(data, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.view_subcategory_model,
                parent,
                false
            )
        return SubCategoryViewHolder(view)
    }
}

class ModelViewHolder(view: View): BaseResourcesViewHolder<EquipmentModel>(view) {

    override fun bind(data: EquipmentModel, clickListener: (category: EquipmentModel) -> Unit) {
        super.bind(data, clickListener)
        title.text = data.model
        data.equipmentImageResId?.let { image.setImageResource(it) }
    }

}

class ModelAdapter(
    data: List<EquipmentModel>,
    clickListener: (item: EquipmentModel) -> Unit
): BaseResourcesAdapter<EquipmentModel>(data, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.view_subcategory_model,
                parent,
                false
            )
        return ModelViewHolder(view)
    }
}