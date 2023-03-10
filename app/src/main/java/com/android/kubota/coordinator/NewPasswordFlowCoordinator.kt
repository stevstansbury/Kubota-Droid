package com.android.kubota.coordinator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.coordinator.flow.*
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.NewPasswordState.FromBegin
import com.android.kubota.coordinator.state.NewPasswordState.FromChangePassword
import com.android.kubota.coordinator.state.NewPasswordState.FromResetPassword
import com.android.kubota.coordinator.state.NewPasswordState.FromRequestCode
import com.android.kubota.ui.flow.account.NewPasswordFlowFragment
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.auth.ResetPasswordToken

class NewPasswordFlowCoordinator
    : AuthStateMachineFlowCoordinator<NewPasswordState, NewPasswordType, Boolean>()
    , NewPasswordStateMachine {

    companion object {
        fun changePasswordIntent(context: Context): Intent {
            val intent = Intent(context, NewPasswordFlowCoordinator::class.java)
            intent.putExtra(
                FLOWKIT_BUNDLE_CONTEXT,
                Bundle().put(FLOWKIT_BUNDLE_STATE, NewPasswordState.Begin(context = NewPasswordType.ChangePassword))
            )
            return intent
        }
    }

    override fun onBegin(state: NewPasswordState, context: NewPasswordType): Promise<FromBegin> {
        return Promise.value(when(context) {
            is NewPasswordType.ChangePassword -> FromBegin.ChangePassword(context=null)
            is NewPasswordType.ResetPassword -> FromBegin.RequestCode(context=context.email)
        })
    }

    override fun onChangePassword(state: NewPasswordState, context: Throwable?): Promise<FromChangePassword> {
        val input = NewPasswordFlowFragment.Input(type = NewPasswordFlowFragment.Type.CHANGE_PASSWORD, error = context)
        return this.subflow2(NewPasswordFlowFragment::class.java, context=input)
                    .thenMap { result ->
                        when (result) {
                            is NewPasswordFlowFragment.Result.ChangePassword -> {
                                this.changePassword(currentPassword = result.currentPassword, newPassword = result.newPassword)
                                    .map {
                                        FromChangePassword.End(context = true) as FromChangePassword
                                    }
                                    .recover { error ->
                                        val err = if (this.showError(error)) null else error
                                        Promise.value(FromChangePassword.ChangePassword(context = err))
                                    }
                            }
                            else ->
                                Promise.value(
                                    FromChangePassword.ChangePassword(context=context) as FromChangePassword
                                )
                        }
                    }
    }

    override fun onResetPassword( state: NewPasswordState, context: ResetPasswordContext ): Promise<FromResetPassword> {
        val input = NewPasswordFlowFragment.Input(type = NewPasswordFlowFragment.Type.RESET_PASSWORD, error = context.error)
        return this.subflow2(NewPasswordFlowFragment::class.java, context=input)
                    .thenMap {
                        when (it) {
                            is NewPasswordFlowFragment.Result.ResetPassword -> {
                                this.resetPassword(token=context.token, verificationCode = it.verificationCode, newPassword = it.newPassword)
                                    .map {
                                        FromResetPassword.End(context = true) as FromResetPassword
                                    }
                                    .recover { error ->
                                        val err = if (this.showError(error)) null else error
                                        val c = ResetPasswordContext(token = context.token, email = context.email, error = err)
                                        Promise.value(FromResetPassword.ResetPassword(context = c))
                                    }
                            }
                            is NewPasswordFlowFragment.Result.ResendCode -> {
                                Promise.value(FromResetPassword.RequestCode(context.email) as FromResetPassword)
                            }
                            else -> {
                                Promise.value(FromResetPassword.ResetPassword(context=context) as FromResetPassword)
                            }
                        }
                    }
    }

    override fun onRequestCode(state: NewPasswordState, context: String): Promise<FromRequestCode> {
        this.showBlockingActivityIndicator()
        return AppProxy.proxy.accountManager.sendForgotPasswordVerificationCode(context)
            .map {
                ResetPasswordContext(token=it, email=context, error=null)
            }
            .recover {
                when (it) {
                    is AccountError.InvalidEmail -> {
                        // Don't show InvalidEmail error for security
                        val token = ResetPasswordToken("")
                        Promise.value(ResetPasswordContext(token=token, email=context, error=null))
                    }
                    else -> throw it
                }
            }
            .thenMap { ctx ->
                this.showMessageDialog(R.string.verify_code, R.string.send_new_code_message).map {
                    FromRequestCode.ResetPassword(context=ctx) as FromRequestCode
                }
            }
            .ensure {
                this.hideBlockingActivityIndicator()
            }
    }

    private fun changePassword(currentPassword: String, newPassword: String): Promise<Unit> {
        this.showBlockingActivityIndicator()
        return AuthPromise(this)
                .then {
                    AppProxy.proxy.accountManager.changePassword(currentPassword, newPassword)
                }
                .thenMap {
                    this.showMessageDialog(title=R.string.change_password_preference_title, message=R.string.change_password_success)
                }
                .ensure {
                    this.hideBlockingActivityIndicator()
                }
    }

    private fun resetPassword(token: ResetPasswordToken, verificationCode: String, newPassword: String): Promise<Unit> {
        this.showBlockingActivityIndicator()
        return AppProxy.proxy.accountManager
                        .resetPassword(token=token, verificationCode=verificationCode, newPassword=newPassword)
                        .ensure { this.hideBlockingActivityIndicator() }
    }

    private fun showError(error: Throwable): Boolean {
        when (error) {
            is AccountError.InvalidPassword,
            is AccountError.InvalidPasswordResetCode ->
                return false
            is AccountError.InvalidPasswordResetToken ->
                this.showToast(R.string.forgot_password_reset_token_expired)
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                this.showToast(R.string.connectivity_error_message)
            is KubotaServiceError.ServerMaintenance ->
                this.showToast(R.string.server_maintenance)
            else ->
                this.showToast(R.string.server_error_message)
        }
        return true
    }

}
