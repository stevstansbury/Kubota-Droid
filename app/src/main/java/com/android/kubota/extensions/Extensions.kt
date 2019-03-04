package com.android.kubota.extensions

import android.app.Activity
import com.android.kubota.MyKubotaApplication
import com.kubota.repository.ext.getUserByPolicy
import com.kubota.repository.user.PCASetting
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationCallback
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

//
// Activity extension methods
//
fun Activity.getPublicClientApplication() = (application as MyKubotaApplication).pca