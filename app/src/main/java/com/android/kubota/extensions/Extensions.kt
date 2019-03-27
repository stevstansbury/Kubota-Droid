package com.android.kubota.extensions

import android.app.Activity
import android.util.Base64
import com.kubota.repository.user.PCASetting
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.UiBehavior


//
// PublicClientApplication extension methods
//
fun PublicClientApplication.login(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, accounts.getUserByPolicy(PCASetting.SignIn().policy),
        UiBehavior.FORCE_LOGIN, null, callback)
}

fun PublicClientApplication.createAccount(activity: Activity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, "", UiBehavior.SELECT_ACCOUNT, null,
        emptyArray<String>(), PCASetting.SignUp().authority, callback)
}

private fun List<IAccount>.getUserByPolicy(policy: String): IAccount? {
    for (user in this) {
        val userIdentifier = user.accountIdentifier.identifier.split("\\.")[0].base64UrlDecode()
        if (userIdentifier.contains(policy.toLowerCase())) {
            return user
        }
    }

    return null
}

private fun String.base64UrlDecode(): String {
    val data = Base64.decode(this, Base64.DEFAULT or Base64.URL_SAFE)
    return String(data, Charsets.UTF_8)
}