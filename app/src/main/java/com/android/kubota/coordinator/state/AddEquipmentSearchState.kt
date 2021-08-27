package com.android.kubota.coordinator.state

import android.os.Parcelable
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.domain.EquipmentModel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EquipmentSearchInput(
    val equipmentType: EquipmentModel.Type,
    val result: List<EquipmentModel>,
    val error: Throwable?
) : Parcelable

@Parcelize
data class SearchParamsContext(
    val serial: String,
    val modelName: String,
    val equipmentType: EquipmentModel.Type
) : Parcelable

@Parcelize
data class AddEquipmentModelContext(
    val serial: String,
    val model: EquipmentModel,
    val equipmentType: EquipmentModel.Type
) : Parcelable

// State
sealed class AddEquipmentSearchState : Parcelable, FlowState {
    @Parcelize
    class Begin(val context: EquipmentModel.Type) : AddEquipmentSearchState(), Parcelable

    @Parcelize
    class SearchView(val context: EquipmentSearchInput) : AddEquipmentSearchState(), Parcelable

    @Parcelize
    class Search(val context: SearchParamsContext) : AddEquipmentSearchState(), Parcelable

    @Parcelize
    class AddEquipmentUnit(val context: AddEquipmentModelContext) : AddEquipmentSearchState(),
        Parcelable

    @Parcelize
    class End(val context: AddEquipmentResult) : AddEquipmentSearchState(), Parcelable

    @Parcelize
    class Fail(val context: Throwable) : AddEquipmentSearchState(), Parcelable

    @Parcelize
    class Terminate(val context: Result<AddEquipmentResult>) : AddEquipmentSearchState(), Parcelable

    sealed class FromBegin {
        class SearchView(val context: EquipmentSearchInput) : FromBegin()
    }

    sealed class FromSearchView {
        class Search(val context: SearchParamsContext) : FromSearchView()
        class AddEquipmentUnit(val context: AddEquipmentModelContext) : FromSearchView()
    }

    sealed class FromSearch {
        class SearchView(val context: EquipmentSearchInput) : FromSearch()
    }

    sealed class FromAddEquipmentUnit {
        class SearchView(val context: EquipmentSearchInput) : FromAddEquipmentUnit()
        class End(val context: AddEquipmentResult) : FromAddEquipmentUnit()
    }

    sealed class FromEnd {
        class Terminate(val context: AddEquipmentResult) : FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable) : FromFail()
    }
}

// StateMachine
interface AddEquipmentSearchStateMachine :
    StateMachine<AddEquipmentSearchState, EquipmentModel.Type, AddEquipmentResult> {
    fun onBegin(
        state: AddEquipmentSearchState,
        context: EquipmentModel.Type
    ): Promise<AddEquipmentSearchState.FromBegin>

    fun onSearchView(
        state: AddEquipmentSearchState,
        context: EquipmentSearchInput
    ): Promise<AddEquipmentSearchState.FromSearchView>

    fun onSearch(
        state: AddEquipmentSearchState,
        context: SearchParamsContext
    ): Promise<AddEquipmentSearchState.FromSearch>

    fun onAddEquipmentUnit(
        state: AddEquipmentSearchState,
        context: AddEquipmentModelContext
    ): Promise<AddEquipmentSearchState.FromAddEquipmentUnit>

    fun onEnd(
        state: AddEquipmentSearchState,
        context: AddEquipmentResult
    ): Promise<AddEquipmentSearchState.FromEnd> =
        Promise.value(AddEquipmentSearchState.FromEnd.Terminate(context))

    fun onFail(
        state: AddEquipmentSearchState,
        context: Throwable
    ): Promise<AddEquipmentSearchState.FromFail> =
        Promise.value(AddEquipmentSearchState.FromFail.Terminate(context))

    override fun dispatch(
        prev: AddEquipmentSearchState,
        state: AddEquipmentSearchState
    ): Promise<AddEquipmentSearchState> =
        when (state) {
            is AddEquipmentSearchState.Begin ->
                onBegin(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.SearchView ->
                onSearchView(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.Search ->
                onSearch(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.AddEquipmentUnit ->
                onAddEquipmentUnit(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.End ->
                onEnd(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.Fail ->
                onFail(state = prev, context = state.context)
                    .map { toSearchEquipmentState(substate = it) }
            is AddEquipmentSearchState.Terminate ->
                onTerminate(state = prev, context = state.context)
                    .map { AddEquipmentSearchState.Terminate(context = Result.Success(it)) as AddEquipmentSearchState }
                    .recover { Promise.value(AddEquipmentSearchState.Terminate(Result.Failure(it)) as AddEquipmentSearchState) }
        }

    override fun getResult(state: AddEquipmentSearchState): Result<AddEquipmentResult>? =
        when (state) {
            is AddEquipmentSearchState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable): AddEquipmentSearchState =
        AddEquipmentSearchState.Fail(context = error)

    override fun createState(context: EquipmentModel.Type): AddEquipmentSearchState =
        AddEquipmentSearchState.Begin(context = context)

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromBegin): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromBegin.SearchView -> AddEquipmentSearchState.SearchView(
                context = substate.context
            )
        }

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromSearchView): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromSearchView.Search -> AddEquipmentSearchState.Search(
                context = substate.context
            )
            is AddEquipmentSearchState.FromSearchView.AddEquipmentUnit -> AddEquipmentSearchState.AddEquipmentUnit(
                context = substate.context
            )
        }

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromSearch): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromSearch.SearchView -> AddEquipmentSearchState.SearchView(
                context = substate.context
            )
        }

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromAddEquipmentUnit): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromAddEquipmentUnit.SearchView -> AddEquipmentSearchState.SearchView(
                context = substate.context
            )
            is AddEquipmentSearchState.FromAddEquipmentUnit.End -> AddEquipmentSearchState.End(
                context = substate.context
            )
        }

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromEnd): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromEnd.Terminate -> AddEquipmentSearchState.Terminate(
                context = Result.Success(substate.context)
            )
        }

    private fun toSearchEquipmentState(substate: AddEquipmentSearchState.FromFail): AddEquipmentSearchState =
        when (substate) {
            is AddEquipmentSearchState.FromFail.Terminate -> AddEquipmentSearchState.Terminate(
                context = Result.Failure(substate.context)
            )
        }
}

