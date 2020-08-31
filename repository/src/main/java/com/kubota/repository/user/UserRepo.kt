package com.kubota.repository.user

import androidx.lifecycle.LiveData
import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Account
import com.kubota.repository.data.AccountDao
import kotlinx.coroutines.*

class UserRepo(private val accountDao: AccountDao) {

    private val databaseScope = CoroutineScope(Dispatchers.IO)

    fun addGuestAccount() {
        launchDataLoad {
            if (accountDao.getAccount() == null) {
                accountDao.insert(Account.createGuestAccount())
            }
        }
    }

    fun getAccount() : LiveData<Account?> = accountDao.getLiveDataAccount()

    fun login(userName: String, token: OAuthToken) {
        accountDao.getAccount()?.let { accountDao.deleteAccount(it) }
        accountDao.insert(Account.createAccount(userName, token.accessToken, token.expiresOn,
            token.refreshToken))

        BaseApplication.serviceProxy.accountSync()
    }

    fun logout() {
        accountDao.getAccount()?.let {
            if (it.isGuest().not()) {
                accountDao.deleteAccount(it)
                accountDao.insert(Account.createGuestAccount())
            }
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

data class OAuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresOn: Long
)