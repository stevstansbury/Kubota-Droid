//
//  KubotaServiceError.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

sealed class KubotaServiceError: Throwable {
    constructor(): super()
    constructor(message: String): super(message = message)
    constructor(cause: Throwable): super(cause = cause)
    constructor(message: String, cause: Throwable): super(message = message, cause = cause)

    class Generic(val statusCode: Int = 0, message: String = ""): KubotaServiceError(message = message)
    class Unauthorized(val statusCode: Int = 401, message: String): KubotaServiceError(message = message)
    class NetworkConnectionLost(message: String): KubotaServiceError(message = message)
    class NotConnectedToInternet(message: String): KubotaServiceError(message = message)
}
