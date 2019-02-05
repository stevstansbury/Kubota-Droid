package com.android.kubota.extensions

import android.support.v7.app.AppCompatActivity
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
fun PublicClientApplication.login(activity: AppCompatActivity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, accounts.getUserByPolicy(PCASetting.SignIn().policy),
        UiBehavior.CONSENT, null, callback)
}

fun PublicClientApplication.createAccount(activity: AppCompatActivity, callback: AuthenticationCallback) {
    acquireToken(activity, UserRepo.SCOPES, "", UiBehavior.SELECT_ACCOUNT, null,
        emptyArray<String>(), PCASetting.SignUp().authority, callback)
}

//
// Activity extension methods
//
fun AppCompatActivity.getPublicClientApplication() = (application as MyKubotaApplication).pca