package com.android.kubota.viewmodel.equipment

import android.view.View
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.combineAndCompute
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.RestartInhibitStatusCode
import java.util.*

class InhibitRestartViewModelFactory(
    private val equipmentUnitId: UUID
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InhibitRestartViewModel(equipmentUnitId) as T
    }
}

class InhibitRestartViewModel(
    private val equipmentUnitId: UUID
): ViewModel() {
    private val _currentState = MutableLiveData(STATE.LOADING)

    private val _equipmentUnit = MutableLiveData<EquipmentUnit>()
    val equipmentUnit: LiveData<EquipmentUnit> = _equipmentUnit
    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error

    val headerStringResId: LiveData<Int> = _equipmentUnit.combineAndCompute(_currentState) { unit, currentState ->
        when (currentState) {
            STATE.STARTER_DISABLED -> R.string.starter_disabled
            STATE.PROCESSING_REQUEST -> {
                if (unit.telematics?.restartInhibitStatus?.equipmentStatus == RestartInhibitStatusCode.RestartEnabled) {
                    R.string.disabling_starter
                } else {
                    R.string.enabling_starter
                }
            }
            else -> R.string.starter_enabled
        }
    }
    val headerTextColorId: LiveData<Int> = _equipmentUnit.combineAndCompute(_currentState) { unit, currentState ->
        when (currentState) {
            STATE.STARTER_DISABLED -> R.color.disabled_header_text_color
            STATE.PROCESSING_REQUEST -> {
                if (unit.telematics?.restartInhibitStatus?.equipmentStatus == RestartInhibitStatusCode.RestartEnabled) {
                    R.color.disabled_header_text_color
                } else {
                    R.color.enabled_header_text_color
                }
            }
            else -> R.color.enabled_header_text_color
        }
    }
    val currentStateImageSrcId: LiveData<Int> = Transformations.map(_equipmentUnit) {
        it.telematics?.restartInhibitStatus?.let {
            return@map if (it.equipmentStatus == RestartInhibitStatusCode.RestartEnabled)
                R.drawable.ic_starter_enabled
            else
                R.drawable.ic_starter_disabled
        }
    }
    val endStateImageSrcId: LiveData<Int> = Transformations.map(_equipmentUnit) {
        it.telematics?.restartInhibitStatus?.let {
            return@map if (it.equipmentStatus == RestartInhibitStatusCode.RestartEnabled)
                R.drawable.ic_starter_disabled
            else
                R.drawable.ic_starter_enabled
        }
    }
    val currentStateImageVisibility: LiveData<Int> = Transformations.map(_currentState) {
        if (it == STATE.PROCESSING_REQUEST) View.GONE else View.VISIBLE
    }
    val processingGroupVisibility: LiveData<Int> = Transformations.map(_currentState) {
        if (it == STATE.PROCESSING_REQUEST) View.VISIBLE else View.INVISIBLE
    }
    val actionButtonBackgroundId: LiveData<Int> = Transformations.map(_currentState) {
        if (it == STATE.STARTER_ENABLED) R.drawable.disable_restart_button_background else R.drawable.enable_restart_button_background
    }
    val actionButtonStringResId: LiveData<Int> = Transformations.map(_currentState) {
        when (it) {
            STATE.STARTER_DISABLED -> R.string.enable_starting
            STATE.PROCESSING_REQUEST -> android.R.string.cancel
            STATE.STARTER_ENABLED, STATE.LOADING -> R.string.disable_starting
        }
    }
    val footerStringResId: LiveData<Int> = Transformations.map(_currentState) {
        when (it) {
            STATE.STARTER_DISABLED -> R.string.enable_starting_description
            STATE.PROCESSING_REQUEST -> R.string.inhibit_request_sent_description
            STATE.STARTER_ENABLED, STATE.LOADING -> R.string.disable_starting_description
        }
    }
    val isProcessing: LiveData<Boolean> = Transformations.map(_currentState) { it == STATE.PROCESSING_REQUEST }
    val isLoading: LiveData<Boolean> = Transformations.map(_currentState) { it == STATE.LOADING }

    init {
        loadEquipment(delegate = null)
    }

    private fun loadEquipment(delegate: AuthDelegate?) {
        _currentState.postValue(STATE.LOADING)
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = equipmentUnitId) }
            .done { unit ->
                unit?.telematics?.restartInhibitStatus?.let {
                    _equipmentUnit.postValue(unit)
                    if (it.equipmentStatus == it.commandStatus) {
                        _currentState.postValue(if (it.equipmentStatus == RestartInhibitStatusCode.RestartEnabled)
                            STATE.STARTER_ENABLED
                        else
                            STATE.STARTER_DISABLED
                        )
                    } else {
                        _currentState.postValue(STATE.PROCESSING_REQUEST)
                    }
                }
            }
            .catch { _error.postValue(it) }
    }

    fun cancelRequest(delegate: AuthDelegate?) {
        _equipmentUnit.value?.telematics?.restartInhibitStatus?.let {
            toggleStarterState(delegate = delegate, commandStatus = it.equipmentStatus)
        }
    }

    fun toggleStarterState(delegate: AuthDelegate?) {
        _equipmentUnit.value?.telematics?.restartInhibitStatus?.let {
            toggleStarterState(delegate = delegate, commandStatus = it.equipmentStatus.not())
        }
    }

    private fun toggleStarterState(delegate: AuthDelegate?, commandStatus: RestartInhibitStatusCode) {
        val currEquipment = _equipmentUnit.value
        currEquipment?.telematics?.restartInhibitStatus?.let {
            val newStatus = it.equipmentStatus.not()
            _currentState.postValue(STATE.LOADING)

            AuthPromise(delegate)
                .then {
                    AppProxy.proxy.serviceManager.userPreferenceService.updateEquipmentUnitRestartInhibitStatus(equipmentUnitId, newStatus)
                }
                .done {
                    if (currEquipment.telematics?.restartInhibitStatus?.equipmentStatus == commandStatus && commandStatus == RestartInhibitStatusCode.RestartEnabled) {
                        _currentState.postValue(STATE.STARTER_ENABLED)
                    } else if (currEquipment.telematics?.restartInhibitStatus?.equipmentStatus == commandStatus) {
                        _currentState.postValue(STATE.STARTER_DISABLED)
                    } else {
                        _currentState.postValue(STATE.PROCESSING_REQUEST)
                    }
                    val newUnit = currEquipment.copy(telematics = currEquipment.telematics!!.copy(restartInhibitStatus = currEquipment.telematics!!.restartInhibitStatus!!.copy(commandStatus = newStatus)))
                    _equipmentUnit.postValue(newUnit)
                }
                .catch { _error.postValue(it) }
        }
    }

    private enum class STATE {
        LOADING,
        STARTER_ENABLED,
        STARTER_DISABLED,
        PROCESSING_REQUEST
    }

}

private fun RestartInhibitStatusCode.not(): RestartInhibitStatusCode {
    return when {
        this == RestartInhibitStatusCode.RestartEnabled -> RestartInhibitStatusCode.RestartDisabled
        else -> RestartInhibitStatusCode.RestartEnabled
    }
}

