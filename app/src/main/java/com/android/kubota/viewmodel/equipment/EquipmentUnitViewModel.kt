package com.android.kubota.viewmodel.equipment

import android.os.Parcelable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.android.kubota.BR
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.engineHours
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.ItemViewModel
import com.android.kubota.utility.notifyChange
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.domain.EquipmentMaintenanceHistoryEntry
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.EquipmentUnitUpdate
import com.kubota.service.internal.MaintenanceHistoryUpdate
import kotlinx.android.parcel.Parcelize
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

class EquipmentUnitViewModelFactory(
    private val equipmentUnit: EquipmentUnit
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnit) as T
    }
}

class EquipmentUnitViewModel(unit: EquipmentUnit) : ViewModel() {

    companion object {
        fun instance(
            owner: ViewModelStoreOwner,
            equipmentUnit: EquipmentUnit
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnit))
                .get(EquipmentUnitViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentUnit = MutableLiveData(unit)
    private val mEquipmentMaintenanceHistory =
        MutableLiveData(emptyList<EquipmentMaintenanceHistoryEntry>())
    private val mEquipmentMaintenanceSchedule =
        MutableLiveData(emptyList<MaintenanceIntervalItem>())
    private val mEquipmentMaintenanceChecklist =
        MutableLiveData(emptyList<MaintenanceChecklistItem>())
    private val mNextMaintenanceSchedules = MutableLiveData(emptyList<MaintenanceIntervalItem>())
    private val mUnitUpdated = MutableLiveData(false)
    private val mCompatibleAttachments = MutableLiveData<List<EquipmentModelTree>>()
    private val mCompatibleMachines = MutableLiveData<List<EquipmentModel>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentUnit: LiveData<EquipmentUnit?> = mEquipmentUnit
    val equipmentMaintenanceHistory: LiveData<List<EquipmentMaintenanceHistoryEntry>> =
        mEquipmentMaintenanceHistory
    val equipmentMaintenanceSchedule: LiveData<List<MaintenanceIntervalItem>> =
        mEquipmentMaintenanceSchedule
    val equipmentMaintenanceChecklist: LiveData<List<MaintenanceChecklistItem>> =
        mEquipmentMaintenanceChecklist
    val nextMaintenanceSchedules: LiveData<List<MaintenanceIntervalItem>> =
        mNextMaintenanceSchedules
    val unitUpdated: LiveData<Boolean> = mUnitUpdated
    val compatibleAttachments: LiveData<List<EquipmentModelTree>> = mCompatibleAttachments
    val compatibleMachines = mCompatibleMachines

    fun reload(delegate: AuthDelegate?) {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)
            AuthPromise(delegate = delegate)
                .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(unit.id) }
                .done { mEquipmentUnit.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .done { loadCompatibleAttachments() }
                .catch { mError.postValue(it) }
        }
    }

    fun loadCompatibleAttachments() {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)

            val equipmentService = AppProxy.proxy.serviceManager.equipmentService
            equipmentService.getModel(unit.model)
                .thenMap { equipmentModel ->
                    val model = equipmentModel
                        ?: throw IllegalStateException("exists as equipment unit, must exist as a model")

                    when (model.compatibleAttachments.isEmpty()) {
                        true -> Promise.value(emptyList())
                        false -> {
                            val filter = EquipmentTreeFilter.AttachmentsCompatibleWith(unit.model)
                            equipmentService.getEquipmentTree(listOf(filter))
                        }
                    }
                }
                .done { mCompatibleAttachments.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .catch { mError.postValue(it) }
        }
    }

    fun loadMaintenanceInfo() {
        mIsLoading.postValue(true)

        mEquipmentUnit.value?.let { unit ->
            AppProxy.proxy.serviceManager.equipmentService.getMaintenanceSchedule(unit.model)
                .done { maintenanceList ->
                    val results = maintenanceList
                        .filter {
                            it.intervalType != null && it.checkPoint != null && it.measures != null
                        }
                        .groupBy { it.intervalType!! to it.intervalValue }
                        .map { (intervalGroup, subList) ->
                            val (interval, value) = intervalGroup

                            MaintenanceIntervalItem(
                                intervalType = interval,
                                intervalValue = value!!,
                                actions = subList.map {
                                    MaintenanceAction(
                                        id = it.id,
                                        category = it.measures!!,
                                        value = it.checkPoint!!
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

                    mEquipmentMaintenanceSchedule.value = results
                    loadMaintenanceHistory()
                }
                .catch {
                    mError.value = it
                }
        }
    }


    private fun loadMaintenanceHistory() {
        mEquipmentUnit.value?.let { unit ->
            AppProxy.proxy.serviceManager.equipmentService.getMaintenanceHistory(unit.id.toString())
                .done {
                    val history = it.toMutableList()
                    val pendingUpdate = AppProxy.proxy.preferences.getMaintenancePendingUpdate()

                    if (pendingUpdate != null) {
                        val update = pendingUpdate.second

                        history.add(
                            EquipmentMaintenanceHistoryEntry(
                                id = update.id.orEmpty(),
                                intervalType = update.intervalType,
                                intervalValue = update.intervalValue,
                                completedEngineHours = update.completedEngineHours,
                                notes = update.notes,
                                updatedDate = Date.from(
                                    LocalDateTime.from(
                                        DateTimeFormatter.ofPattern(
                                            "yyyy-MM-dd'T'HH:mm:ss'Z'"
                                        ).parse(update.updatedDate)
                                    ).atZone(ZoneId.systemDefault()).toInstant()
                                ),
                                maintenanceCheckList = update.maintenanceCheckList
                            )
                        )
                    }
                    mEquipmentMaintenanceHistory.value =
                        history.sortedByDescending { item -> item.completedEngineHours }

                    getNextMaintenanceSchedules()
                }
                .catch { mError.value = it }
        }
    }

    fun toggleChecklistItem(item: MaintenanceChecklistItem) {
        mEquipmentMaintenanceChecklist.value.orEmpty().forEach {
            if (it is MaintenanceChecklistItemViewModel && it == item) {
                it.checked = !it.checked
            }
        }
    }

    fun toggleAllChecklistItems() {
        val areItemsChecked = mEquipmentMaintenanceChecklist.value.orEmpty()
            .any { it is MaintenanceChecklistItemViewModel && it.checked }

        mEquipmentMaintenanceChecklist.value.orEmpty().map {
            if (it is MaintenanceChecklistItemViewModel && it.checked == areItemsChecked) {
                it.checked = !it.checked
            } else {
                it
            }
        }
    }

    fun loadActions(interval: MaintenanceIntervalItem) {
        val actions = mutableListOf<MaintenanceChecklistItem>()

        val actionCategories = interval.actions.distinct().groupBy { it.category }
        actionCategories.keys.forEach {
            actions.add(MaintenanceChecklistCategoryViewModel(it))
            actions.addAll(actionCategories[it]!!.map { action ->
                MaintenanceChecklistItemViewModel(
                    id = action.id,
                    value = action.value,
                    isChecked = action.checked
                )
            })
        }

        mEquipmentMaintenanceChecklist.postValue(actions)
    }

    fun updateMaintenanceSchedule(interval: MaintenanceIntervalItem, hours: Long, notes: String) {
        val equipmentId = mEquipmentUnit.value?.id ?: return

        val checklist = mEquipmentMaintenanceChecklist.value.orEmpty()
            .filterIsInstance<MaintenanceChecklistItemViewModel>()
            .associate { it.id to it.checked }

        val update = MaintenanceHistoryUpdate(
            id = UUID.randomUUID().toString(),
            intervalType = interval.intervalType,
            intervalValue = interval.intervalValue,
            completedEngineHours = hours,
            notes = notes,
            updatedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(LocalDateTime.now()),
            maintenanceCheckList = checklist
        )

        mIsLoading.postValue(true)

        AppProxy.proxy.serviceManager.equipmentService.updateMaintenanceHistory(
            equipmentId.toString(),
            update
        )
            .done { updated ->
                if (updated) {
                    AppProxy.proxy.preferences.clearMaintenancePendingUpdate()
                } else {
                    if (AppProxy.proxy.preferences.getMaintenancePendingUpdate() == null) {
                        AppProxy.proxy.preferences.setMaintenancePendingUpdate(
                            equipmentId.toString(),
                            update
                        )
                    }
                }

                mUnitUpdated.postValue(true)
            }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

    private fun getNextMaintenanceSchedules() {
        val schedules = mEquipmentMaintenanceSchedule.value.orEmpty()
        val filteredSchedules = schedules.filter {
            it.intervalType == "Every X Hours"
        }
        val nextIntervals = mutableListOf<MaintenanceIntervalItem>()

        val minInterval = filteredSchedules.firstOrNull()?.intervalValue ?: 0
        val nextInterval = getEquipmentNextInterval()

        repeat(3) {
            nextIntervals.add(
                getHourIntervals(
                    getNextInterval(
                        nextIntervals,
                        nextInterval + (it * minInterval),
                        minInterval
                    )
                )
            )
        }

        nextIntervals.addAll(schedules.filter { it.intervalType != "Every X Hours" })

        mNextMaintenanceSchedules.postValue(nextIntervals)
        mIsLoading.postValue(false)
    }

    fun getNextInterval(
        intervals: List<MaintenanceIntervalItem>,
        interval: Int,
        minimumInterval: Int
    ): Int {
        val filteredHistory = mEquipmentMaintenanceHistory.value.orEmpty()
            .filter { it.intervalType == "Every X Hours" }.map { it.intervalValue }

        val historyValue = filteredHistory + intervals.map { it.intervalValue }

        return if (interval in historyValue) {
            getNextInterval(intervals, interval + minimumInterval, minimumInterval)
        } else {
            interval
        }
    }

    fun getEquipmentNextInterval(): Int {
        val schedules = mEquipmentMaintenanceSchedule.value.orEmpty().filter {
            it.intervalType == "Every X Hours"
        }
        val history = mEquipmentMaintenanceHistory.value.orEmpty()

        val lastMaintenanceEngineHours = equipmentUnit.value?.engineHours?.toInt() ?: 0

        val minInterval = schedules.firstOrNull()?.intervalValue ?: 0

        val intervalDifference = lastMaintenanceEngineHours.mod(minInterval)
        val lastInterval = (lastMaintenanceEngineHours - intervalDifference)

        return if (history.any { (it.completedEngineHours ?: 0L) >= lastInterval }) {
            lastInterval + minInterval
        } else {
            lastInterval
        }
    }

    fun getMinInterval(): Int {
        return mEquipmentMaintenanceSchedule.value.orEmpty().firstOrNull {
            it.intervalType == "Every X Hours"
        }?.intervalValue ?: 0
    }

    fun getHourIntervals(value: Int): MaintenanceIntervalItem {
        val intervals = mEquipmentMaintenanceSchedule.value.orEmpty()
            .filter { it.intervalType == "Every X Hours" }
            .filter { value.mod(it.intervalValue) == 0 }
        val actions = intervals.map { it.actions }.flatten()

        val maintenanceInterval = intervals.first()

        return MaintenanceIntervalItem(
            intervalType = "Every X Hours",
            intervalValue = value,
            actions = actions,
            sortOrderPrimary = maintenanceInterval.sortOrderPrimary,
            sortOrderSecondary = maintenanceInterval.sortOrderSecondary
        )
    }

    fun getMaintenanceItem(historyEntry: EquipmentMaintenanceHistoryEntry): MaintenanceIntervalItem {
        val schedule = mEquipmentMaintenanceSchedule.value.orEmpty().map { it.actions }.flatten()

        val actions = historyEntry.maintenanceCheckList.map {
            val action = schedule.first { action -> action.id == it.key }

            MaintenanceAction(
                id = it.key,
                category = action.category,
                value = action.value,
                checked = it.value
            )
        }

        return MaintenanceIntervalItem(
            intervalType = historyEntry.intervalType!!,
            intervalValue = historyEntry.intervalValue!!,
            actions = actions,
            sortOrderPrimary = -1,
            sortOrderSecondary = -1
        )
    }

    fun loadCompatibleMachines() {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)

            AppProxy.proxy.serviceManager.equipmentService.getCompatibleMachines(unit.model)
                .done { mCompatibleMachines.postValue(it) }
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
        }
    }

    fun updateEquipmentUnit(delegate: AuthDelegate?, nickName: String?, engineHours: Double?) {
        val equipmentUnit = this.equipmentUnit.value ?: return
        this.mUnitUpdated.postValue(false)
        this.mIsLoading.postValue(true)
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(
                        EquipmentUnitUpdate(
                            equipmentUnit.id,
                            nickName = nickName?.trim(),
                            engineHours = engineHours?.let { abs(it) })
                    )
            }
            .done { equipment ->
                equipment.firstOrNull {
                    it.id == equipmentUnit.id
                        && it.model == equipmentUnit.model
                        && it.pinOrSerial == equipmentUnit.pinOrSerial
                }?.let {
                    mEquipmentUnit.postValue(it)
                    mUnitUpdated.postValue(true)
                }
            }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }

    fun updateEngineHours(delegate: AuthDelegate?, engineHours: Double?) {
        val equipmentUnit = this.equipmentUnit.value ?: return
        this.mUnitUpdated.postValue(false)
        this.mIsLoading.postValue(true)
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(
                        EquipmentUnitUpdate(
                            equipmentUnit.id,
                            nickName = equipmentUnit.nickName,
                            engineHours = engineHours?.let { abs(it) })
                    )
            }
            .done { equipment ->
                equipment.firstOrNull {
                    it.id == equipmentUnit.id
                        && it.model == equipmentUnit.model
                        && it.pinOrSerial == equipmentUnit.pinOrSerial
                }?.let {
                    mEquipmentUnit.postValue(it)
                    mUnitUpdated.postValue(true)
                }
            }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }
}

@Parcelize
data class MaintenanceIntervalItem(
    val intervalType: String,
    val intervalValue: Int,
    val actions: List<MaintenanceAction>,
    val sortOrderPrimary: Int,
    val sortOrderSecondary: Int?
) : Parcelable {
    /*val x = when (intervalType) {
        "Every X Hours" -> {}
        "Every X Months" -> {}
        "Every X Years" -> {}
        else -> {}
    }
*/

    /*val displayedInterval = if (intervalType == "Every X Hours") {
        intervalType.replace("Every ", "").replace("X", "$intervalValue")
    } else {
        intervalType.replace("X", "$intervalValue")
    }*/
}

@Parcelize
data class MaintenanceAction(
    val id: String,
    val category: String,
    val value: String,
    val checked: Boolean = false
) : Parcelable

sealed class MaintenanceChecklistItem : ItemViewModel()

data class MaintenanceChecklistItemViewModel(
    val id: String,
    val value: String,
    private val isChecked: Boolean = false
) : MaintenanceChecklistItem() {

    @get:Bindable
    var checked by notifyChange(isChecked, BR.checked)
}

data class MaintenanceChecklistCategoryViewModel(
    val title: String
) : MaintenanceChecklistItem()