package com.android.kubota.ui.equipment.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree

class EquipmentSearchViewModel : ViewModel() {

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mViewData =
        MutableLiveData(EquipmentTreeFilterViewData("", emptyList(), emptyList()))

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val viewData: LiveData<EquipmentTreeFilterViewData> = mViewData

    fun init(filters: List<EquipmentTreeFilter>) {
        val title = filters
            .firstNotNullOfOrNull { it as? EquipmentTreeFilter.Category }
            ?.category ?: ""

        mViewData.value = EquipmentTreeFilterViewData(title, emptyList(), filters)
        updateModelTree(filters)
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

fun EquipmentModelTree.getSuggestions(query: String): List<EquipmentModelTree> {
    return when (this) {
        is EquipmentModelTree.Model -> {
            if (this.model.model.lowercase().contains(query) || this.model.description?.lowercase()?.contains(query) == true) {
                listOf(this)
            } else {
                emptyList()
            }
        }
        is EquipmentModelTree.Category -> {
            this.items.map { it.getSuggestions(query) }
                .fold(
                    if (this.category.category.lowercase().contains(query)) {
                        listOf(this)
                    } else {
                        emptyList()
                    }
                ) { acc, next -> acc + next }
        }
    }
}

