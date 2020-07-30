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
@Parcelize
data class CreateAccountContext(
    val email: String,
    val password: String,
    val phoneNumber: String
): Parcelable

// State
sealed class CreateAccountState: Parcelable, FlowState {
    @Parcelize
    class Begin(val context: Unit): CreateAccountState(), Parcelable
    @Parcelize
    class Prompt(val context: Throwable?): CreateAccountState(), Parcelable
    @Parcelize
    class CreateAccount(val context: CreateAccountContext): CreateAccountState(), Parcelable
    @Parcelize
    class TermsAndConditions(val context: Unit): CreateAccountState(), Parcelable
    @Parcelize
    class End(val context: Boolean): CreateAccountState(), Parcelable
    @Parcelize
    class Fail(val context: Throwable): CreateAccountState(), Parcelable
    @Parcelize
    class Terminate(val context: Result<Boolean>): CreateAccountState(), Parcelable

    sealed class FromBegin {
        class Prompt(val context: Throwable?): FromBegin()
    }

    sealed class FromPrompt {
        class CreateAccount(val context: CreateAccountContext): FromPrompt()
        class TermsAndConditions(val context: Unit): FromPrompt()
    }

    sealed class FromCreateAccount {
        class Prompt(val context: Throwable?): FromCreateAccount()
        class End(val context: Boolean): FromCreateAccount()
    }

    sealed class FromTermsAndConditions {
        class Prompt(val context: Throwable?): FromTermsAndConditions()
    }

    sealed class FromEnd {
        class Terminate(val context: Boolean): FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable): FromFail()
    }
}

// StateMachine
interface CreateAccountStateMachine: StateMachine<CreateAccountState, Unit, Boolean> {
    fun onBegin(state: CreateAccountState, context: Unit): Promise<CreateAccountState.FromBegin>
    fun onPrompt(state: CreateAccountState, context: Throwable?): Promise<CreateAccountState.FromPrompt>
    fun onCreateAccount(state: CreateAccountState, context: CreateAccountContext): Promise<CreateAccountState.FromCreateAccount>
    fun onTermsAndConditions(state: CreateAccountState, context: Unit): Promise<CreateAccountState.FromTermsAndConditions>

    fun onEnd(state: CreateAccountState, context: Boolean) : Promise<CreateAccountState.FromEnd> =
        Promise.value(CreateAccountState.FromEnd.Terminate(context))
    fun onFail(state: CreateAccountState, context: Throwable) : Promise<CreateAccountState.FromFail> =
        Promise.value(CreateAccountState.FromFail.Terminate(context))

    override fun dispatch(prev: CreateAccountState, state: CreateAccountState): Promise<CreateAccountState> =
        when (state) {
            is CreateAccountState.Begin ->
                onBegin(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.Prompt ->
                onPrompt(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.CreateAccount ->
                onCreateAccount(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.TermsAndConditions ->
                onTermsAndConditions(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.End ->
                onEnd(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.Fail ->
                onFail(state=prev, context=state.context)
                    .map { toCreateAccountState(substate=it) }
            is CreateAccountState.Terminate ->
                onTerminate(state=prev, context=state.context)
                    .map { CreateAccountState.Terminate(context= Result.Success(it)) as CreateAccountState }
                    .recover { Promise.value(CreateAccountState.Terminate(Result.Failure(it)) as CreateAccountState) }
        }

    override fun getResult(state: CreateAccountState): Result<Boolean>? =
        when (state) {
            is CreateAccountState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable):  CreateAccountState = CreateAccountState.Fail(context=error)
    override fun createState(context: Unit): CreateAccountState = CreateAccountState.Begin(context=context)

    private fun toCreateAccountState(substate: CreateAccountState.FromBegin): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromBegin.Prompt -> CreateAccountState.Prompt(context=substate.context)
        }

    private fun toCreateAccountState(substate: CreateAccountState.FromPrompt): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromPrompt.CreateAccount -> CreateAccountState.CreateAccount(context=substate.context)
            is CreateAccountState.FromPrompt.TermsAndConditions -> CreateAccountState.TermsAndConditions(context=substate.context)
        }

    private fun toCreateAccountState(substate: CreateAccountState.FromCreateAccount): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromCreateAccount.Prompt -> CreateAccountState.Prompt(context=substate.context)
            is CreateAccountState.FromCreateAccount.End -> CreateAccountState.End(context=substate.context)
        }

    private fun toCreateAccountState(substate: CreateAccountState.FromTermsAndConditions): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromTermsAndConditions.Prompt -> CreateAccountState.Prompt(context=substate.context)
        }

    private fun toCreateAccountState(substate: CreateAccountState.FromEnd): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromEnd.Terminate -> CreateAccountState.Terminate(context= Result.Success(substate.context))
        }

    private fun toCreateAccountState(substate: CreateAccountState.FromFail): CreateAccountState =
        when (substate) {
            is CreateAccountState.FromFail.Terminate -> CreateAccountState.Terminate(context= Result.Failure(substate.context))
        }
}
