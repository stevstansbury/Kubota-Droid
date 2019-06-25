package com.kubota.repository.service

import com.kubota.network.service.ModelAPI
import com.kubota.network.service.NetworkResponse


class CategoryModelService {

    private val api = ModelAPI()

    fun getCategories(): CategorySyncResults {
        when (val categories = api.getCategories()) {
            is NetworkResponse.ServerError -> return CategorySyncResults.ServerError(categories.code, categories.message)
            is NetworkResponse.IOException -> return CategorySyncResults.IOException()
            is NetworkResponse.Success -> {
                val map = mutableMapOf<String, List<String>>()
                for (cat in categories.value) {
                    when (val models= api.getModels(cat)) {
                        is NetworkResponse.ServerError -> return CategorySyncResults.ServerError(models.code, models.message)
                        is NetworkResponse.IOException -> return CategorySyncResults.IOException()
                        is NetworkResponse.Success -> {
                            map[cat] = models.value
                        }
                    }
                }
                return CategorySyncResults.Success(map)
            }
        }
    }
}

sealed class CategorySyncResults {
    class Success(val results: Map<String, List<String>>): CategorySyncResults()
    class ServerError(val code: Int, val error: String): CategorySyncResults()
    class IOException: CategorySyncResults()
}
