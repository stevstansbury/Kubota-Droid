package com.kubota.network.service

import okhttp3.Request
import java.io.IOException

class GenericNetworkAPI {

    fun request(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            if (response.isSuccessful) {
                return response.body()?.string()
            }
        } catch (ex: IOException) {}

        return null
    }
}