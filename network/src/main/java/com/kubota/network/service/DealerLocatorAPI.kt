package com.kubota.network.service

import com.kubota.network.Constants.Companion.BASE_URL
import com.kubota.network.model.Dealer
import com.kubota.network.model.DealerResponse
import com.squareup.moshi.JsonAdapter
import okhttp3.Request
import java.io.IOException

class DealerLocatorAPI() {

    fun getDealers(latitude: Double = 32.9792895, longitude: Double = -97.0315917, model: String = "", distance: Int = 300): NetworkResponse<List<Dealer>> {
        val request = Request.Builder()
            .url("$BASE_URL/api/dealer/nearest?latitude=$latitude&longitude=$longitude&model=$\"$model\"&distance=$distance")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<DealerResponse> = Utils.MOSHI.adapter(DealerResponse::class.java)
                    val dealerResponse = adapter.fromJson(it)
                    return NetworkResponse.Success(dealerResponse?.dealers ?: emptyList())
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