package com.android.kubota.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.KubotaServiceError
import kotlinx.android.parcel.Parcelize

class MaintenanceIntervalViewModelFactory(
    private val model: String,
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MaintenanceIntervalViewModel(model = model, application = application) as T
    }
}

class MaintenanceIntervalViewModel(
    model: String,
    application: Application
) : AndroidViewModel(application) {

    private val _loading = MutableLiveData<Boolean>()
    private val _maintenanceSchedule = MutableLiveData<List<MaintenanceInterval>>()
    private val _error = MutableLiveData<Int>()

    val loading: LiveData<Boolean> = _loading
    val maintenanceSchedule: LiveData<List<MaintenanceInterval>> = _maintenanceSchedule
    val error: LiveData<Int> = _error

    init {
        _loading.postValue(true)
        AppProxy.proxy.serviceManager.equipmentService.getMaintenanceSchedule(model)
            .done { maintenanceList ->
                val results = maintenanceList
                    .filter {
                        it.intervalType != null && it.checkPoint != null && it.measures != null
                    }
                    .groupBy { it.intervalType!! to it.intervalValue }
                    .map { (intervalGroup, subList) ->
                        val (interval, value) = intervalGroup

                        MaintenanceInterval(
                            interval = interval.replace(" X ", " $value "),
                            actions = subList.map {
                                application.getString(
                                    R.string.maintenance_action_fmt,
                                    it.checkPoint!!,
                                    it.measures!!
                                )
                            },
                            sortOrderPrimary = subList.first().sortOrder,
                            sortOrderSecondary = value
                        )
                    }
                    .sortedWith { a, b ->
                        when (val diff = a.sortOrderPrimary - b.sortOrderPrimary) {
                            0 -> (a.sortOrderSecondary ?: 0) - (b.sortOrderSecondary ?: 0)
                            else -> diff
                        }
                    }


                _maintenanceSchedule.postValue(results)
            }
            .ensure {
                _loading.postValue(false)
            }
            .catch {
                val errorResId = when (it) {
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet ->
                        R.string.connectivity_error_message
                    is KubotaServiceError.ServerMaintenance ->
                        R.string.server_maintenance
                    else ->
                        R.string.server_error_message
                }

                _error.postValue(errorResId)
            }
    }
}

@Parcelize
data class MaintenanceInterval(
    val interval: String,
    val actions: List<String>,
    val sortOrderPrimary: Int,
    val sortOrderSecondary: Int?
) : Parcelable
