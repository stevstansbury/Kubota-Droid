package com.android.kubota.app.account

import android.annotation.SuppressLint
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountManager.Companion.PREFERENCE_KEY_ACCOUNT
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.security.HexEncoder
import com.inmotionsoftware.foundation.security.KeyStoreService
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.auth.ResetPasswordToken
import com.kubota.service.domain.preference.UserSettings
import java.lang.ref.WeakReference

interface AccountManagerDelegate {
    fun didAuthenticate(token: OAuthToken): Guarantee<Unit>
    fun willUnauthenticate()
    fun didUnauthenticate()
}

sealed class AccountError: Throwable {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(cause: Throwable): super(cause)
    constructor(message: String, cause: Throwable): super(message, cause)

    class InvalidCredentials(message: String = ""): AccountError(message)
    class InvalidEmail(message: String = ""): AccountError(message)
    class InvalidPassword(message: String = ""): AccountError(message)
    class InvalidPasswordResetCode(message: String = ""): AccountError(message)
    class InvalidPasswordResetToken(message: String = ""): AccountError(message)
    class AccountExists(message: String = ""): AccountError(message)
    class InvalidPhoneNumber(message: String = ""): AccountError(message)
    class NotMobilePhoneNumber(message: String = ""): AccountError(message)
    class BlacklistedPassword(message: String = ""): AccountError(message)
}



class AccountManager(private val delegate: AccountManagerDelegate? = null) {

    companion object {
        const val PREFERENCE_KEY_USERNAME = "CryptoDataUsername"
        const val PREFERENCE_KEY_PASSWORD = "CryptoDataPassword"
        const val PREFERENCE_KEY_ACCOUNT = "CryptoDataAccount"
    }

    enum class CrpAlias(val alias: String) {
        OAUTH_ALIAS("com.kubota.android.oauth"),
        ACCOUNT_ALIAS("com.kubota.android.kubotaAccount"),
        BIOMETRIC_ALIAS("com.kubota.android.biometric")
    }

    private val mIsAuthenticated = MutableLiveData(false)

    private val mIsVerified = MutableLiveData<Boolean>()

    private val accountDelegate =
        if (this.delegate != null) WeakReference(this.delegate) else null

    var account: KubotaAccount? = null
        private set(newValue) {
            field = newValue
            this.mIsAuthenticated.value = newValue?.authToken?.accessToken?.isNotBlank() ?: false
        }

    val authToken: OAuthToken?
        get() {
            return this.account?.authToken
        }

    val isAuthenticated: LiveData<Boolean> = mIsAuthenticated

    val isVerified: LiveData<Boolean> = mIsVerified

    init {
        val preferences = AppProxy.proxy.preferences
        if (preferences.firstTimeUsed) {
            preferences.firstTimeUsed = false
            this.clearAccountPreferences()
        }

        this.account = this.accountFromPreferences()
    }

    fun authenticate(username: String, password: String): Promise<Unit> {
        return AppProxy.proxy.serviceManager.authService.authenticate(username = username, password = password)
                       .done { this.didAuthenticate(username = username, authToken = it) }
                        .recover { error ->
                            when (error) {
                                is KubotaServiceError.BadRequest -> {
                                    when {
                                        error.message?.contains("invalid_grant") == true ->
                                            throw AccountError.InvalidCredentials()
                                        else -> throw AccountError.InvalidPassword()
                                    }
                                }
                                else -> throw error
                            }
                        }
    }

    fun reauthenticate(): Promise<Unit> {
        val account = this.account ?: return Promise(error = KubotaServiceError.Unauthorized(message = "No refresh token"))
        return AppProxy.proxy.serviceManager.authService.authenticate(token = account.authToken)
                       .done {
                            this.didAuthenticate(username = account.username, authToken = it, silentReauth = true)
                        }
    }

    fun resetPassword(token: ResetPasswordToken, verificationCode: String, newPassword: String): Promise<Unit> {
        return AppProxy.proxy.serviceManager.authService.resetPassword(token = token, verificationCode = verificationCode, newPassword = newPassword)
            .recover { error ->
                when (error) {
                    is KubotaServiceError.BadRequest -> {
                        when {
                            error.message?.contains("Reset password code entered is invalid") == true ->
                                throw AccountError.InvalidPasswordResetCode()
                            error.message?.contains("Invalid token") == true ->
                                throw AccountError.InvalidPasswordResetToken()
                            else -> throw error
                        }
                    }
                    else -> throw error
                }
            }
    }

