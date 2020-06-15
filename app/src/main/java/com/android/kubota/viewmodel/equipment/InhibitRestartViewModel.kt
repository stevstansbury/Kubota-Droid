package com.android.kubota.viewmodel.equipment

import android.view.View
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.SignInHandler
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.then
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.EquipmentUnit
import okhttp3.internal.waitMillis
import java.lang.ref.WeakReference
import java.util.*

class InhibitRestartViewModelFactory(
    private val signInHandler: WeakReference<SignInHandler>?,
    private val equipmentUnitId: UUID
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InhibitRestartViewModel(signInHandler, equipmentUnitId) as T
    }
}

class InhibitRestartViewModel(
    private val signInHandler: WeakReference<SignInHandler>?,
    private val equipmentUnitId: UUID
): ViewModel() {

    //TODO: Once we have a value on EquipmentUnit we can get rid of this
    private var _isStarterEnabled = MutableLiveData<Boolean>(true)

    private val _currentState = MutableLiveData(STATE.LOADING)

    private val _equipmentUnit = MutableLiveData<EquipmentUnit>()
    val equipmentUnit: LiveData<EquipmentUnit> = _equipmentUnit

    val headerStringResId: LiveData<Int> = Transformations.map(_currentState) {
        return@map if (it == STATE.STARTER_DISABLED) R.string.starter_disabled else R.string.starter_enabled
    }
    val headerTextColorId: LiveData<Int> = Transformations.map(_currentState) {
        if (it == STATE.STARTER_ENABLED) R.color.enabled_header_text_color else R.color.disabled_header_text_color
    }
    val currentStateImageSrcId: LiveData<Int> = Transformations.map(_isStarterEnabled) {
        if (it) R.drawable.ic_starter_enabled else R.drawable.ic_starter_disabled
    }
    val endStateImageSrcId: LiveData<Int> = Transformations.map(_isStarterEnabled) {
        if (it) R.drawable.ic_starter_disabled else R.drawable.ic_starter_enabled
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
        loadEquipment()
    }

    private fun loadEquipment() {
        AuthPromise()
            .onSignIn { signIn() }
            .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = equipmentUnitId) }
            .done { unit ->
                unit?.let {
                    _equipmentUnit.postValue(it)
                    //TODO: Here we would check the state of the Equipment's starter and determine how to adjust the UI
                    val newValue = _isStarterEnabled.value!!.not()
                    _isStarterEnabled.postValue(newValue)
                    _currentState.postValue(if (newValue) STATE.STARTER_ENABLED else STATE.STARTER_DISABLED)
                }
            }
            .catch {
                //TODO: Handle errors here
            }
    }

    fun cancelRequest() {

    }

    fun toggleStarterState() {
        _currentState.postValue(STATE.PROCESSING_REQUEST)

        AuthPromise()
            .onSignIn { signIn() }
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.updateStarterState()
            }
            .done {
                //TODO: Here we would check the state of the Equipment's starter and determine how to adjust the UI
                val newValue = _isStarterEnabled.value!!.not()
                _isStarterEnabled.postValue(newValue)
                _currentState.postValue(if (newValue) STATE.STARTER_ENABLED else STATE.STARTER_DISABLED)
            }
            .catch {
                //TODO: Handle errors here
            }
    }

    private fun signIn(): Promise<Unit> {
        return signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }

    private enum class STATE {
        LOADING,
        STARTER_ENABLED,
        STARTER_DISABLED,
        PROCESSING_REQUEST
    }

}

fun UserPreferenceService.updateStarterState(): Promise<Unit> {
    return after(6.0).then { Promise.value(Unit) }
}

