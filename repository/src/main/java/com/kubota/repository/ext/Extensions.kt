package com.kubota.repository.ext

import android.util.Base64
import com.microsoft.identity.client.IAccount

fun List<IAccount>.getUserByPolicy(policy: String): IAccount? {
    for (user in this) {
        val userIdentifier = user.accountIdentifier.identifier.split("\\.")[0].base64UrlDecode()
        if (userIdentifier.contains(policy.toLowerCase())) {
            return user
        }
    }

    return null
}

fun String.base64UrlDecode(): String {
    val data = Base64.decode(this, Base64.DEFAULT or Base64.URL_SAFE)
    return String(data, Charsets.UTF_8)
}
