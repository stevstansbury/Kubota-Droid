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

// Context
sealed class NewPasswordType: Parcelable {
    @Parcelize
    class ResetPassword(val token: ResetPasswordToken): NewPasswordType(), Parcelable
    @Parcelize
    object ChangePassword: NewPasswordType(), Parcelable
}

@Parcelize
data class ResetPasswordContext(val token: ResetPasswordToken, val error: Throwable?): Parcelable

// State
sealed class NewPasswordState: Parcelable, FlowState {
    @Parcelize
    class Begin(val context: NewPasswordType): NewPasswordState(), Parcelable
    @Parcelize
    class ChangePassword(val context: Throwable?): NewPasswordState(), Parcelable
    @Parcelize
    class ResetPassword(val context: ResetPasswordContext): NewPasswordState(), Parcelable
    @Parcelize
    class End(val context: Boolean): NewPasswordState(), Parcelable
    @Parcelize
    class Fail(val context: Throwable): NewPasswordState(), Parcelable
    @Parcelize
    class Terminate(val context: Result<Boolean>): NewPasswordState(), Parcelable

    sealed class FromBegin {
        class ChangePassword(val context: Throwable?): FromBegin()
        class ResetPassword(val context: ResetPasswordContext): FromBegin()
    }

    sealed class FromChangePassword {
        class ChangePassword(val context: Throwable?): FromChangePassword()
        class End(val context: Boolean): FromChangePassword()
    }

    sealed class FromResetPassword {
        class ResetPassword(val context: ResetPasswordContext): FromResetPassword()
        class End(val context: Boolean): FromResetPassword()
    }

    sealed class FromEnd {
        class Terminate(val context: Boolean): FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable): FromFail()
    }
}

// StateMachine
interface NewPasswordStateMachine: StateMachine<NewPasswordState, NewPasswordType, Boolean> {
    fun onBegin(state: NewPasswordState, context: NewPasswordType): Promise<NewPasswordState.FromBegin>
    fun onChangePassword(state: NewPasswordState, context: Throwable?): Promise<NewPasswordState.FromChangePassword>
    fun onResetPassword(state: NewPasswordState, context: ResetPasswordContext): Promise<NewPasswordState.FromResetPassword>

    fun onEnd(state: NewPasswordState, context: Boolean) : Promise<NewPasswordState.FromEnd> =
        Promise.value(NewPasswordState.FromEnd.Terminate(context))
    fun onFail(state: NewPasswordState, context: Throwable) : Promise<NewPasswordState.FromFail> =
        Promise.value(NewPasswordState.FromFail.Terminate(context))

    override fun dispatch(state: NewPasswordState): Promise<NewPasswordState> =
        when (state) {
            is NewPasswordState.Begin ->
                onBegin(state=state, context=state.context)
                    .map { toResetPasswordState(substate=it) }
            is NewPasswordState.ChangePassword ->
                onChangePassword(state=state, context=state.context)
                    .map { toResetPasswordState(substate=it) }
            is NewPasswordState.ResetPassword ->
                onResetPassword(state=state, context=state.context)
                    .map { toResetPasswordState(substate=it) }
            is NewPasswordState.End ->
                onEnd(state=state, context=state.context)
                    .map { toResetPasswordState(substate=it) }
            is NewPasswordState.Fail ->
                onFail(state=state, context=state.context)
                    .map { toResetPasswordState(substate=it) }
            is NewPasswordState.Terminate ->
                onTerminate(state=state, context=state.context)
                    .map { NewPasswordState.Terminate(context= Result.Success(it)) as NewPasswordState }
                    .recover { Promise.value(NewPasswordState.Terminate(Result.Failure(it)) as NewPasswordState) }
        }

    override fun getResult(state: NewPasswordState): Result<Boolean>? =
        when (state) {
            is NewPasswordState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable):  NewPasswordState = NewPasswordState.Fail(context=error)
    override fun createState(context: NewPasswordType): NewPasswordState = NewPasswordState.Begin(context=context)

    private fun toResetPasswordState(substate: NewPasswordState.FromBegin): NewPasswordState =
        when (substate) {
            is NewPasswordState.FromBegin.ChangePassword -> NewPasswordState.ChangePassword(context=substate.context)
            is NewPasswordState.FromBegin.ResetPassword -> NewPasswordState.ResetPassword(context=substate.context)
        }

    private fun toResetPasswordState(substate: NewPasswordState.FromChangePassword): NewPasswordState =
        when (substate) {
            is NewPasswordState.FromChangePassword.ChangePassword -> NewPasswordState.ChangePassword(context=substate.context)
            is NewPasswordState.FromChangePassword.End -> NewPasswordState.End(context=substate.context)
        }

    private fun toResetPasswordState(substate: NewPasswordState.FromResetPassword): NewPasswordState =
        when (substate) {
            is NewPasswordState.FromResetPassword.ResetPassword -> NewPasswordState.ResetPassword(context=substate.context)
            is NewPasswordState.FromResetPassword.End -> NewPasswordState.End(context=substate.context)
        }

    private fun toResetPasswordState(substate: NewPasswordState.FromEnd): NewPasswordState =
        when (substate) {
            is NewPasswordState.FromEnd.Terminate -> NewPasswordState.Terminate(context= Result.Success(substate.context))
        }

    private fun toResetPasswordState(substate: NewPasswordState.FromFail): NewPasswordState =
        when (substate) {
            is NewPasswordState.FromFail.Terminate -> NewPasswordState.Terminate(context= Result.Failure(substate.context))
        }

}
