package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.ManualMapping
import com.squareup.moshi.JsonAdapter
import okhttp3.Request
import java.io.IOException

class ManualAPI {

    fun getManualMapping(modelName: String): NetworkResponse<ManualMapping> {
        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/Manuals/$modelName")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<ManualMapping> = Utils.MOSHI.adapter(ManualMapping::class.java)
                    val manualMapping = adapter.fromJson(it)
                    if (manualMapping != null) {
                        return NetworkResponse.Success(manualMapping)
                    }
                }

                return NetworkResponse.Success(ManualMapping("", "", emptyList()))
            } else {
                return NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }
}