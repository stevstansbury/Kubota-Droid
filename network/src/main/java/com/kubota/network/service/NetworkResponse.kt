package com.kubota.network.service

sealed class NetworkResponse<out T> {
    data class Success<T>(val value: T): NetworkResponse<T>()
    data class ServerError(val code: Int, val message: String): NetworkResponse<Nothing>()
    data class IOException(val message: String): NetworkResponse<Nothing>()
}