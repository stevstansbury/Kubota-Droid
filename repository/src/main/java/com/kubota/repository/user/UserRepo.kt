package com.kubota.repository.user

import android.arch.lifecycle.LiveData
import android.text.TextUtils
import com.kubota.repository.data.Account
import com.kubota.repository.data.AccountDao
import com.kubota.repository.ext.getUserByPolicy
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.*

class UserRepo(private val pca: PublicClientApplication, private val accountDao: AccountDao) {

    companion object {
        val SCOPES = arrayOf("https://kubotauser.onmicrosoft.com/api/read",
            "https://kubotauser.onmicrosoft.com/api/write")
    }

    private val databaseScope = CoroutineScope(Dispatchers.IO)

    private fun silentLogin() {
        val account = accountDao.getAccount()
        val iAccount = pca.accounts.getUserByPolicy(PCASetting.SignIn().policy)
        if (account != null && iAccount != null) {
            pca.acquireTokenSilentAsync(SCOPES, iAccount, object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: AuthenticationResult?) {
                    account.flags = Account.FLAGS_TOKEN_EXPIRED
                    authenticationResult?.let {
                        account.accessToken = it.accessToken
                        account.expireDate = it.expiresOn.time
                    }
                    updateAccount(account)
                }

                override fun onCancel() {
                    account.flags = Account.FLAGS_TOKEN_EXPIRED
                    updateAccount(account)
                }

                override fun onError(exception: MsalException?) {
                    account.flags = Account.FLAGS_TOKEN_EXPIRED
                    updateAccount(account)
                }
            })
        } else {
            logout()
        }
    }

    fun getAccount() : LiveData<Account?> {
        launchDataLoad {
            if (accountDao.getAccount() == null) {
                accountDao.insert(Account.createGuestAccount())
            }
        }

        return accountDao.getUIAccount()
    }

    fun updateAccount(account: Account) {
        accountDao.update(account)
    }

    fun login(authenticationResult: AuthenticationResult) {
        accountDao.getAccount()?.let { accountDao.deleteAccount(it) }
        accountDao.insert(Account.createAccount(authenticationResult.account.username, authenticationResult.accessToken,
            authenticationResult.expiresOn.time))
    }

    fun logout() {
        accountDao.getAccount()?.let {
            if (!it.isGuest()) {
                for (account in pca.accounts) {
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
}

sealed class PCASetting(val policy: String) {
    val clientId = "77983c5f-937b-4461-9ff9-896a25616f1a"
    val authority = "https://login.microsoftonline.com/tfp/kubotauser.onmicrosoft.com/${policy}"

    class SignIn(): PCASetting("B2C_1_kubota-api")
    class SignUp(): PCASetting("B2C_1_kubota-sign-up-policy")
}