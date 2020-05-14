package com.android.kubota.utility

import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError

class AuthPromise {
    private var signIn: (() -> Unit)? = null

    fun onSignIn(handler: () -> Unit): AuthPromise {
        this.signIn = handler
        return this
    }

    fun <T> then(execute: () -> Promise<T>): Promise<T> {
        return execute()
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
                    is KubotaServiceError.Unauthorized -> throw error
                    else -> {
                        val signIn = this.signIn ?: throw error
                        signIn()
                        execute()
                    }
                }
            }
    }
}