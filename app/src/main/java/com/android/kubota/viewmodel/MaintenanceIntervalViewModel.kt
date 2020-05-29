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
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MaintenanceIntervalViewModel(model = model, application = application) as T
    }
}

class MaintenanceIntervalViewModel(
    model: String,
    application: Application
): AndroidViewModel(application) {

    private val _loading = MutableLiveData<Boolean>()
    private val _maintenanceSchedule = MutableLiveData<List<MaintenanceInterval>>()
    private val _error = MutableLiveData<Int>()

    val loading: LiveData<Boolean> = _loading
    val maintenanceSchedule: LiveData<List<MaintenanceInterval>> = _maintenanceSchedule
    val error: LiveData<Int> = _error

    init {
        _loading.postValue(true)
        AppProxy.proxy.serviceManager.equipmentService.getMaintenanceSchedule(model)
            .done {maintenanceList ->
                val intervalList = mutableListOf<MaintenanceInterval>()
                var interval = -1
                var actionString = ""

                maintenanceList
                    .filter { it.intervalType == "Every X Hours"}
                    .sortedBy { it.intervalValue }
                    .forEach {

                        if (interval == -1) {
                            interval = it.intervalValue
                        } else if (interval != it.intervalValue) {
                            intervalList.add(
                                MaintenanceInterval(
                                    interval = application.getString(
                                        R.string.maintenance_interval_fmt,
                                        interval
                                    ),
                                    action = actionString
                                )
                            )

                            interval = it.intervalValue
                            actionString = ""
                        } else {
                            actionString += "\n"
                        }

                        actionString += application.getString(R.string.maintenance_action_fmt, it.checkPoint, it.measures)
                    }

                //Add the last interval value.
                if (interval != -1) {
                    intervalList.add(
                        MaintenanceInterval(
                            interval = application.getString(
                                R.string.maintenance_interval_fmt,
                                interval
                            ),
                            action = actionString
                        )
                    )
                }

                _maintenanceSchedule.postValue(intervalList)
            }
            .ensure {
                _loading.postValue(false)
            }
            .catch {
                val errorResId = when (it) {
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet ->
                        R.string.connectivity_error_message
                    else ->
                        R.string.server_error_message
                }

                _error.postValue(errorResId)
            }
    }
}

@Parcelize
data class MaintenanceInterval(val interval: String, val action: String): Parcelable