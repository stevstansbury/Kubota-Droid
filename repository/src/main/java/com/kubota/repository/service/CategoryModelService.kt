package com.kubota.repository.service

import android.util.Log
import com.kubota.network.service.ModelService
import com.kubota.repository.BuildConfig
import com.kubota.repository.utils.Utils
import java.io.IOException


internal class CategoryModelService() {

    companion object {
        private const val LOG_TAG = "CategoryModelService"
    }
    private val api = Utils.getRetrofit().create(ModelService::class.java)

    fun getCategories(): CategorySyncResults {
        try {
            val response = api.getCategories().execute()
            if (response.isSuccessful) {
                val categoriesList = response.body()

                categoriesList?.let {
                    val categoryModelMap : MutableMap<String, List<String>> = HashMap(categoriesList.size)
                    for (category in categoriesList) {
                        val modelList = getCategoryModels(category)
                        modelList?.let {
                            categoryModelMap.put(category, it)
                        }
                    }

                    return@let CategorySyncResults.Success(categoryModelMap)
                }

                return CategorySyncResults.ServerError(response.code(), "")
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "Response code: ${response.code()}, Error Body: ${response.errorBody().toString() ?: ""}")
                }

                return CategorySyncResults.ServerError(response.code(), response.errorBody().toString())
            }
        } catch (exception: IOException) {
            return CategorySyncResults.IOException()
        }

    }

    private fun getCategoryModels(modelName: String): List<String>? {
        val response = api.getModels(modelName).execute()
        if (response.isSuccessful) {
            return response.body()
        }

        return null
    }
}

sealed class CategorySyncResults() {
    class Success(val results: Map<String, List<String>>): CategorySyncResults()
    class ServerError(val code: Int, val error: String): CategorySyncResults()
    class IOException(): CategorySyncResults()
}