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
                    HTTPService.StatusCode.NotFound ->
                        KubotaServiceError.NotFound(statusCode = this.statusCode.code, message = message)
                    HTTPService.StatusCode.Conflict ->
                        KubotaServiceError.Conflict(statusCode = this.statusCode.code, message = message)
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
            val serviceError = (err as? HTTPService.Error)?.serviceError
            if (serviceError != null) {
                resolver.reject(serviceError)
            } else {
                resolver.reject(err)
            }
        }
    }
}
