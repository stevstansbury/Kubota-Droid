package com.android.kubota.viewmodel.equipment

import android.os.Parcelable
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
import kotlinx.android.parcel.Parcelize

sealed class EquipmentTreeFilter : Parcelable {
    @Parcelize
    data class AttachmentsCompatibleWith(val machineModel: String) : EquipmentTreeFilter()

    @Parcelize
    data class MachinesCompatibleWith(val attachmentModel: String) : EquipmentTreeFilter()

    @Parcelize
    data class Category(val category: String) : EquipmentTreeFilter()

    @Parcelize
    object Discontinued : EquipmentTreeFilter()
}

fun EquipmentService.getEquipmentTree(filters: List<EquipmentTreeFilter>): Promise<List<EquipmentModelTree>> {

    data class FilterAggregator(
        val models: Set<String>,
        val categories: Set<String>
    )

    val filterPromises = filters.map { filter ->
        when (filter) {
            is EquipmentTreeFilter.AttachmentsCompatibleWith -> this
                .getModel(filter.machineModel)
                .map {
                    FilterAggregator(
                        models = it?.compatibleAttachments?.toSet() ?: emptySet(),
                        categories = emptySet()
                    )
                }
            is EquipmentTreeFilter.MachinesCompatibleWith -> this
                .getCompatibleMachines(filter.attachmentModel)
                .map { models ->
                    FilterAggregator(
                        models = models.map { it.model }.toSet(),
                        categories = emptySet()
                    )
                }
            is EquipmentTreeFilter.Category -> Promise.value(
                FilterAggregator(models = emptySet(), categories = setOf(filter.category))
            )
            is EquipmentTreeFilter.Discontinued -> this.getAvailableModels().map { models ->
                FilterAggregator(
                    models = models.map { it.model }.toSet(),
                    categories = emptySet()
                )
            }
        }
    }

    return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
        val waited = filterPromises.map { it.wait() }

        val modelFilters = waited
            .filter { it.models.isNotEmpty() }
            .let {
                val initial = it.firstOrNull()?.models ?: emptySet()
                it.fold(initial) { acc, next -> acc.intersect(next.models) }
            }
            .toList()

        val categoryFilters = waited
            .fold(setOf<String>()) { acc, next -> acc.union(next.categories) }
            .toList()

        this.getEquipmentTree(
            modelFilters = modelFilters,
            categoryFilters = categoryFilters
        )
    }
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

    fun addFilter(filter: EquipmentTreeFilter) {
        val existing = viewData.value?.filters ?: emptySet()
        updateModelTree( existing + filter)
    }

    fun removeFilter(filter: EquipmentTreeFilter) {
        val existing = viewData.value?.filters ?: emptySet()
        updateModelTree( existing - filter)
    }

    private fun updateModelTree(filters: List<EquipmentTreeFilter>) {
        mIsLoading.value = true
        val equipmentService = AppProxy.proxy.serviceManager.equipmentService

        val categoryFilters = filters
            .mapNotNull { it as? EquipmentTreeFilter.Category }
            .map { it.category }

        val untrimmedTree: Promise<List<EquipmentModelTree>> =
            equipmentService.getEquipmentTree(filters)

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