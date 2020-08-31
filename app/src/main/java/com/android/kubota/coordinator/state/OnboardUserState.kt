package com.android.kubota.coordinator.state

import android.os.Parcelable
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import kotlinx.android.parcel.Parcelize

// Context
enum class OnboardUserType {
    ADD_EQUIPMENT,
    FAVORITE_DEALER
}

// State
sealed class OnboardUserState: Parcelable, FlowState {
    @Parcelize
    class Begin(val context: OnboardUserType): OnboardUserState(), Parcelable
    @Parcelize
    class Intro(val context: OnboardUserType): OnboardUserState(), Parcelable
    @Parcelize
    class SignIn(val context: Unit): OnboardUserState(), Parcelable
    @Parcelize
    class CreateAccount(val context: Unit): OnboardUserState(), Parcelable
    @Parcelize
    class End(val context: Boolean): OnboardUserState(), Parcelable
    @Parcelize
    class Fail(val context: Throwable): OnboardUserState(), Parcelable
    @Parcelize
    class Terminate(val context: Result<Boolean>): OnboardUserState(), Parcelable

    sealed class FromBegin {
        class Intro(val context: OnboardUserType): FromBegin()
    }

    sealed class FromIntro {
        class SignIn(val context: Unit): FromIntro()
        class CreateAccount(val context: Unit): FromIntro()
        class End(val context: Boolean): FromIntro()
    }

    sealed class FromSignIn {
        class Intro(val context: OnboardUserType): FromSignIn()
        class End(val context: Boolean): FromSignIn()
    }

    sealed class FromCreateAccount {
        class Intro(val context: OnboardUserType): FromCreateAccount()
        class End(val context: Boolean): FromCreateAccount()
    }

    sealed class FromEnd {
        class Terminate(val context: Boolean): FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable): FromFail()
    }
}

// StateMachine
interface OnboardUserStateMachine: StateMachine<OnboardUserState, OnboardUserType, Boolean> {
    fun onBegin(state: OnboardUserState, context: OnboardUserType): Promise<OnboardUserState.FromBegin>
    fun onIntro(state: OnboardUserState, context: OnboardUserType): Promise<OnboardUserState.FromIntro>
    fun onSignIn(state: OnboardUserState, context: Unit): Promise<OnboardUserState.FromSignIn>
    fun onCreateAccount(state: OnboardUserState, context: Unit): Promise<OnboardUserState.FromCreateAccount>

    fun onEnd(state: OnboardUserState, context: Boolean) : Promise<OnboardUserState.FromEnd> =
        Promise.value(OnboardUserState.FromEnd.Terminate(context))
    fun onFail(state: OnboardUserState, context: Throwable) : Promise<OnboardUserState.FromFail> =
        Promise.value(OnboardUserState.FromFail.Terminate(context))

    override fun dispatch(prev: OnboardUserState, state: OnboardUserState): Promise<OnboardUserState> =
        when (state) {
            is OnboardUserState.Begin ->
                onBegin(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.Intro ->
                onIntro(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.SignIn ->
                onSignIn(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.CreateAccount ->
                onCreateAccount(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.End ->
                onEnd(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.Fail ->
                onFail(state=prev, context=state.context)
                    .map { toOnboardUserState(substate=it) }
            is OnboardUserState.Terminate ->
                onTerminate(state=prev, context=state.context)
                    .map { OnboardUserState.Terminate(context= Result.Success(it)) as OnboardUserState }
                    .recover { Promise.value(OnboardUserState.Terminate(Result.Failure(it)) as OnboardUserState) }
        }

    override fun getResult(state: OnboardUserState): Result<Boolean>? =
        when (state) {
            is OnboardUserState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable):  OnboardUserState = OnboardUserState.Fail(context=error)
    override fun createState(context: OnboardUserType): OnboardUserState = OnboardUserState.Begin(context=context)

    private fun toOnboardUserState(substate: OnboardUserState.FromBegin): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromBegin.Intro -> OnboardUserState.Intro(context=substate.context)
        }

    private fun toOnboardUserState(substate: OnboardUserState.FromIntro): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromIntro.SignIn -> OnboardUserState.SignIn(context=substate.context)
            is OnboardUserState.FromIntro.CreateAccount -> OnboardUserState.CreateAccount(context=substate.context)
            is OnboardUserState.FromIntro.End -> OnboardUserState.End(context=substate.context)
        }

    private fun toOnboardUserState(substate: OnboardUserState.FromSignIn): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromSignIn.Intro -> OnboardUserState.Intro(context=substate.context)
            is OnboardUserState.FromSignIn.End -> OnboardUserState.End(context=substate.context)
        }

    private fun toOnboardUserState(substate: OnboardUserState.FromCreateAccount): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromCreateAccount.Intro -> OnboardUserState.Intro(context=substate.context)
            is OnboardUserState.FromCreateAccount.End -> OnboardUserState.End(context=substate.context)
        }

    private fun toOnboardUserState(substate: OnboardUserState.FromEnd): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromEnd.Terminate -> OnboardUserState.Terminate(context=Result.Success(substate.context))
        }

    private fun toOnboardUserState(substate: OnboardUserState.FromFail): OnboardUserState =
        when (substate) {
            is OnboardUserState.FromFail.Terminate -> OnboardUserState.Terminate(context=Result.Failure(substate.context))
        }
}
