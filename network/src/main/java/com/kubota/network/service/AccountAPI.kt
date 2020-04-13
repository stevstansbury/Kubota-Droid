package com.kubota.network.service

import com.kubota.network.BuildConfig
import com.kubota.network.Constants
import com.kubota.network.model.*
import com.squareup.moshi.JsonAdapter
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

private const val AUTH_PATH = "oauth/token"

object AccountAPI {

    private val ERROR_ADAPTER: JsonAdapter<AuthError> = Utils.MOSHI.adapter(AuthError::class.java)

    fun refreshToken(token: String) = authenticate(AuthType.Token(token = token))

    fun forgotPassword(email:String): NetworkResponse<String> {
        val body = FormBody.Builder()
            .add("email", email)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.AUTH_URL}oauth/forgot_password")
            .post(body)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            when (response.code()) {
                200 -> {
                    response.body()?.string()?.let {
                        Utils.MOSHI.adapter(Token::class.java).fromJson(it)?.let {token ->
                            NetworkResponse.Success(token.token)
                        }
                    } ?: NetworkResponse.ServerError(500, "")
                }
                else -> {
                    val errorMessage = response.body()?.string()?.let {
                        ERROR_ADAPTER.fromJson(it)?.errorDescription
                    } ?: ""

                    NetworkResponse.ServerError(response.code(), errorMessage)
                }
            }

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String
    ): NetworkResponse<Unit> {
        val body = FormBody.Builder()
            .add("password", newPassword)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.AUTH_URL}oauth/user")
            .addHeader(Constants.AUTH_HEADER_KEY, "Bearer $accessToken")
            .patch(body)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            when (response.code()) {
                200 -> NetworkResponse.Success(Unit)
                else -> {
                    val errorMessage = response.body()?.string()?.let {
                        ERROR_ADAPTER.fromJson(it)?.errorDescription
                    } ?: ""

                    NetworkResponse.ServerError(response.code(), errorMessage)
                }
            }

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

    fun signIn(userName: String, password: String) =
        authenticate(AuthType.Password(userName = userName, password = password))

    fun resetPassword(token: String, code: String, newPassword: String): NetworkResponse<Unit> {
        val formBody = FormBody.Builder()
            .add("token", token)
            .add("code", code)
            .add("new_password", newPassword)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.AUTH_URL}oauth/reset_password")
            .put(formBody)
            .build()

        return try {
            val response = Utils.HTTP_CLIENT.newCall(request).execute()
            when (response.code()) {
                200 -> NetworkResponse.Success(Unit)
                else -> NetworkResponse.ServerError(response.code(), response.message())
            }

        } catch (ex: IOException) {
            NetworkResponse.IOException(ex.localizedMessage ?: "")
        }
    }

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
                        val authError = ERROR_ADAPTER.fromJson(it)
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
            .add(Constants.GRANT_TYPE, authType.grantType)
            .add(Constants.CLIENT_ID, BuildConfig.CLIENT_ID)
            .add(Constants.CLIENT_SECRET, BuildConfig.CLIENT_SECRET)

        val requestBody = when (authType) {
            is AuthType.Password -> {
                builder
                    .add(Constants.USERNAME, authType.userName)
                    .add(Constants.PASSWORD_NAME, authType.password)
                    .build()
            }
            is AuthType.Token -> {
                builder
                    .add(Constants.REFRESH_TOKEN, authType.token)
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