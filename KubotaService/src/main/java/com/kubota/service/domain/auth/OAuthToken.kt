//
//  OAuthToken.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain.auth

import com.squareup.moshi.Json

data class OAuthToken(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "refresh_token")
    val refreshToken: String,
    @Json(name = "expires_in")
    val expiresIn: Long,
    val scope: String
)
