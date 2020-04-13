package com.kubota.repository.service

import androidx.annotation.WorkerThread
import com.kubota.network.service.AccountAPI
import com.kubota.network.service.NetworkResponse

internal const val INVALID_TOKEN = "Invalid Token"
internal const val MISSING_TOKEN = "Missing Token"

internal const val INVALID_CODE = "Reset password code entered is invalid"
internal const val MISSING_CODE = "Missing reset password code"

class ChangePasswordService {

    @WorkerThread
    fun requestResetCode(email: String): Result {
        return when(val networkResponse = AccountAPI.forgotPassword(email)) {
            is NetworkResponse.Success -> Result.Success(networkResponse.value)
            is NetworkResponse.ServerError -> {
                when (networkResponse.code) {
                    500 -> Result.InvalidEmail()
                    else -> Result.Error()
                }
            }
            is NetworkResponse.IOException -> Result.NetworkError()
        }
    }

    @WorkerThread
    fun changePassword(requestType: RequestType): Result {
        return when (requestType) {
            is RequestType.ChangePassword ->  changePassword(requestType)
            is RequestType.ResetPassword -> resetPassword(requestType)
        }
    }

    private fun resetPassword(request: RequestType.ResetPassword): Result {
        val networkResponse = AccountAPI.resetPassword(
            token = request.accessToken,
            code = request.code,
            newPassword = request.newPassword
        )

        return parseResponse(networkResponse)
    }

    private fun changePassword(request: RequestType.ChangePassword): Result {
        val networkResponse = AccountAPI.changePassword(
            accessToken = request.accessToken,
            currentPassword = request.currentPassword,
            newPassword = request.newPassword
        )

        return parseResponse(networkResponse)
    }

    internal fun parseResponse(response: NetworkResponse<*>): Result {
        return when(response) {
            is NetworkResponse.Success -> Result.Success("")
            is NetworkResponse.ServerError -> {
                when(response.code) {
                    400 -> {
                        val tokenErrors = arrayOf(INVALID_TOKEN, MISSING_TOKEN)
                        val codeErrors = arrayOf(INVALID_CODE, MISSING_CODE)
                        if (response.message.contains(tokenErrors)) {
                            Result.InvalidToken()
                        } else if (response.message.contains(codeErrors)) {
                            Result.InvalidCode()
                        } else {
                            Result.InvalidPassword()
                        }
                    }
                    else -> Result.Error()
                }
            }
            is NetworkResponse.IOException -> Result.NetworkError()
        }
    }
}

sealed class Result {
    data class Success(val response: String? = null): Result()
    class NetworkError: Result()
    class Error(): Result()
    // Used for Reset Password Only
    class InvalidToken: Result()
    class InvalidCode: Result()
    class InvalidPassword: Result()

    class InvalidEmail: Result()
}

sealed class RequestType(val accessToken: String, val newPassword: String) {

    class ResetPassword(
        val code: String,
        accessToken: String,
        newPassword: String
    ): RequestType(accessToken, newPassword)

    class ChangePassword(
        val currentPassword: String,
        accessToken: String,
        newPassword: String
    ): RequestType(accessToken, newPassword)
}

private fun String.contains(others: Array<String>, ignoreCase: Boolean = true): Boolean {
    others.forEach {
        if (contains(it, ignoreCase)) return true
    }
    return false
}


