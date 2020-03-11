package com.android.kubota.viewmodel

import androidx.lifecycle.*
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.EquipmentMaintenanceService
import com.kubota.repository.service.ServiceResponse

class HoursToServiceViewModel(
    private val equipmentRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): BaseEquipmentViewModel() {

    private val service = EquipmentMaintenanceService()

    private val loadingLiveData = MutableLiveData<Boolean>().apply {
        value = false
    }

    private val equipmentServiceSchedule = MutableLiveData<ServiceResponse>()

    val equipmentServiceLiveData: LiveData<ServiceResponse> = equipmentServiceSchedule

    val isLoading: LiveData<Boolean> = loadingLiveData

    override val equipmentImage: LiveData<Int>

    override val equipmentModel: LiveData<String>

    val equipmentSerial: LiveData<String?>

    val equipmentNickname: LiveData<String?>

    val equipmentEngineHours: LiveData<Int>

    init {
        val equipmentLiveData = equipmentRepo.getEquipment(equipmentId = equipmentId)

        equipmentImage = MediatorLiveData<Int>().apply {
            addSource(equipmentLiveData) {
                value = if (it != null) CategoryUtils.getEquipmentImage(it.category, it.model) else 0

                it?.let {
                    loadEquipmentServiceSchedule(it.category, it.model)
                }
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
        equipmentEngineHours = MediatorLiveData<Int>().apply {
            addSource(equipmentLiveData) {
                value = it?.engineHours
            }
        }
    }

    private fun loadEquipmentServiceSchedule(equipmentCategory: String, equipmentModel: String) {
        Utils.backgroundTask {
            loadingLiveData.postValue(true)
            val response = service.getMaintenanceSchedule(equipmentCategory, equipmentModel)
            equipmentServiceSchedule.postValue(response)
            loadingLiveData.postValue(false)
        }
    }

}

class HoursToServiceViewModelFactory(
    private val equipmentRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HoursToServiceViewModel(equipmentRepo, equipmentId) as T
    }
}