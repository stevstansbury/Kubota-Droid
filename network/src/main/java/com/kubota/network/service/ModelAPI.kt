package com.kubota.network.service

import com.kubota.network.Constants
import com.squareup.moshi.JsonAdapter
import okhttp3.Request
import java.io.IOException

class ModelAPI {

    fun getCategories(): NetworkResponse<List<String>> {
        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/KubotaModels/categories")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<StringListResponse> = Utils.MOSHI.adapter(StringListResponse::class.java)
                    val stringListResponse = adapter.fromJson(it)
                    if (stringListResponse != null && stringListResponse.categories != null) {
                        return NetworkResponse.Success(stringListResponse.categories)
                    }
                }

                return NetworkResponse.Success(emptyList())
            } else {
                return NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun getModels(category: String): NetworkResponse<List<String>> {
        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/KubotaModels?category=\'$category\"")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<StringListResponse> = Utils.MOSHI.adapter(StringListResponse::class.java)
                    val stringListResponse = adapter.fromJson(it)
                    if (stringListResponse != null && stringListResponse.categories != null) {
                        return NetworkResponse.Success(stringListResponse.categories)
                    }
                }

                return NetworkResponse.Success(emptyList())
            } else {
                return NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

}

private data class StringListResponse(val categories: List<String>?)