package com.android.kubota.utility

import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError

class AuthPromise {
    private var signIn: (() -> Promise<Unit>)? = null

    fun onSignIn(handler: () -> Promise<Unit>): AuthPromise {
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
                    is KubotaServiceError.Unauthorized -> {
                        val signIn = this.signIn ?: throw error
                        signIn().thenMap { execute() }
                    }
                    else ->
                        throw error
                }
            }
    }
}