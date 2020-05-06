//
//  KubotaAuthService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.patch
import com.inmotionsoftware.foundation.service.post
import com.inmotionsoftware.foundation.service.put
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.asVoid
import com.kubota.service.api.AuthService
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.auth.ResetPasswordToken

internal class KubotaAuthService(config: Config, private val clientId: String, private val clientSecret: String)
    : HTTPService(config = config), AuthService {

    override fun authenticate(username: String, password: String): Promise<OAuthToken> {
        val params = mapOf(
            "username" to username,
            "password" to password,
            "grant_type" to "password",
            "client_id" to this.clientId,
            "client_secret" to this.clientSecret
        )

        return service {
            this.post(
                route = "/oauth/token",
                body = UploadBody.FormUrlEncoded(params),
                type = OAuthToken::class.java
            )
        }
    }

    override fun authenticate(token: OAuthToken): Promise<OAuthToken> {
        val params = mapOf(
            "refresh_token" to token.refreshToken,
            "grant_type" to "refresh_token",
            "client_id" to this.clientId,
            "client_secret" to this.clientSecret
        )
        return service {
            this.post(route = "/oauth/token",
                body = UploadBody.FormUrlEncoded(params),
                type = OAuthToken::class.java
            )
        }
    }

    override fun logout(): Promise<Void> {
        TODO("Not yet implemented")
    }

    override fun createAccount(email: String, password: String): Promise<Unit> {
        val params = mapOf(
                "email" to email,
                "password" to password
            )
        return service {
            this.post(route = "/oauth/user", body = UploadBody.FormUrlEncoded(params)).asVoid()
        }
    }

    override fun requestForgotPasswordVerificationCode(email: String): Promise<ResetPasswordToken> {
        val params = mapOf("email" to email)
        return service {
            this.post(
                route = "/oauth/forgot_password",
                body = UploadBody.FormUrlEncoded(params),
                type = ResetPasswordToken::class.java
            )
        }
    }

    override fun resetPassword(token: ResetPasswordToken,  verificationCode: String, newPassword: String): Promise<Unit> {
        val params = mapOf(
                "token" to token.token,
                "code" to verificationCode,
                "new_password" to newPassword
            )
        return service {
            this.put(route = "/oauth/reset_password", body = UploadBody.FormUrlEncoded(params)).asVoid()
        }
    }

    override fun changePassword(currentPassword: String, newPassword: String): Promise<Unit> {
        val params = mapOf(
            "current_password" to currentPassword,
            "password" to newPassword
        )
        return service { this.patch(route = "/oauth/user", body = UploadBody.FormUrlEncoded(params)) }
    }

}
