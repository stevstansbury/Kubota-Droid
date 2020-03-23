package com.kubota.network.service

import com.kubota.network.Constants.LOCAL_HOST
import com.kubota.network.model.FaultCodeApiResponse

import com.squareup.moshi.JsonAdapter
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException

class FaultCodeApi {
    fun getFaultCodeForModel(model: String, codes: List<Int>): NetworkResponse<FaultCodeApiResponse> {
        val request = buildRequest(model, codes)

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return when (response.code()) {
                200 -> {
                    val responseBody = response.body()?.string()
                    responseBody?.let {
                        val adapter: JsonAdapter<FaultCodeApiResponse> = Utils.MOSHI.adapter(FaultCodeApiResponse::class.java)
                        val faultCodeResponse = adapter.fromJson(it)
                        if (faultCodeResponse != null) {
                            return NetworkResponse.Success(faultCodeResponse)
                        }
                    }
                    return NetworkResponse.ServerError(response.code(), response.message() ?: "")
                }
                else -> NetworkResponse.ServerError(response.code(), response.message() ?: "")
            }
        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    private fun buildRequest(model: String, codes: List<Int>): Request {
        //TODO: Update URL for LOCAL_HOST
        val url = HttpUrl.get("$LOCAL_HOST/api/faultCode/$model").newBuilder()
        codes.forEach {code->
            url.addQueryParameter("code", "$code")
        }
        return Request.Builder()
            .url(url.build())
            .build()
    }
}