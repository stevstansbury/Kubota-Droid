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
import com.kubota.service.domain.EquipmentMaintenance
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
                val alteredMaintenanceList = mutableListOf<EquipmentMaintenance>()
                maintenanceList.forEachIndexed { index, equipmentMaintenance ->
                    if (equipmentMaintenance.intervalType == "Annually") {
                        alteredMaintenanceList.add(index, equipmentMaintenance.copy(intervalType = "Every X Years", intervalValue = 1))
                    } else {
                        alteredMaintenanceList.add(index, equipmentMaintenance)
                    }
                }

                val results = mutableListOf<MaintenanceInterval>()

                // As Needed maintenance
                val asNeededList = parse(
                    maintenanceList = alteredMaintenanceList,
                    predicate = { it.intervalType == "As Needed" },
                    getIntervalString = { application.getString(R.string.maintenance_interval_as_needed) },
                    getActionString = {checkPoint, measures ->
                        application.getString(R.string.maintenance_action_fmt, checkPoint, measures)
                    }
                )
                results.addAll(asNeededList)

                // Daily maintenance
                val dailyList = parse(
                    maintenanceList = alteredMaintenanceList,
                    predicate = { it.intervalType == "Daily" },
                    getIntervalString = { application.getString(R.string.maintenance_interval_daily) },
                    getActionString = {checkPoint, measures ->
                        application.getString(R.string.maintenance_action_fmt, checkPoint, measures)
                    }
                )
                results.addAll(dailyList)

                // Annually maintenance
                val annually = parse(
                    maintenanceList = alteredMaintenanceList,
                    predicate = { it.intervalType == "Every X Years" },
                    getIntervalString = {intervalValue ->
                        if (intervalValue == 1) {
                            application.getString(
                                R.string.maintenance_interval_annually
                            )
                        } else {
                            application.getString(
                                R.string.maintenance_interval_years_fmt,
                                intervalValue
                            )
                        }
                    },
                    getActionString = {checkPoint, measures ->
                        application.getString(R.string.maintenance_action_fmt, checkPoint, measures)
                    }
                )
                results.addAll(annually)

                // Maintenance by Hours
                val hoursList = parse(
                    maintenanceList = maintenanceList,
                    predicate = { it.intervalType == "Every X Hours" },
                    getIntervalString = { application.getString(R.string.maintenance_interval_hours_fmt, it) },
                    getActionString = {checkPoint, measures ->
                        application.getString(R.string.maintenance_action_fmt, checkPoint, measures)
                    }
                )
                results.addAll(hoursList)

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
                    else ->
                        R.string.server_error_message
                }

                _error.postValue(errorResId)
            }
    }


    private fun parse(
        maintenanceList: List<EquipmentMaintenance>,
        predicate:(maintenance :EquipmentMaintenance) -> Boolean,
        getIntervalString:(intervalValue: Int) -> String,
        getActionString:(checkPoint: String, measures: String) -> String
    ): List<MaintenanceInterval> {
        val intervalList = mutableListOf<MaintenanceInterval>()

        val mapByIntervalValue = mutableMapOf<Int, List<EquipmentMaintenance>>()

        val filteredSortedList = maintenanceList
            .filter { predicate(it) }
            .sortedBy { it.intervalValue }

        filteredSortedList.forEach {
            if (!mapByIntervalValue.containsKey(it.intervalValue)) {
                mapByIntervalValue[it.intervalValue] = mutableListOf()
            }
        }

        for (key in mapByIntervalValue.keys) {
            mapByIntervalValue[key] = parseByIntervalValue(key, filteredSortedList)
        }

        for (key in mapByIntervalValue.keys) {
            mapByIntervalValue[key]?.let {
                intervalList.add(combine(it, getIntervalString, getActionString))
            }
        }

        return intervalList
    }

    private fun parseByIntervalValue(
        intervalValue: Int,
        filteredSortedList: List<EquipmentMaintenance>
    ): List<EquipmentMaintenance> {
        val returnList = mutableListOf<EquipmentMaintenance>()
        filteredSortedList.forEach {
            if (intervalValue == 0 || (intervalValue >= it.intervalValue && intervalValue.rem(it.intervalValue) == 0)) {
                returnList.add(it)
            }
        }

        return returnList
    }

    private fun combine(
        filteredSortedList: List<EquipmentMaintenance>,
        getIntervalString:(intervalValue: Int) -> String,
        getActionString:(checkPoint: String, measures: String) -> String
    ): MaintenanceInterval {
        var interval = -1
        var actionString = ""

        filteredSortedList.forEach {
            if (actionString.isNotEmpty()) {
                actionString += "\n"
            }

            interval = it.intervalValue
            actionString += getActionString(it.checkPoint, it.measures)
        }

        return MaintenanceInterval(interval = getIntervalString(interval), action = actionString)
    }
}

@Parcelize
data class MaintenanceInterval(val interval: String, val action: String): Parcelable