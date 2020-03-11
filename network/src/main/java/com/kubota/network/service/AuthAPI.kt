package com.kubota.network.service

import com.kubota.network.BuildConfig
import com.kubota.network.Constants.CLIENT_ID
import com.kubota.network.Constants.CLIENT_SECRET
import com.kubota.network.Constants.GRANT_TYPE
import com.kubota.network.Constants.PASSWORD_NAME
import com.kubota.network.Constants.REFRESH_TOKEN
import com.kubota.network.Constants.USERNAME
import com.kubota.network.model.AuthError
import com.kubota.network.model.AuthResponse
import com.squareup.moshi.JsonAdapter
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

private const val AUTH_PATH = "oauth/token"

class AuthAPI {

    fun signIn(userName: String, password: String) =
        authenticate(AuthType.Password(userName = userName, password = password))

    fun refreshToken(token: String) = authenticate(AuthType.Token(token = token))

    private fun authenticate(authType: AuthType): NetworkResponse<AuthResponse> {
        val request = buildRequest(authType = authType)

        try {
            val expirationOffSet = System.currentTimeMillis() / 1000
            val response = Utils.HTTP_CLIENT.newCall(request).execute()

            when (response.code()) {
                200 -> {
                    val responseBody = response.body()?.string()
                    responseBody?.let {
                        val adapter: JsonAdapter<AuthResponse> = Utils.MOSHI.adapter(AuthResponse::class.java)
                        adapter.fromJson(it)?.let {authResponse ->
                            authResponse.expirationDate += expirationOffSet
                            return NetworkResponse.Success(authResponse)
                        }
                    }

                    return NetworkResponse.ServerError(response.code(), responseBody ?: "")
                }
                400 -> {
                    val responseBody = response.body()?.string()
                    responseBody?.let {
                        val adapter: JsonAdapter<AuthError> = Utils.MOSHI.adapter(AuthError::class.java)
                        val authError = adapter.fromJson(it)
                        return NetworkResponse.ServerError(response.code(), authError?.errorDescription ?: "")
                    }
                    return NetworkResponse.ServerError(response.code(), "")
                }
                401 -> {
                    return NetworkResponse.ServerError(response.code(), "Missing Client_ID or ClientSecret_ID")
                }
                else -> {
                    return NetworkResponse.ServerError(response.code(), response.body()?.string() ?: "")
                }
            }
        } catch (ex: IOException) {
            return NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    private fun buildRequest(authType: AuthType): Request {
        val builder = FormBody.Builder()
            .add(GRANT_TYPE, authType.grantType)
            .add(CLIENT_ID, BuildConfig.CLIENT_ID)
            .add(CLIENT_SECRET, BuildConfig.CLIENT_SECRET)

        val requestBody = when (authType) {
            is AuthType.Password -> {
                builder
                    .add(USERNAME, authType.userName)
                    .add(PASSWORD_NAME, authType.password)
                    .build()
            }
            is AuthType.Token -> {
                builder
                    .add(REFRESH_TOKEN, authType.token)
                    .build()
            }
        }

        return Request.Builder()
            .url("${BuildConfig.AUTH_URL}$AUTH_PATH")
            .post(requestBody)
            .build()
    }
}

private sealed class AuthType(val grantType: String) {
    data class Password(val userName: String, val password: String) : AuthType("password")
    data class Token(val token: String) : AuthType("refresh_token")
}