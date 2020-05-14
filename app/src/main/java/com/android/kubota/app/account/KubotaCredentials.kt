package com.android.kubota.app.account

// For biometrics authentication only
data class KubotaCredentials(
    val username: String,
    val passwordval: String
) {
    companion object {}
}

fun KubotaCredentials.Companion.account(prefsValue: String): KubotaCredentials? {
    TODO()
}

fun KubotaCredentials.prefsValue(): String {
    TODO()
}
