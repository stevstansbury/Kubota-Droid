package com.android.kubota.utility

import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import java.lang.ref.WeakReference

interface AuthDelegate {
    fun authenticateOnSessionExpired(): Promise<Boolean>
}

class AuthPromise(delegate: AuthDelegate? = null) {

    private val delegate: WeakReference<AuthDelegate>? =
                            if (delegate != null) WeakReference(delegate) else null

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

}
