package com.kubota.network.service

import com.kubota.network.BuildConfig
import com.kubota.network.Constants.EMAIL_NAME
import com.kubota.network.Constants.PASSWORD_NAME
import com.kubota.network.model.Error
import com.squareup.moshi.JsonAdapter
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException

private const val ACCOUNT_CREATION_PATH = "oauth/user"

class NewAccountAPI {

    fun createAccount(email: String, password: String): NetworkResponse<Unit> {
        val request = buildRequest(email, password)

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return when (response.code()) {
                200 -> NetworkResponse.Success(Unit)
                else -> NetworkResponse.ServerError(response.code(), parseResponse(response.body()))
            }
        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    private fun buildRequest(email: String, password: String): Request {
        val body = FormBody.Builder()
            .add(EMAIL_NAME, email)
            .add(PASSWORD_NAME, password)
            .build()

        return Request.Builder()
            .url("${BuildConfig.AUTH_URL}$ACCOUNT_CREATION_PATH")
            .post(body)
            .build()
    }

    private fun parseResponse(responseBody: ResponseBody?): String {
        return responseBody?.string()?.let {
            val adapter: JsonAdapter<Error> = Utils.MOSHI.adapter(Error::class.java)
            adapter.fromJson(it)?.error
        } ?: ""

    }
}