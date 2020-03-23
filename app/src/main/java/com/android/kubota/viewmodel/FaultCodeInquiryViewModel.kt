package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.kubota.extensions.toUIEquipment
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.FaultCodeResponse
import com.kubota.repository.service.FaultCodeService

class FaultCodeInquiryViewModel(
    private val equipmentRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): BaseEquipmentViewModel() {

    override val equipmentImage: LiveData<Int>
    override val equipmentModel: LiveData<String>

    val equipmentSerial: LiveData<String?>
    val equipmentNickname: LiveData<String?>

    val faultCodeResultsLiveData = MutableLiveData<FaultCodeResponse>()
    val isLoading = MutableLiveData<Boolean>().apply {
        value = false
    }

    private val service = FaultCodeService()

    init {
        val equipmentLiveData = equipmentRepo.getEquipment(equipmentId = equipmentId)

        equipmentImage = MediatorLiveData<Int>().apply {
            addSource(equipmentLiveData) {
                value = if (it != null) CategoryUtils.getEquipmentImage(it.category, it.model) else 0
            }
        }
        equipmentModel = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.model
            }
        }
        equipmentSerial = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.serialNumber
            }
        }
        equipmentNickname = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.nickname
            }
        }
    }

    fun loadModel(equipmentId: Int): LiveData<UIEquipment?> = Transformations.map(equipmentRepo.getEquipment(equipmentId)) {
        return@map it?.toUIEquipment()
    }

    fun getEquipmentFaultCode(model: String, codes: ArrayList<Int>) {
        Utils.backgroundTask {
            isLoading.postValue(true)
            val response = service.checkFaultCodeForModel(model, codes)
            faultCodeResultsLiveData.postValue(response)
            isLoading.postValue(false)
        }
    }
}