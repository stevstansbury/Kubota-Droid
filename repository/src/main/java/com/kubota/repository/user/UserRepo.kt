package com.kubota.repository.user

import android.app.Activity
import com.kubota.repository.ext.getUserByPolicy
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.UiBehavior
import com.microsoft.identity.client.exception.MsalException

class UserRepo(private val pca: PublicClientApplication) {

    companion object {
        private val SCOPES = arrayOf("https://kubotauser.onmicrosoft.com/api/read",
            "https://kubotauser.onmicrosoft.com/api/write")
    }

    fun silentLogin() {
        val account = pca.accounts.getUserByPolicy(PCASetting.SignIn().policy)
        if (account != null) {
            pca.acquireTokenSilentAsync(SCOPES, account, object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: AuthenticationResult?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onCancel() {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onError(exception: MsalException?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        } else {
            // We have no user. We should remove the user from the local DB.
        }
    }
}

sealed class PCASetting(val policy: String) {
    val clientId = "77983c5f-937b-4461-9ff9-896a25616f1a"
    val authority = "https://login.microsoftonline.com/tfp/kubotauser.onmicrosoft.com/${policy}"

    class SignIn(): PCASetting("B2C_1_kubota-api")
    class SignUp(): PCASetting("B2C_1_kubota-sign-up-policy")
}