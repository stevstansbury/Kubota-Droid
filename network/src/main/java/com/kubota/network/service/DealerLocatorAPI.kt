package com.kubota.network.service

import com.kubota.network.Constants.BASE_URL
import com.kubota.network.model.Dealer
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import okhttp3.Request
import java.io.IOException

class DealerLocatorAPI() {

    fun getDealers(latitude: Double, longitude: Double, model: String = "", distance: Int = 300): NetworkResponse<List<Dealer>> {
        val request = Request.Builder()
            .url("$BASE_URL/api/dealer/nearest?latitude=$latitude&longitude=$longitude&model=$\"$model\"&distance=$distance")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                responseBody?.let {
                    val adapter: JsonAdapter<Array<Dealer>?> = Utils.MOSHI.adapter(Array<Dealer>::class.java)
                    val dealerResponse = adapter.fromJson(it)
                    return NetworkResponse.Success(dealerResponse?.toList() ?: emptyList())
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

@JsonClass(generateAdapter = true)
data class DealerResponse (val dealers: Array<Dealer>?)