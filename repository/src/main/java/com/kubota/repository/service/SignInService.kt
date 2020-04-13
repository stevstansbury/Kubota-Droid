package com.kubota.repository.service

import androidx.annotation.WorkerThread
import com.kubota.network.service.AccountAPI
import com.kubota.network.model.AuthResponse as NetworkAuthResponse
import com.kubota.network.service.NetworkResponse
import com.kubota.repository.user.OAuthToken
import com.kubota.repository.user.UserRepo

class SignInService(private val userRepo: UserRepo) {

    @WorkerThread
    fun signIn(creds: AuthCredentials): AuthResponse {
        val response = AccountAPI.signIn(userName = creds.userName, password = creds.password)
        when (response) {
            is NetworkResponse.Success -> {
                userRepo.login(userName = creds.userName, token = response.value.toOAuthToken())
                return AuthResponse.Success()
            }
            is NetworkResponse.ServerError -> {
                return if (response.code == 400) {
                    AuthResponse.AuthenticationError()
                } else {
                    AuthResponse.GenericError()
                }
            }
            is NetworkResponse.IOException -> {
                return AuthResponse.IOError()
            }
        }
    }
}

private fun NetworkAuthResponse.toOAuthToken(): OAuthToken {
    return OAuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresOn = expirationDate
    )
}

sealed class AuthResponse {
    class Success(): AuthResponse()
    class AuthenticationError: AuthResponse()
    class GenericError: AuthResponse()
    class IOError: AuthResponse()
}

data class AuthCredentials(
    val userName: String,
    val password: String
)