    fun sendForgotPasswordVerificationCode(email: String): Promise<ResetPasswordToken> {
        return AppProxy.proxy.serviceManager.authService.requestForgotPasswordVerificationCode(email = email)
                       .recover { error ->
                           when (error) {
                               is KubotaServiceError.ServerError -> throw AccountError.InvalidEmail()
                               else -> throw error
                           }
                       }
    }

    fun createAccount(email: String, password: String, phoneNumber: String): Promise<Unit> {
        return AppProxy.proxy.serviceManager.authService.createAccount(email = email, password = password, phoneNumber = phoneNumber)
            .then { this.authenticate(username = email, password = password) }
            .recover { error ->
                when (error) {
                    is KubotaServiceError.Conflict -> throw AccountError.AccountExists()
                    else -> {
                        if (error.message?.contains("Blacklisted password") == true) {
                            throw AccountError.BlacklistedPassword()
                        }
                        throw error
                    }
                }
            }
    }

    fun changePassword(currentPassword: String, newPassword: String): Promise<Unit> {
        // The auth token does not get invalidated when changing password, so we don't
        // have to reauthenticate to get a new auth token
        return AppProxy.proxy.serviceManager.authService
                       .changePassword(currentPassword = currentPassword, newPassword = newPassword)
    }

    fun logout(): Promise<Unit> {
        return AuthPromise()
                .then {
                    @SuppressLint("HardwareIds")
                    val deviceId = Settings.Secure.getString(AppProxy.proxy.contentResolver, Settings.Secure.ANDROID_ID)
                    AppProxy.proxy.serviceManager.userPreferenceService.deregisterFCMToken(deviceId)
                }
                .recover {
                    Promise.value(Unit)
                }
                .then {
                    AppProxy.proxy.serviceManager.authService.logout()
                }
                .map(on = DispatchExecutor.global) {
                    this.clearAccountPreferences()
                    AppProxy.proxy.preferences.guidesDisclaimerAccepted = false
                }
                .done {
                    // Must set LiveData value on main thread
                    this.account = null
                    this.accountDelegate?.get()?.didUnauthenticate()
                }
    }

    private fun didAuthenticate(username: String, authToken: OAuthToken, silentReauth: Boolean = false) {
        if (!silentReauth) {
            AppProxy.proxy.preferences.guidesDisclaimerAccepted = true
        }
        val account = KubotaAccount(username = username, authToken = authToken)
        this.saveAccountToPreferences(account)
        this.account = account

        this.accountDelegate?.get()?.didAuthenticate(token = authToken)?.done {
            AppProxy.proxy.apply {
                fcmToken?.let {
                    @SuppressLint("HardwareIds")
                    val deviceId = Settings.Secure.getString(AppProxy.proxy.contentResolver, Settings.Secure.ANDROID_ID)
                    serviceManager.userPreferenceService.registerFCMToken(it, deviceId)
                }
            }
        }
    }

    fun refreshUserSettings(): Promise<UserSettings> {
        return AuthPromise()
            .then { AppProxy.proxy.serviceManager.userPreferenceService.getUserSettings() }
    }

}

private fun AccountManager.clearAccountPreferences() {
    AppProxy.proxy.preferences.clearKey(PREFERENCE_KEY_ACCOUNT)
}

private fun AccountManager.accountFromPreferences(): KubotaAccount? {
    return try {
        val alias = AccountManager.CrpAlias.ACCOUNT_ALIAS.alias
        val key = PREFERENCE_KEY_ACCOUNT

        val (encryptedDataString, encryptedIvString) = AppProxy.proxy.preferences.getCryptoDataFor(key)
        val cryptoData = KeyStoreService.CryptoData(
            encryption = HexEncoder.toBytes(encryptedDataString)
            , iv = HexEncoder.toBytes(encryptedIvString)
        )
        val prefsValue = KeyStoreService.decrypt(alias = alias, cryptoData = cryptoData)
        KubotaAccount.account(prefsValue)
    } catch (ignored: Exception) {
        Log.e("AccountManager", ignored.toString())
        null
    }
}

private fun AccountManager.saveAccountToPreferences(account: KubotaAccount) {
    try {
        val alias = AccountManager.CrpAlias.ACCOUNT_ALIAS.alias
        val data = account.prefsValue()
        val accountCryptoData = KeyStoreService.encrypt(alias, data)

        AppProxy.proxy.preferences.setCryptoDataFor(
            key = PREFERENCE_KEY_ACCOUNT
            , encryptedData = HexEncoder.toHex(accountCryptoData.encryption)
            , encryptedIv = HexEncoder.toHex(accountCryptoData.iv)
        )
    } catch (ignored: Exception) {
        Log.e("AccountManager", ignored.toString())
    }
}
