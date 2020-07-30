package com.android.kubota.coordinator.state

import android.os.Parcelable
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.domain.auth.ResetPasswordToken
import kotlinx.android.parcel.Parcelize

// State
sealed class SignInState: Parcelable, FlowState {
    @Parcelize
    class Begin(val context: Unit): SignInState(), Parcelable
    @Parcelize
    class SignIn(val context: Throwable?): SignInState(), Parcelable
    @Parcelize
    class Authenticate(val context: Credentials): SignInState(), Parcelable
    @Parcelize
    class ForgotPassword(val context: String?): SignInState(), Parcelable
    @Parcelize
    class ResetPasswordWithVerificationCode(val context: ResetPasswordContext): SignInState(), Parcelable
    @Parcelize
    class End(val context: Boolean): SignInState(), Parcelable
    @Parcelize
    class Fail(val context: Throwable): SignInState(), Parcelable
    @Parcelize
    class Terminate(val context: Result<Boolean>): SignInState(), Parcelable

    sealed class FromBegin {
        class SignIn(val context: Throwable?): FromBegin()
    }

    sealed class FromSignIn {
        class Authenticate(val context: Credentials): FromSignIn()
        class ForgotPassword(val context: String?): FromSignIn()
    }

    sealed class FromAuthenticate {
        class SignIn(val context: Throwable?): FromAuthenticate()
        class End(val context: Boolean): FromAuthenticate()
    }

    sealed class FromForgotPassword {
        class ResetPasswordWithVerificationCode(val context: ResetPasswordContext): FromForgotPassword()
        class SignIn(val context: Throwable?): FromForgotPassword()
        class ForgotPassword(val context: String?): FromForgotPassword()
    }

    sealed class FromResetPasswordWithVerificationCode {
        class ResetPasswordWithVerificationCode(val context: ResetPasswordContext): FromResetPasswordWithVerificationCode()
        class ForgotPassword(val context: String?): FromResetPasswordWithVerificationCode()
        class SignIn(val context: Throwable?): FromResetPasswordWithVerificationCode()
    }

    sealed class FromEnd {
        class Terminate(val context: Boolean): FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable): FromFail()
    }

    @Parcelize
    data class Credentials(val username: String, val password: String): Parcelable
    @Parcelize
    data class ResetPasswordContext(val email: String): Parcelable
}

// StateMachine
interface SignInStateMachine: StateMachine<SignInState, Unit, Boolean> {
    fun onBegin(state: SignInState, context: Unit): Promise<SignInState.FromBegin>
    fun onSignIn(state: SignInState, context: Throwable?): Promise<SignInState.FromSignIn>
    fun onAuthenticate(state: SignInState, context: SignInState.Credentials): Promise<SignInState.FromAuthenticate>
    fun onForgotPassword(state: SignInState, context: String?): Promise<SignInState.FromForgotPassword>
    fun onResetPasswordWithVerificationCode(state: SignInState, context: SignInState.ResetPasswordContext):
            Promise<SignInState.FromResetPasswordWithVerificationCode>

    fun onEnd(state: SignInState, context: Boolean) : Promise<SignInState.FromEnd> =
        Promise.value(SignInState.FromEnd.Terminate(context))
    fun onFail(state: SignInState, context: Throwable) : Promise<SignInState.FromFail> =
        Promise.value(SignInState.FromFail.Terminate(context))

    override fun dispatch(prev: SignInState, state: SignInState): Promise<SignInState> =
        when (state) {
            is SignInState.Begin ->
                onBegin(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.SignIn ->
                onSignIn(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.Authenticate ->
                onAuthenticate(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.ForgotPassword ->
                onForgotPassword(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.ResetPasswordWithVerificationCode ->
                onResetPasswordWithVerificationCode(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.End ->
                onEnd(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.Fail ->
                onFail(state=prev, context=state.context)
                    .map { toSignInState(substate=it) }
            is SignInState.Terminate ->
                onTerminate(state=prev, context=state.context)
                    .map { SignInState.Terminate(context= Result.Success(it)) as SignInState }
                    .recover { Promise.value(SignInState.Terminate(Result.Failure(it)) as SignInState) }
        }

    override fun getResult(state: SignInState): Result<Boolean>? =
        when (state) {
            is SignInState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable):  SignInState = SignInState.Fail(context=error)
    override fun createState(context: Unit): SignInState = SignInState.Begin(context=context)

    private fun toSignInState(substate: SignInState.FromBegin): SignInState =
        when (substate) {
            is SignInState.FromBegin.SignIn -> SignInState.SignIn(context=substate.context)
        }

    private fun toSignInState(substate: SignInState.FromSignIn): SignInState =
        when (substate) {
            is SignInState.FromSignIn.Authenticate -> SignInState.Authenticate(context=substate.context)
            is SignInState.FromSignIn.ForgotPassword -> SignInState.ForgotPassword(context=substate.context)
        }

    private fun toSignInState(substate: SignInState.FromAuthenticate): SignInState =
        when (substate) {
            is SignInState.FromAuthenticate.SignIn -> SignInState.SignIn(context=substate.context)
            is SignInState.FromAuthenticate.End -> SignInState.End(context=substate.context)
        }

    private fun toSignInState(substate: SignInState.FromForgotPassword): SignInState =
        when (substate) {
            is SignInState.FromForgotPassword.ResetPasswordWithVerificationCode ->
                SignInState.ResetPasswordWithVerificationCode(context=substate.context)
            is SignInState.FromForgotPassword.SignIn ->
                SignInState.SignIn(context=substate.context)
            is SignInState.FromForgotPassword.ForgotPassword ->
                SignInState.ForgotPassword(context=substate.context)
        }

    private fun toSignInState(substate: SignInState.FromResetPasswordWithVerificationCode): SignInState =
        when (substate) {
            is SignInState.FromResetPasswordWithVerificationCode.ResetPasswordWithVerificationCode ->
                SignInState.ResetPasswordWithVerificationCode(context = substate.context)
            is SignInState.FromResetPasswordWithVerificationCode.ForgotPassword ->
                SignInState.ForgotPassword(context = substate.context)
            is SignInState.FromResetPasswordWithVerificationCode.SignIn ->
                SignInState.SignIn(context = substate.context)
        }

    private fun toSignInState(substate: SignInState.FromEnd): SignInState =
        when (substate) {
            is SignInState.FromEnd.Terminate -> SignInState.Terminate(context= Result.Success(substate.context))
        }

    private fun toSignInState(substate: SignInState.FromFail): SignInState =
        when (substate) {
            is SignInState.FromFail.Terminate -> SignInState.Terminate(context= Result.Failure(substate.context))
        }
}
