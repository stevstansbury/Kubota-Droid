package com.android.kubota.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
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
