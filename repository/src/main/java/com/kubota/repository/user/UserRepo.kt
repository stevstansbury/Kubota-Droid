package com.kubota.repository.user

import com.kubota.repository.data.Account
import com.kubota.repository.data.AccountDao
import com.kubota.repository.ext.getUserByPolicy
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

class UserRepo(private val pca: PublicClientApplication, private val accountDao: AccountDao) {

    companion object {
        private val SCOPES = arrayOf("https://kubotauser.onmicrosoft.com/api/read",
            "https://kubotauser.onmicrosoft.com/api/write")
    }

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
            logout(account, iAccount)
        }
    }

    fun getAccount() = accountDao.getUIAccount()

    fun updateAccount(account: Account) = accountDao.update(account)

    fun login(authenticationResult: AuthenticationResult) {
        accountDao.insert(Account(1, authenticationResult.account.username, authenticationResult.accessToken, authenticationResult.expiresOn.time))
    }

    fun logout() {
        logout(accountDao.getAccount(), pca.accounts[0])
    }

    private fun logout(account: Account?, iAccount: IAccount?) {
        account?.let { accountDao.deleteAccount(it) }
        iAccount?.let { pca.removeAccount(it) }
    }
}

sealed class PCASetting(val policy: String) {
    val clientId = "77983c5f-937b-4461-9ff9-896a25616f1a"
    val authority = "https://login.microsoftonline.com/tfp/kubotauser.onmicrosoft.com/${policy}"

    class SignIn(): PCASetting("B2C_1_kubota-api")
    class SignUp(): PCASetting("B2C_1_kubota-sign-up-policy")
}