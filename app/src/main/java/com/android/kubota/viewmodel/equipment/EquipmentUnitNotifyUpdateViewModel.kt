package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

class EquipmentUnitNotifyUpdateViewModelFactory
    : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitNotifyUpdateViewModel() as T
    }

}

class EquipmentUnitNotifyUpdateViewModel
    : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): EquipmentUnitNotifyUpdateViewModel {
            return ViewModelProvider(owner, EquipmentUnitNotifyUpdateViewModelFactory())
                .get(EquipmentUnitNotifyUpdateViewModel::class.java)
        }
    }

    val unitUpdated = MutableLiveData(false)

}
