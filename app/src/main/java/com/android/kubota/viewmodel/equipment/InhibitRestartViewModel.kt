package com.android.kubota.viewmodel.equipment

import android.util.Log
import android.view.View
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.after
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.RestartInhibitStatusCode

class InhibitRestartViewModelFactory(
    private val equipmentUnit: EquipmentUnit
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InhibitRestartViewModel(equipmentUnit) as T
    }
}

val DEFAULT_REFRESH_INTERVAL = 60.0
val DEFAULT_INHIBIT_RESTART_REFRESH_INTERNAL = 15.0

class InhibitRestartViewModel(
    equipmentUnit: EquipmentUnit
): ViewModel() {
    private val equipmentUnitId = equipmentUnit.id
    var polling: Boolean = false
        set(newValue) {
            if (newValue == field) return
            field = newValue
            if (newValue) {
                reloadEquipment(null)
            }
        }

    var interval = DEFAULT_INHIBIT_RESTART_REFRESH_INTERNAL
    private val _equipmentUnit = MutableLiveData<EquipmentUnit>()
    val currentState:LiveData<STATE> = Transformations.map(_equipmentUnit) {
        it.telematics?.restartInhibitStatus?.let {inhibitStatus ->

            if (inhibitStatus.equipmentStatus == inhibitStatus.commandStatus) {
                if (inhibitStatus.equipmentStatus == RestartInhibitStatusCode.RestartEnabled) {
                    STATE.STARTER_ENABLED
                } else {
                    STATE.STARTER_DISABLED
                }
            } else if (inhibitStatus.commandStatus == RestartInhibitStatusCode.RestartDisabled) {
                STATE.PROCESSING_ENABLE_REQUEST
            } else {
                STATE.PROCESSING_DISABLE_REQUEST
            }
        } ?: STATE.STARTER_ENABLED
    }
    val equipmentNickname: LiveData<String> = Transformations.map(_equipmentUnit) { it.nickName ?: it.model }
    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error

    val headerStringResId: LiveData<Int> = Transformations.map(currentState) { state ->
        when (state) {
            STATE.STARTER_ENABLED -> R.string.starter_enabled
            STATE.STARTER_DISABLED -> R.string.starter_disabled
            STATE.PROCESSING_ENABLE_REQUEST -> R.string.disabling_starter
            STATE.PROCESSING_DISABLE_REQUEST -> R.string.enabling_starter
        }
    }
    val headerTextColorId: LiveData<Int> = Transformations.map(currentState) {state ->
        when (state) {
            STATE.STARTER_ENABLED -> R.color.enabled_header_text_color
            STATE.STARTER_DISABLED -> R.color.disabled_header_text_color
            STATE.PROCESSING_ENABLE_REQUEST -> R.color.disabled_header_text_color
            STATE.PROCESSING_DISABLE_REQUEST -> R.color.enabled_header_text_color
        }
    }
    val currentStateImageSrcId: LiveData<Int> = Transformations.map(_equipmentUnit) {
        it.telematics?.restartInhibitStatus?.let {
            return@map if (it.equipmentStatus == RestartInhibitStatusCode.RestartEnabled)
                R.drawable.ic_starter_enabled
            else
                R.drawable.ic_starter_disabled
        } ?: R.drawable.ic_starter_enabled
    }
    val endStateImageSrcId: LiveData<Int> = Transformations.map(_equipmentUnit) {
        it.telematics?.restartInhibitStatus?.let {
            return@map if (it.equipmentStatus == RestartInhibitStatusCode.RestartEnabled)
                R.drawable.ic_starter_disabled
            else
                R.drawable.ic_starter_enabled
        } ?: R.drawable.ic_starter_enabled
    }
    val currentStateImageVisibility: LiveData<Int> = Transformations.map(currentState) {
        if (it == STATE.PROCESSING_ENABLE_REQUEST || it == STATE.PROCESSING_DISABLE_REQUEST) View.GONE else View.VISIBLE
    }
    val processingGroupVisibility: LiveData<Int> = Transformations.map(currentState) {
        if (it == STATE.PROCESSING_ENABLE_REQUEST || it == STATE.PROCESSING_DISABLE_REQUEST) View.VISIBLE else View.INVISIBLE
    }
    val actionButtonBackgroundId: LiveData<Int> = Transformations.map(currentState) {
        when (it) {
            STATE.STARTER_ENABLED, STATE.PROCESSING_DISABLE_REQUEST -> R.drawable.disable_restart_button_background
            STATE.STARTER_DISABLED, STATE.PROCESSING_ENABLE_REQUEST -> R.drawable.enable_restart_button_background
        }
    }
    val actionButtonBackgroundColorId: LiveData<Int> = Transformations.map(currentState) {
        when (it) {
            STATE.STARTER_ENABLED, STATE.PROCESSING_DISABLE_REQUEST -> R.color.disable_starting_background_color
            STATE.STARTER_DISABLED, STATE.PROCESSING_ENABLE_REQUEST -> R.color.enable_starting_background_color
        }
    }
    val actionButtonStringResId: LiveData<Int> = Transformations.map(currentState) {
        when (it) {
            STATE.STARTER_ENABLED -> R.string.disable_starting
            STATE.STARTER_DISABLED -> R.string.enable_starting
            else -> android.R.string.cancel
        }
    }
    val footerStringResId: LiveData<Int> = Transformations.map(currentState) {
        when (it) {
            STATE.STARTER_ENABLED -> R.string.disable_starting_description
            STATE.STARTER_DISABLED -> R.string.enable_starting_description
            else -> R.string.inhibit_request_sent_description
        }
    }
    val footerStringInfoVisibility: LiveData<Int> = Transformations.map(currentState) {
        if (it == STATE.PROCESSING_ENABLE_REQUEST || it == STATE.PROCESSING_DISABLE_REQUEST) View.VISIBLE else View.INVISIBLE
    }
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _equipmentUnit.postValue(equipmentUnit)
        this.polling = true
    }

    private fun reloadEquipment(delegate: AuthDelegate?) {
        _isLoading.postValue(true)
        if (!polling) return

        AuthPromise(delegate=delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.getEquipment(equipmentUnitId)
            }
            .ensure { _isLoading.postValue(false) }
            .thenMap { equipmentUnit ->
                if (!polling) return@thenMap Promise.value(Unit)

                this._equipmentUnit.value = equipmentUnit

                after(seconds=this.interval)
            }
            .done {
                this.reloadEquipment(delegate=delegate)
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
        _isLoading.postValue(true)
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.updateEquipmentUnitRestartInhibitStatus(equipmentUnitId, commandStatus)
            }
            .done { _equipmentUnit.postValue(it) }
            .ensure { _isLoading.postValue(false) }
            .catch { _error.postValue(it) }
    }

}

enum class STATE {
    STARTER_ENABLED,
    STARTER_DISABLED,
    PROCESSING_ENABLE_REQUEST,
    PROCESSING_DISABLE_REQUEST
}

private fun RestartInhibitStatusCode.not(): RestartInhibitStatusCode {
    return when {
        this == RestartInhibitStatusCode.RestartEnabled -> RestartInhibitStatusCode.RestartDisabled
        else -> RestartInhibitStatusCode.RestartEnabled
    }
}

