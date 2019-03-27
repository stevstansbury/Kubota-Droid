package com.kubota.repository.service

import com.kubota.network.service.ModelAPI
import com.kubota.network.service.NetworkResponse


internal class CategoryModelService() {

    private val api = ModelAPI()

    companion object {
        private const val LOG_TAG = "CategoryModelService"
    }

    fun getCategories(): CategorySyncResults {
        val categories = api.getCategories()
        when (categories) {
            is NetworkResponse.ServerError -> return CategorySyncResults.ServerError(categories.code, categories.message)
            is NetworkResponse.IOException -> return CategorySyncResults.IOException()
            is NetworkResponse.Success -> {
                val map = mutableMapOf<String, List<String>>()
                for (cat in categories.value) {
                    val models= api.getModels(cat)
                    when (models) {
                        is NetworkResponse.ServerError -> return CategorySyncResults.ServerError(models.code, models.message)
                        is NetworkResponse.IOException -> return CategorySyncResults.IOException()
                        is NetworkResponse.Success -> {
                            map.put(cat, models.value)
                        }
                    }
                }
                return CategorySyncResults.Success(map)
            }
        }
    }
}

sealed class CategorySyncResults() {
    class Success(val results: Map<String, List<String>>): CategorySyncResults()
    class ServerError(val code: Int, val error: String): CategorySyncResults()
    class IOException(): CategorySyncResults()
}