package com.android.kubota.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.android.kubota.extensions.backgroundTask
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import kotlinx.coroutines.*

class ChooseEquipmentViewModel(private val categoryService: CategoryModelService): ViewModel() {
    private val viewModelJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    val categories: MutableLiveData<Map<String, List<String>>> by lazy {
        loadCategories()
        MutableLiveData<Map<String, List<String>>>()
    }
    val isLoading = MutableLiveData<Boolean>()

    val serverError = MutableLiveData<Boolean>()

    private fun loadCategories() {
        backgroundScope.backgroundTask {
            isLoading.postValue(true)
            serverError.postValue(false)
            when (val result = categoryService.getCategories()) {
                is CategorySyncResults.Success -> categories.postValue(result.results)
                is CategorySyncResults.ServerError,
                is CategorySyncResults.IOException -> serverError.postValue(true)
            }
            isLoading.postValue(false)
        }
    }
}
