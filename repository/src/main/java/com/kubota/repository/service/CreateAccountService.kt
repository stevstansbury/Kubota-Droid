package com.kubota.repository.service

import androidx.annotation.WorkerThread
import com.kubota.network.service.NetworkResponse
import com.kubota.network.service.NewAccountAPI

private const val BLACKLISTED_PASSWORD = "Blacklisted password"

class CreateAccountService {

    private val api = NewAccountAPI()

    @WorkerThread
    fun createAccount(email: String, password: String): Response {
        return when (val results = api.createAccount(email, password)) {
            is NetworkResponse.Success -> Response.Success()
            is NetworkResponse.IOException -> Response.IOError()
            is NetworkResponse.ServerError -> {
                when (results.code) {
                    400 -> {
                        if (results.message.contains(BLACKLISTED_PASSWORD))
                            Response.BlacklistedPassword()
                        else
                            Response.InvalidPassword()
                    }
                    409 -> Response.DuplicateAccount()
                    else ->Response.GenericError()
                }
            }
        }
    }
}

sealed class Response {
    class Success: Response()
    class DuplicateAccount: Response()
    class BlacklistedPassword: Response()
    class InvalidPassword: Response()
    class GenericError: Response()
    class IOError: Response()
}