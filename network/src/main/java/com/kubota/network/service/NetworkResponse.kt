package com.kubota.network.service

sealed class NetworkResponse<T> {
    data class Success<T>(val value: T): NetworkResponse<T>()
    data class ServerError<T>(val code: Int, val message: String): NetworkResponse<T>()
    data class IOException<T>(val message: String): NetworkResponse<T>()
}