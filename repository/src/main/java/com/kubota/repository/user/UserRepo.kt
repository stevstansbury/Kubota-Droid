package com.kubota.repository.user

import android.arch.lifecycle.LiveData
import android.text.TextUtils
import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Account
import com.kubota.repository.data.AccountDao
import com.microsoft.identity.client.*
import kotlinx.coroutines.*

class UserRepo(private val pca: PublicClientApplication, private val accountDao: AccountDao) {

    companion object {
        val SCOPES = arrayOf("https://kubotauser.onmicrosoft.com/api/read",
            "https://kubotauser.onmicrosoft.com/api/write")
    }

    private val databaseScope = CoroutineScope(Dispatchers.IO)

    fun addGuestAccount() {
        launchDataLoad {
            if (accountDao.getAccount() == null) {
                accountDao.insert(Account.createGuestAccount())
            }
        }
    }

    fun getAccount() : LiveData<Account?> = accountDao.getLiveDataAccount()

    fun login(authenticationResult: AuthenticationResult) {
        accountDao.getAccount()?.let { accountDao.deleteAccount(it) }
        accountDao.insert(Account.createAccount(authenticationResult.account.username, authenticationResult.accessToken,
            authenticationResult.expiresOn.time))

        BaseApplication.serviceProxy.accountSync()
    }

    fun logout() {
        accountDao.getAccount()?.let {
            if (!it.isGuest()) {
                for (account in pca.accounts) {
                    //TODO(JC): Should we perhaps remove ALL accounts rather than just selected accounts?
                    if (TextUtils.equals(it.userName, account.username)) {
                        pca.removeAccount(account)
                        break
                    }
                }
            }
            accountDao.deleteAccount(it)
            accountDao.insert(Account.createGuestAccount())
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return databaseScope.launch {
            block()
        }
    }

    fun syncAccount(){
        BaseApplication.serviceProxy.accountSync()
    }
}

sealed class PCASetting(val policy: String) {
    val clientId = "77983c5f-937b-4461-9ff9-896a25616f1a"
    val authority = "https://login.microsoftonline.com/tfp/kubotauser.onmicrosoft.com/$policy"

    class SignIn : PCASetting("B2C_1_kubota-api")
    class SignUp: PCASetting("B2C_1_kubota-sign-up-policy")
    class ResetPassword: PCASetting("B2C_1_SSOPasswordReset")
}