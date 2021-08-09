package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.api.EquipmentService
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.internal.containsCategoryWithName

sealed class EquipmentTreeFilter {
    data class AttachmentsCompatibleWith(val machineModel: String) : EquipmentTreeFilter()
    data class MachinesCompatibleWith(val attachmentModel: String) : EquipmentTreeFilter()
    data class Category(val category: String) : EquipmentTreeFilter()
    // TODO: Discontinued Filter
}

fun EquipmentService.getEquipmentTree(
    filter: EquipmentTreeFilter.AttachmentsCompatibleWith,
    categories: List<String>
): Promise<List<EquipmentModelTree>> = this
    .getModel(filter.machineModel)
    .thenMap { model ->
        model ?: return@thenMap Promise.value(emptyList())
        this.getEquipmentTree(
            modelFilters = model.compatibleAttachments,
            categoryFilters = categories
        )
    }

fun EquipmentService.getEquipmentTree(
    filter: EquipmentTreeFilter.MachinesCompatibleWith,
    categories: List<String>
): Promise<List<EquipmentModelTree>> = this
    .getCompatibleMachines(filter.attachmentModel)
    .thenMap { models ->
        this.getEquipmentTree(
            modelFilters = models.map { it.model },
            categoryFilters = categories
        )
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

    fun init(compatibleWith: String?, filters: List<EquipmentTreeFilter>) {
        val title = filters
            .firstNotNullOfOrNull { it as? EquipmentTreeFilter.Category }
            ?.category ?: ""

        val firstFilter = compatibleWith?.let { modelName ->
            AppProxy.proxy.serviceManager.equipmentService.getModel(modelName).map { model ->
                model ?: return@map emptyList()
                when (model.type) {
                    EquipmentModel.Type.Machine ->
                        listOf(EquipmentTreeFilter.AttachmentsCompatibleWith(model.model))
                    EquipmentModel.Type.Attachment ->
                        listOf(EquipmentTreeFilter.MachinesCompatibleWith(model.model))
                }
            }
        } ?: Promise.value(emptyList())

        firstFilter.done {
            val newFilters = it + filters
            mViewData.value = EquipmentTreeFilterViewData(title, emptyList(), newFilters)
            updateModelTree(newFilters)
        }
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

        val compatibleWithAttachmentFilter = filters
            .firstNotNullOfOrNull { it as? EquipmentTreeFilter.MachinesCompatibleWith }

        val untrimmedTree: Promise<List<EquipmentModelTree>> = when {
            compatibleWithMachineFilter != null -> equipmentService
                .getEquipmentTree(compatibleWithMachineFilter, categoryFilters)
            compatibleWithAttachmentFilter != null -> equipmentService
                .getEquipmentTree(compatibleWithAttachmentFilter, categoryFilters)
            else -> equipmentService.getEquipmentTree(emptyList(), categoryFilters)
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
}

tailrec fun List<EquipmentModelTree>.getTitle(current: String): String {
    if (this.size == 1 && this.first() is EquipmentModelTree.Category) {
        val categoryWrapper = this.first() as EquipmentModelTree.Category
        return categoryWrapper.items.getTitle(categoryWrapper.category.category)
    }

    return current
}

/**
 * if the tree starts to look like a linked list, remove the parent
 * nodes until the tree has multiple branches again
 */
fun List<EquipmentModelTree>.removeParentCategories(
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