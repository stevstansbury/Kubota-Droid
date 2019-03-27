package com.kubota.repository.ext

import android.content.Context
import com.kubota.repository.BaseApplication
import com.microsoft.identity.client.IAccount

fun List<IAccount>.getUserByPolicy(policy: String): IAccount? {
    for (user in this) {
        val userIdentifier = user.homeAccountIdentifier.identifier
        if (userIdentifier.contains(policy.toLowerCase())) {
            return user
        }
    }

    return null
}

fun Context.getPublicClientApplication() = (applicationContext as BaseApplication).pca