package com.android.kubota.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.android.kubota.R
import com.android.kubota.ui.ItemDivider
import com.android.kubota.utility.CategoryUtils
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.EquipmentCategory
import com.kubota.repository.uimodel.KubotaModelSubCategory
import kotlinx.coroutines.launch

class EquipmentSubCategoryFragment: BaseResourcesListFragment() {

    private lateinit var equipmentCategory: EquipmentCategory
    private var viewMode = SUB_CATEGORIES_MODE
    override val layoutResId: Int = R.layout.fragment_subcategories

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        arguments?.let {bundle ->
            viewMode = bundle.getInt(DISPLAY_MODE_KEY, SUB_CATEGORIES_MODE)

            bundle.getString(CATEGORY_KEY)
                ?.let {
                    CategoryUtils.CATEGORY_MAP[it]
                }
                ?.let {category ->
                    equipmentCategory = category
                    loadData()
                }
                ?: activity?.onBackPressed()
        } ?: activity?.onBackPressed()

        return view
    }

    override fun initUi(view: View) {
        super.initUi(view)
        recyclerView.addItemDecoration(
            ItemDivider(requireContext(), R.drawable.divider)
        )
    }

    override fun loadData() {
        when(viewMode) {
            SUB_CATEGORIES_MODE -> loadSubCategoryData(equipmentCategory)
            MODELS_MODE -> loadModelData(equipmentCategory)
        }
    }

    private fun loadModelData(category: EquipmentCategory) {
        activity?.title = getString(CategoryUtils.getEquipmentName(category))
        viewLifecycleOwner.lifecycleScope.launch {
            refreshLayout.isRefreshing = true
            when (val results = viewModel.loadModels(category)) {
                is CategorySyncResults.Success -> {
                    recyclerView.adapter =
                        SubCategoriesAdapter(
                            results.results
                        ) {

                        }
                }
                is CategorySyncResults.ServerError -> flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
                is CategorySyncResults.IOException -> flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
            }
            refreshLayout.isRefreshing = false
        }
    }

    private fun loadSubCategoryData(category: EquipmentCategory) {
        activity?.title = getString(CategoryUtils.getEquipmentName(category))
        viewLifecycleOwner.lifecycleScope.launch {
            refreshLayout.isRefreshing = true
            when (val results = viewModel.loadSubCategories(category)) {
                is CategorySyncResults.Success -> {
                    recyclerView.adapter =
                        SubCategoriesAdapter(
                            results.results
                        ) {
                            flowActivity?.addFragmentToBackStack(createModelInstance(it.category))
                        }
                }
                is CategorySyncResults.ServerError -> flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
                is CategorySyncResults.IOException -> flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
            }
            refreshLayout.isRefreshing = false
        }
    }

    companion object {
        private const val DISPLAY_MODE_KEY = "display_mode"
        private const val SUB_CATEGORIES_MODE = 0
        private const val MODELS_MODE = 1
        private const val CATEGORY_KEY = "equipment_category"

        fun createSubCategoryInstance(category: EquipmentCategory): EquipmentSubCategoryFragment {
            return createInstance(SUB_CATEGORIES_MODE, category)
        }

        fun createModelInstance(category: EquipmentCategory): EquipmentSubCategoryFragment {
            return createInstance(MODELS_MODE, category)
        }

        private fun createInstance(mode: Int, category: EquipmentCategory): EquipmentSubCategoryFragment {
            return EquipmentSubCategoryFragment()
                .apply {
                    arguments = Bundle(2).apply {
                        putInt(DISPLAY_MODE_KEY, mode)
                        putString(CATEGORY_KEY, category.toString())
                    }
                }
        }
    }
}

class SubCategoryViewHolder(view: View): BaseResourcesViewHolder<KubotaModelSubCategory>(view) {

    override fun bind(data: KubotaModelSubCategory, clickListener: (category: KubotaModelSubCategory) -> Unit) {
        super.bind(data, clickListener)
        title.text = data.title
        CategoryUtils.getEquipmentImage(data.category).let {
            if (it != 0) {
                image.setImageResource(it)
            }
        }
    }
}

class SubCategoriesAdapter(
    data: List<KubotaModelSubCategory>,
    clickListener: (item: KubotaModelSubCategory) -> Unit
): BaseResourcesAdapter<KubotaModelSubCategory>(data, clickListener) {

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