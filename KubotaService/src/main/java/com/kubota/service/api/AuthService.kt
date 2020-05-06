//
//  AuthService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.auth.ResetPasswordToken

interface AuthService {

    fun authenticate(username: String, password: String): Promise<OAuthToken>

    fun authenticate(token: OAuthToken): Promise<OAuthToken>

    fun logout(): Promise<Void>

    fun createAccount(email: String, password: String): Promise<Unit>

    fun requestForgotPasswordVerificationCode(email: String): Promise<ResetPasswordToken>

    fun resetPassword(token: ResetPasswordToken, verificationCode: String, newPassword: String): Promise<Unit>

    fun changePassword(currentPassword: String, newPassword: String): Promise<Unit>

}
