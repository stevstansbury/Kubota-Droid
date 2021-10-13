package com.android.kubota.utility

import android.util.Base64
import androidx.annotation.Keep
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.squareup.moshi.Moshi
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Semaphore

interface AuthDelegate {
    fun authenticateOnSessionExpired(): Promise<Boolean>
}

class AuthPromise(delegate: AuthDelegate? = null) {

    private companion object {
        val semaphore = Semaphore(1)
        val moshi: Moshi = Moshi.Builder().build()
    }

    private val delegate: WeakReference<AuthDelegate>? =
        if (delegate != null) WeakReference(delegate) else null

    fun <T> then(execute: () -> Promise<T>): Promise<T> {
        return reAuthIfAboutToExpire()
            .thenMap { execute() }
            .recover { error ->
                when (error) {
                    is KubotaServiceError.Unauthorized ->
                        AppProxy.proxy.accountManager.reauthenticate().thenMap { execute() }
                    else ->
                        throw error
                }
            }
            .recover { error ->
                when (error) {
                    is KubotaServiceError.Unauthorized -> {
                        this.delegate?.get()?.let {
                            it.authenticateOnSessionExpired()
                                .thenMap { authenticated ->
                                    if (authenticated) execute() else throw error
                                }
                        } ?: throw error
                    }
                    else ->
                        throw error
                }
            }
    }

    @Keep
    private data class TokenExpiration(val exp: Long)

    private fun reAuthIfAboutToExpire(): Promise<Unit> {
        return noConcurrentReAuth {
            val accessToken = AppProxy.proxy.accountManager.authToken?.accessToken
                ?: return@noConcurrentReAuth Promise.value(Unit)

            val dataPart = accessToken
                .substringAfter(".")
                .substringBeforeLast(".")

            val decoded = Base64.decode(dataPart, Base64.DEFAULT).toString(Charsets.UTF_8)

            val expirationUnixSeconds =
                moshi.adapter(TokenExpiration::class.java).fromJson(decoded)!!.exp

            if (expirationUnixSeconds < (System.currentTimeMillis() / 1000) - 30) {
                AppProxy.proxy.accountManager.reauthenticate()
            } else {
                Promise.value(Unit)
            }
        }
    }

    private fun <T> noConcurrentReAuth(execute: () -> Promise<T>): Promise<T> {
        return Promise.value(Unit)
            .thenMap(on = DispatchExecutor.global) {
                semaphore.acquire()
                execute()
            }
            .ensure(on = DispatchExecutor.global) {
                semaphore.release()
            }
    }
}
