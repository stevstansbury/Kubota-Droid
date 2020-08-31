package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.EquipmentModel
import com.kubota.network.model.EquipmentModelResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import okhttp3.CacheControl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelAPI {

    fun getModels(): NetworkResponse<List<EquipmentModel>> {
        val cacheControl = CacheControl.Builder()
            .maxStale(1, TimeUnit.DAYS)
            .build()

        val request = Request.Builder()
            .cacheControl(cacheControl)
            .url("${Constants.STAGGING_URL}/api/models/")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<EquipmentModelResult> = Utils.MOSHI.adapter(EquipmentModelResult::class.java)
                    val stringListResponse = adapter.fromJson(it)?.models ?: emptyList()

                    return NetworkResponse.Success(stringListResponse)
                }

                return NetworkResponse.Success(emptyList())
            } else {
                return NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun getCategories(): NetworkResponse<List<String>> {
        val cacheControl = CacheControl.Builder()
            .maxStale(1, TimeUnit.DAYS)
            .build()
        return makeRequest(Request.Builder()
            .cacheControl(cacheControl)
            .url("${Constants.BASE_URL}/api/KubotaModels/categories")
            .build())
    }

    fun getModelNames(category: String): NetworkResponse<List<String>> {
        val cacheControl = CacheControl.Builder()
            .maxStale(12, TimeUnit.HOURS)
            .build()
        return makeRequest(Request.Builder()
            .cacheControl(cacheControl)
            .url("${Constants.BASE_URL}/api/KubotaModels?category=$category")
            .build())
    }


    private fun makeRequest(request: Request): NetworkResponse<List<String>> {
        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val type = Types.newParameterizedType(List::class.java, String::class.java)
                    val adapter: JsonAdapter<List<String>> = Utils.MOSHI.adapter(type)
                    val stringListResponse = adapter.fromJson(it)
                    if (stringListResponse != null) {
                        return NetworkResponse.Success(stringListResponse)
                    }
                }

                return NetworkResponse.Success(emptyList())
            } else {
                return NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }
}
