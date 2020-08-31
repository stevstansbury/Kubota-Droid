package com.kubota.service.domain

import com.squareup.moshi.Json

data class User(
    val email: String,
    @Json(name="phone_number")
    val phoneNumber: String?,
    @Json(name="email_verified")
    val emailVerified: Boolean,
    @Json(name="mfa_enabled")
    val mfaEnabled: Boolean
)
