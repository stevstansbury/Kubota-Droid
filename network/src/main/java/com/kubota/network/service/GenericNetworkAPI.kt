package com.kubota.network.service

import com.kubota.network.service.Utils.HTTP_CLIENT
import okhttp3.Request
import java.io.IOException

class GenericNetworkAPI {

    fun request(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val response = HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                return response.body()?.string()
            }
        } catch (ex: IOException) {}

        return null
    }
}