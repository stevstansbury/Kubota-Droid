//
//  KubotaServiceError.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

sealed class KubotaServiceError: Throwable {
    constructor(): super()
    constructor(message: String): super(message)
    constructor(cause: Throwable): super(cause)
    constructor(message: String, cause: Throwable): super(message, cause)

    class Generic(val statusCode: Int = 0, message: String = ""): KubotaServiceError(message)
    class BadRequest(val statusCode: Int = 400, message: String = ""): KubotaServiceError(message)
    class Forbidden(val statusCode: Int = 403, message: String = ""): KubotaServiceError(message)
    class NotFound(val statusCode: Int = 404, message: String = ""): KubotaServiceError(message)
    class Conflict(val statusCode: Int = 409, message: String): KubotaServiceError(message)
    class Unauthorized(val statusCode: Int = 401, message: String): KubotaServiceError(message)
    class ServerError(val statusCode: Int = 500, message: String): KubotaServiceError(message)
    class ServerMaintenance(val statusCode: Int = 502, message: String): KubotaServiceError(message)
    class NetworkConnectionLost(message: String): KubotaServiceError(message)
    class NotConnectedToInternet(message: String): KubotaServiceError(message)
}
