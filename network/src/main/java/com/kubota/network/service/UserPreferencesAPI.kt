package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.Dealer
import com.kubota.network.model.Model
import com.kubota.network.model.UserPreference
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private const val HTTP_UNAUTHORIZED_CODE = 401
private const val AUTH_HEADER_KEY = "Authorization"
private const val MEDIA_TYPE_APPLICATION_JSON = "application/json"
private const val AUTHENTICATION_FAILED = "AuthenticationFailed"
private const val NULL_RESPONSE_BODY = "Null response"

class UserPreferencesAPI {

    private val userPrefsAdapter = Utils.MOSHI.adapter<UserPreference>(UserPreference::class.java)
    private val modelAdapter = Utils.MOSHI.adapter<Model>(Model::class.java)
    private val dealerAdapter = Utils.MOSHI.adapter<Dealer>(Dealer::class.java)

    fun updatePreferences(accessToken: String, preference: UserPreference): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), userPrefsAdapter.toJson(preference))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun getPreferences(accessToken: String): NetworkResponse<UserPreference> {
        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun addModel(accessToken: String, model: Model): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }

    }

    fun editModel(accessToken: String, model: Model): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/edit")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }

    }

    fun deleteModel(accessToken: String, model: Model): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/delete")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun addDealer(accessToken: String, dealer: Dealer): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun deleteDealer(accessToken: String, dealer: Dealer): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    private fun parseResponse(response: Response): NetworkResponse<UserPreference> {
        if (response.isSuccessful) {
            response.body()?.let { body ->
                val responseBody = body.string()
                if (!isTokenExpired(responseBody)) {
                    val userPrefs = userPrefsAdapter.fromJson(responseBody)
                    userPrefs?.let {

                        return NetworkResponse.Success(it)
                    }
                    return NetworkResponse.ServerError(HTTP_UNAUTHORIZED_CODE, NULL_RESPONSE_BODY)
                } else {
                    return NetworkResponse.ServerError(response.code(), responseBody)
                }
            }
        }
        return NetworkResponse.ServerError(response.code(), response.message())
    }

    private fun isTokenExpired(responseBody: String): Boolean = responseBody.contains(AUTHENTICATION_FAILED)

}