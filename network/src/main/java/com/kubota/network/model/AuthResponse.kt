package com.kubota.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "refresh_token")
    val refreshToken: String,
    @Json(name = "expires_in")
    var expirationDate: Long,
    val scope: String
)

@JsonClass(generateAdapter = true)
open class Error(
    val error: String
)

@JsonClass(generateAdapter = true)
class AuthError(
    error: String,
    @Json(name = "error_description")
    val errorDescription: String
): Error(error)