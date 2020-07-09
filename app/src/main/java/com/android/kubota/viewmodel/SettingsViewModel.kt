package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.preference.UserSettings

class SettingsViewModelFactory: ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SettingsViewModel() as T
    }
}

class SettingsViewModel: ViewModel() {
    private val mSettings = MutableLiveData<UserSettings>()
    private val mLoading = MutableLiveData<Boolean>(false)
    private val mError = MutableLiveData<Throwable?>()

    val settings: LiveData<UserSettings> = mSettings
    val loading: LiveData<Boolean> = mLoading
    val error: LiveData<Throwable?> = mError

    init {
        loadSettings()
    }

    fun updateSettings(newSettings: UserSettings) {
        mError.postValue(null)
        mLoading.postValue(true)
        AppProxy.proxy.serviceManager.userPreferenceService.updateUserSettings(newSettings)
            .done { mSettings.postValue(it) }
            .ensure { mLoading.postValue(false) }
            .catch { mError.postValue(it) }
    }

    private fun loadSettings() {
        mError.postValue(null)
        mLoading.postValue(true)
        AppProxy.proxy.serviceManager.userPreferenceService.getUserSettings()
            .done { mSettings.postValue(it) }
            .ensure { mLoading.postValue(false) }
            .catch { mError.postValue(it) }
    }
}