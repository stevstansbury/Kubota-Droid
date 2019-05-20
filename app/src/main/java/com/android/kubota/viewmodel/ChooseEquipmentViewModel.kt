package com.android.kubota.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import com.android.kubota.R
import com.android.kubota.utility.Utils
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults

class ChooseEquipmentViewModel(private val categoryService: CategoryModelService): ViewModel() {

    val categories: MutableLiveData<Map<String, List<String>>> by lazy {
        loadCategories()
        MutableLiveData<Map<String, List<String>>>()
    }

    val isLoading = MutableLiveData<Boolean>()

    val serverError = MutableLiveData<Boolean>()

    fun search(query: String): List<EquipmentUIModel> {
        return categories.value?.entries?.flatMap { (key, entry) ->
            val categoryResId = when(key) {
                "Construction" -> R.string.equipment_construction_category
                "Mowers" -> R.string.equipment_mowers_category
                "Tractors" -> R.string.equipment_tractors_category
                else -> R.string.equipment_utv_category
            }
            val imageResId = when(key) {
                "Construction" -> R.drawable.ic_construction_category_thumbnail
                "Mowers" -> R.drawable.ic_mower_category_thumbnail
                "Tractors" -> R.drawable.ic_tractor_category_thumbnail
                else -> R.drawable.ic_utv_category_thumbnail
            }

            entry.mapNotNull { model ->
                if (model.contains(query, ignoreCase = true)) {
                    EquipmentUIModel(id = 1, name = model, categoryResId = categoryResId, imageResId = imageResId)
                } else {
                    null
                }
            }

        } ?: emptyList()
    }

    private fun loadCategories() {
        Utils.backgroundTask {
            isLoading.postValue(true)
            serverError.postValue(false)
            when (val result = categoryService.getCategories()) {
                is CategorySyncResults.Success -> {
                    // TODO: Map to use EquipmentUIModel
                    categories.postValue(result.results)
                }
                is CategorySyncResults.ServerError,
                is CategorySyncResults.IOException -> serverError.postValue(true)
            }
            isLoading.postValue(false)
        }
    }
}

data class EquipmentUIModel(val id: Int, val name: String, val categoryResId: Int, val imageResId: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        id =parcel.readInt(),
        name = parcel.readString() ?: "",
        categoryResId =parcel.readInt(),
        imageResId = parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(categoryResId)
        parcel.writeInt(imageResId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EquipmentUIModel> {
        override fun createFromParcel(parcel: Parcel): EquipmentUIModel {
            return EquipmentUIModel(parcel)
        }

        override fun newArray(size: Int): Array<EquipmentUIModel?> {
            return arrayOfNulls(size)
        }
    }
}
