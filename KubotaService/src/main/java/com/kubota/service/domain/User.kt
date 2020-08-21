package com.kubota.service.domain

// FIXME: Hack to handle snake_case json properties
data class User(
    val email: String,
    val phone_number: String,
    val email_verified: Boolean,
    val mfa_enabled: Boolean
) {
    val phoneNumber: String get() = this.phone_number
    val emailVerified: Boolean get() = this.email_verified
    val mfaEnabled: Boolean get() = this.mfa_enabled
}

