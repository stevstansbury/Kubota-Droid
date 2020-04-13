package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.FaultCodeApiResponse
import com.kubota.network.model.ManualMapping
import com.squareup.moshi.JsonAdapter
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException

interface FaultCodeAPI {
    fun getFaultCodeForModel(codes: List<Int>): NetworkResponse<FaultCodeApiResponse>
}

interface ManualsAPI {
    fun getManualMapping(): NetworkResponse<ManualMapping>
}

object EquipmentAPIFactory {
    fun getFaultCodeAPI(model: String): FaultCodeAPI = createEquipmentService(model)

    fun getManualsAPI(model: String): ManualsAPI = createEquipmentService(model)

    private fun createEquipmentService(model: String) = EquipmentService(model)
}

internal class EquipmentService(private val model: String): FaultCodeAPI, ManualsAPI {

    override fun getManualMapping(): NetworkResponse<ManualMapping> {
        val request = buildRequest(RequestType.Manuals())

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

    override fun getFaultCodeForModel(codes: List<Int>): NetworkResponse<FaultCodeApiResponse> {
        val request = buildRequest(RequestType.FaultCodes(codes))

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

    private fun buildRequest(requestType: RequestType): Request {
        return when (requestType) {
            is RequestType.Manuals -> {
                Request.Builder()
                    .url("${Constants.BASE_URL}/api/Manuals/$model")
                    .build()
            }
            is RequestType.FaultCodes -> {
                //TODO: Update URL for LOCAL_HOST
                val url = HttpUrl.get("${Constants.LOCAL_HOST}/api/faultCode/$model").newBuilder()
                requestType.codes.forEach {code ->
                    url.addQueryParameter("code", "$code")
                }
                return Request.Builder()
                    .url(url.build())
                    .build()
            }
        }
    }

}

private sealed class RequestType {
    class Manuals: RequestType()
    class FaultCodes(val codes: List<Int>): RequestType()
}