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

    override fun onBegin(
        state: NewPasswordState,
        context: NewPasswordType
    ): Promise<FromBegin> {
        return when(context) {
            is NewPasswordType.ChangePassword ->
                Promise.value(FromBegin.ChangePassword(context=null))
            is NewPasswordType.ResetPassword -> {
                val input = ResetPasswordContext(token = context.token, error = null)
                Promise.value(FromBegin.ResetPassword(context = input))
            }
        }
    }

    override fun onChangePassword(
        state: NewPasswordState,
        context: Throwable?
    ): Promise<FromChangePassword> {
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

    override fun onResetPassword(
        state: NewPasswordState,
        context: ResetPasswordContext
    ): Promise<FromResetPassword> {
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
                                        val c = ResetPasswordContext(token = context.token, error = err)
                                        Promise.value(FromResetPassword.ResetPassword(context = c))
                                    }
                            }
                            else ->
                                Promise.value(
                                    FromResetPassword.ResetPassword(context=context) as FromResetPassword
                                )
                        }
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
            else ->
                this.showToast(R.string.server_error_message)
        }
        return true
    }

}
