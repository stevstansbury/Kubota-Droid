package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.Dealer
import com.kubota.network.model.Model
import com.kubota.network.model.UserPreference
import com.squareup.moshi.JsonAdapter
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class UserPreferencesAPI {

    companion object {
        private val HTTP_UNAUTHORIZED_CODE = 401
    }
    private val userPrefsAdapter = Utils.MOSHI.adapter<UserPreference>(UserPreference::class.java)
    private val modelAdapter = Utils.MOSHI.adapter<Model>(Model::class.java)
    private val dealerAdapter = Utils.MOSHI.adapter<Dealer>(Dealer::class.java)

    fun updatePreferences(accessToken: String, preference: UserPreference): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get("application/json"), userPrefsAdapter.toJson(preference))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences")
            .addHeader("Authorization", "Bearer $accessToken")
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
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            return parseResponse(response = response)

        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage)
        }
    }

    fun addModel(accessToken: String, model: Model): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get("application/json"), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader("Authorization", "Bearer $accessToken")
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
        val requestBody = FormBody.create(MediaType.get("application/json"), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/edit")
            .addHeader("Authorization", "Bearer $accessToken")
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
        val requestBody = FormBody.create(MediaType.get("application/json"), modelAdapter.toJson(model))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/delete")
            .addHeader("Authorization", "Bearer $accessToken")
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
        val requestBody = FormBody.create(MediaType.get("application/json"), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader("Authorization", "Bearer $accessToken")
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
        val requestBody = FormBody.create(MediaType.get("application/json"), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/api/preferences/add")
            .addHeader("Authorization", "Bearer $accessToken")
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
                    val adapter: JsonAdapter<UserPreference> = Utils.MOSHI.adapter(UserPreference::class.java)
                    val userPrefs = adapter.fromJson(responseBody)
                    userPrefs?.let {
                        return NetworkResponse.Success(it)
                    }
                    return NetworkResponse.ServerError(HTTP_UNAUTHORIZED_CODE, "Null response")
                } else {
                    return NetworkResponse.ServerError(response.code(), responseBody)
                }
            }
        }
        return NetworkResponse.ServerError(response.code(), response.message())
    }

    private fun isTokenExpired(responseBody: String): Boolean = responseBody.contains("AuthenticationFailed")

}