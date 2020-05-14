package com.android.kubota.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class EquipmentDetailViewModel(val equipmentId: UUID): ViewModel()  {
    val isLoading = MutableLiveData(false)
    val equipmentUnit = MutableLiveData<EquipmentUnit?>(null)
    var signInHandler: (() -> Unit)? = null

    fun loadEquipmentUnit() {
        this.isLoading.value = true
        AuthPromise()
            .onSignIn { onSignIn() }
            .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = equipmentId) }
            .done { this.equipmentUnit.value = it }
            .ensure { this.isLoading.value = false }
    }

//    val liveData: LiveData<Equipment?> = MutableLiveData(null)
    // FIXME
    //equipmentPrefsRepo.getSavedEquipment(equipmentId)

    fun updateSerialNumber(equipmentId: UUID, newSerialNumber: String?)  {
//        Utils.backgroundTask {
//            equipmentPrefsRepo.updateEquipmentSerialPin(equipmentId, newSerialNumber)
//        }
    }

    fun updateNickName(equipmentId: UUID, newSerialNumber: String?) {
//        Utils.backgroundTask {
//            equipmentPrefsRepo.updateEquipmentNickName(equipmentId, newSerialNumber)
//        }
    }

    private fun onSignIn() {
        this.signInHandler?.let { it() }
    }

}
