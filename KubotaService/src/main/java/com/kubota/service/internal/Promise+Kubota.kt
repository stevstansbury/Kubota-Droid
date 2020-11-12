//
//  Promise+Kubota.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val HTTPService.Error.serviceError: KubotaServiceError
    get() {
        return when (this) {
            is HTTPService.Error.Generic ->
                KubotaServiceError.Generic()
            is HTTPService.Error.UnRecognizedEncoding ->
                KubotaServiceError.Generic(message = this.localizedMessage ?: "")
            is HTTPService.Error.Response -> {
                val message = this.body?.let { String(it, Charsets.UTF_8) } ?: (this.localizedMessage ?: "")
                when (this.statusCode) {
                    HTTPService.StatusCode.Unauthorized ->
                        KubotaServiceError.Unauthorized(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.BadRequest ->
                        KubotaServiceError.BadRequest(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.Forbidden ->
                        KubotaServiceError.Forbidden(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.NotFound ->
                        KubotaServiceError.NotFound(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.Conflict ->
                        KubotaServiceError.Conflict(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.InternalServerError ->
                        KubotaServiceError.ServerError(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.BadGateway ->
                        KubotaServiceError.ServerMaintenance(statusCode = this.statusCode.code, message = message)
                    else ->
                        KubotaServiceError.Generic(statusCode = this.statusCode.code, message = message)
                }
            }
        }
    }

internal fun <T> service(execute: () -> Promise<T>): Promise<T> {
    return execute().catchServiceError()
}

internal fun <T> Promise<T>.catchServiceError(): Promise<T> {
    return Promise<T> { resolver ->
        this.done {
            resolver.fulfill(it)
        }
        .catch { err ->
            val serviceError = when (err) {
                is HTTPService.Error -> err.serviceError
                is UnknownHostException -> KubotaServiceError.NotConnectedToInternet(err.localizedMessage ?: "")
                is SocketTimeoutException -> KubotaServiceError.NetworkConnectionLost(err.localizedMessage ?: "")
                else -> KubotaServiceError.Generic(message = err.localizedMessage ?: "")
            }
            resolver.reject(serviceError)
        }
    }
}
