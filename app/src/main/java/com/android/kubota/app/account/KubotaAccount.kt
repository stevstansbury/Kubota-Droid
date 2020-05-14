package com.android.kubota.app.account

import com.kubota.service.api.JSONService
import com.kubota.service.domain.auth.OAuthToken

data class KubotaAccount(
    val username: String,
    val authToken: OAuthToken
) {
    companion object {}
}

fun KubotaAccount.Companion.account(prefsValue: String): KubotaAccount? {
    return JSONService().decode(type = KubotaAccount::class.java, value = prefsValue.toByteArray())
}

fun KubotaAccount.prefsValue(): String {
    val bytes = JSONService().encode(value = this) ?: return ""
    return String(bytes)
}
