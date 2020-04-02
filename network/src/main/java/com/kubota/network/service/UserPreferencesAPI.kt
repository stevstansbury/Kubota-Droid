package com.kubota.network.service

import com.kubota.network.Constants
import com.kubota.network.model.Dealer
import com.kubota.network.model.Equipment
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

    private val userPrefsAdapter = Utils.MOSHI.adapter(UserPreference::class.java)
    private val modelAdapter = Utils.MOSHI.adapter(Equipment::class.java)
    private val dealerAdapter = Utils.MOSHI.adapter(Dealer::class.java)

    fun getPreferences(accessToken: String): NetworkResponse<UserPreference> {
        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun addEquipment(accessToken: String, equipment: Equipment): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(equipment))

        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences/model/add")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }

    }

    fun updateEquipment(accessToken: String, equipment: Equipment): NetworkResponse<UserPreference> {

        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(equipment))

        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences/model/update")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }

    }

    fun deleteEquipment(accessToken: String, equipment: Equipment): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), modelAdapter.toJson(equipment))

        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences/model/delete")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun addDealer(accessToken: String, dealer: Dealer): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences/dealer/add")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun deleteDealer(accessToken: String, dealer: Dealer): NetworkResponse<UserPreference> {
        val requestBody = FormBody.create(MediaType.get(MEDIA_TYPE_APPLICATION_JSON), dealerAdapter.toJson(dealer))

        val request = Request.Builder()
            .url("${Constants.STAGGING_URL}/api/user/preferences/dealer/delete")
            .addHeader(AUTH_HEADER_KEY, "Bearer $accessToken")
            .post(requestBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            parseResponse(response = response)

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
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
                    return NetworkResponse.ServerError(response.code(), responseBody)
                } else {
                    return NetworkResponse.ServerError(HTTP_UNAUTHORIZED_CODE, NULL_RESPONSE_BODY)
                }
            }
        }
        return NetworkResponse.ServerError(response.code(), response.message())
    }

    private fun isTokenExpired(responseBody: String): Boolean = responseBody.contains(AUTHENTICATION_FAILED)

}