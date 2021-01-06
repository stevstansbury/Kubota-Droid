package com.android.kubota.coordinator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.coordinator.flow.*
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.SignInState.FromBegin
import com.android.kubota.coordinator.state.SignInState.FromSignIn
import com.android.kubota.coordinator.state.SignInState.FromForgotPassword
import com.android.kubota.coordinator.state.SignInState.FromAuthenticate
import com.android.kubota.coordinator.state.SignInState.FromResetPasswordWithVerificationCode
import com.android.kubota.ui.flow.account.ForgotPasswordFlowFragment
import com.android.kubota.ui.flow.account.SignInFlowFragment
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.auth.ResetPasswordToken


class SignInFlowCoordinator: StateMachineFlowCoordinator<SignInState, Unit, Boolean>(), SignInStateMachine {

    companion object {
        fun intent(context: Context): Intent {
            val intent = Intent(context, SignInFlowCoordinator::class.java)
            intent.putExtra(
                FLOWKIT_BUNDLE_CONTEXT,
                Bundle().put(FLOWKIT_BUNDLE_STATE, SignInState.Begin(context = Unit))
            )
            return intent
        }
    }

    override fun onBegin(state: SignInState, context: Unit): Promise<FromBegin> {
        return Promise.value(FromBegin.SignIn(context = null))
    }

    override fun onSignIn(
        state: SignInState,
        context: Throwable?
    ): Promise<FromSignIn> {
        return this.subflow2(SignInFlowFragment::class.java, context = context)
                    .map {
                       when (it) {
                           is SignInFlowFragment.Result.SignIn ->
                               FromSignIn.Authenticate(context =
                                    SignInState.Credentials(username = it.username, password = it.password)
                               )
                           is SignInFlowFragment.Result.ForgotPassword ->
                               FromSignIn.ForgotPassword(context = it.email)
                       }
                    }
    }

    override fun onAuthenticate(
        state: SignInState,
        context: SignInState.Credentials
    ): Promise<FromAuthenticate> {
        this.showBlockingActivityIndicator()
        return AppProxy.proxy.accountManager.authenticate(username = context.username, password = context.password)
                .map {
                    FromAuthenticate.End(context = true) as FromAuthenticate
                }
                .recover {error ->
                     Promise.value(FromAuthenticate.SignIn(context = error))
                }
                .ensure {
                    this.hideBlockingActivityIndicator()
                }
    }

    override fun onForgotPassword(
        state: SignInState,
        context: String?
    ): Promise<FromForgotPassword> {
        return this.subflow2(ForgotPasswordFlowFragment::class.java, context=context)
                    .map { result ->
                        when (result) {
                            is ForgotPasswordFlowFragment.Result.SendVerificationCode -> {
                                val args = SignInState.ResetPasswordContext(email=result.email)
                                FromForgotPassword.ResetPasswordWithVerificationCode(context = args) as FromForgotPassword
                            }
                        }
                    }
                    .back { FromForgotPassword.SignIn(context=null)  }
                    .recover {
                        Promise.value(FromForgotPassword.SignIn(context = null))
                    }
    }

    override fun onResetPasswordWithVerificationCode(state: SignInState, context: SignInState.ResetPasswordContext): Promise<FromResetPasswordWithVerificationCode> {
        val inputState = NewPasswordState.Begin(context = NewPasswordType.ResetPassword(email=context.email))
        return this.subflow(stateMachine = NewPasswordFlowCoordinator::class.java, state = inputState)
                    .map {
                        if (it) {
                            FromResetPasswordWithVerificationCode.SignIn(context = null)
                        } else {
                            FromResetPasswordWithVerificationCode.ResetPasswordWithVerificationCode(context=context)
                        }
                    }
                    .back { FromResetPasswordWithVerificationCode.ForgotPassword(context.email) }
                    .recover {
                        when (it) {
                            is KubotaServiceError.NotConnectedToInternet,
                            is KubotaServiceError.NetworkConnectionLost -> this.showToast(R.string.connectivity_error_message)
                            is KubotaServiceError.ServerMaintenance -> this.showToast(R.string.server_maintenance)
                            else -> this.showToast(R.string.server_error_message)
                        }
                        Promise.value(FromResetPasswordWithVerificationCode.ForgotPassword(context=context.email))
                    }
    }
}
