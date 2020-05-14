package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.CategoryUtils
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class EngineHoursViewModel(
    private val equipmentId: UUID
): ViewModel() {

//    override val equipmentImage = MutableLiveData<Int>(0)
//    override val equipmentModel = MutableLiveData<String>("")
//    val equipmentSerial = MutableLiveData<String?>(null)
//    val equipmentNickname = MutableLiveData<String?>(null)
//    val equipmentEngineHours = MutableLiveData<Int>(0)

    val equipmentUnit = MutableLiveData<EquipmentUnit?>(null)
    var signInHandler: (() -> Unit)? = null

    init {

//        val equipmentLiveData = equipmentRepo.getEquipment(equipmentId = equipmentId)
//
//        equipmentImage = MediatorLiveData<Int>().apply {
//            addSource(equipmentLiveData) {
//                value = if (it != null) CategoryUtils.getEquipmentImage(it.category, it.model) else 0
//            }
//        }
//        equipmentModel = MediatorLiveData<String>().apply {
//            addSource(equipmentLiveData) {
//                value = it?.model
//            }
//        }
//        equipmentSerial = MediatorLiveData<String>().apply {
//            addSource(equipmentLiveData) {
//                value = it?.serialNumber
//            }
//        }
//        equipmentNickname = MediatorLiveData<String>().apply {
//            addSource(equipmentLiveData) {
//                value = it?.nickname
//            }
//        }
//        equipmentEngineHours = MediatorLiveData<Int>().apply {
//            addSource(equipmentLiveData) {
//                value = it?.engineHours
//            }
//        }
    }

    fun loadEquipmentUnit() {
//        this.isLoading.value = true
//        AuthPromise()
//            .onSignIn { onSignIn() }
//            .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = equipmentId) }
//            .done { this.equipmentUnit.value = it }
//            .ensure { this.isLoading.value = false }
    }

    fun updateEngineHours(engineHours: Int) {
//        Utils.backgroundTask {
//            equipmentRepo.updateEquipmentEngineHours(equipmentId, engineHours)
//        }
    }
}