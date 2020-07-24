package com.android.kubota.coordinator.state

import android.os.Parcelable
import com.android.kubota.ui.flow.equipment.Barcode
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.flowkit.Result
import com.inmotionsoftware.flowkit.StateMachine
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.domain.EquipmentModel
import kotlinx.android.parcel.Parcelize


// State
sealed class AddEquipmentScanState: Parcelable, FlowState {
    @Parcelize
    class Begin(val context: Unit): AddEquipmentScanState(), Parcelable
    @Parcelize
    class CameraPermission(val context: Unit): AddEquipmentScanState(), Parcelable
    @Parcelize
    class Instructions(val context: Unit): AddEquipmentScanState(), Parcelable
    @Parcelize
    class ScanBarcode(val context: Unit): AddEquipmentScanState(), Parcelable
    @Parcelize
    class SearchEquipments(val context: Barcode): AddEquipmentScanState(), Parcelable
    @Parcelize
    class ShowSearchResult(val context: ScanSearchResult): AddEquipmentScanState(), Parcelable
    @Parcelize
    class ManualSearch(val context: Unit): AddEquipmentScanState(), Parcelable
    @Parcelize
    class AddEquipmentUnit(val context: AddEquipmentUnitContext): AddEquipmentScanState(), Parcelable
    @Parcelize
    class End(val context: AddEquipmentResult): AddEquipmentScanState(), Parcelable
    @Parcelize
    class Fail(val context: Throwable): AddEquipmentScanState(), Parcelable
    @Parcelize
    class Terminate(val context: Result<AddEquipmentResult>): AddEquipmentScanState(), Parcelable

    sealed class FromBegin {
        class CameraPermission(val context: Unit): FromBegin()
    }

    sealed class FromCameraPermission {
        class Instructions(val context: Unit): FromCameraPermission()
        class ScanBarcode(val context: Unit): FromCameraPermission()
        class ManualSearch(val context: Unit): FromCameraPermission()
    }

    sealed class FromInstructions {
        class ScanBarcode(val context: Unit): FromInstructions()
    }

    sealed class FromScanBarcode {
        class Instructions(val context: Unit): FromScanBarcode()
        class ManualSearch(val context: Unit): FromScanBarcode()
        class CameraPermission(val context: Unit): FromScanBarcode()
        class SearchEquipments(val context: Barcode): FromScanBarcode()
        class ScanBarcode(val context: Unit): FromScanBarcode()
    }

    sealed class FromSearchEquipments {
        class ScanBarcode(val context: Unit): FromSearchEquipments()
        class ShowSearchResult(val context: ScanSearchResult): FromSearchEquipments()
        class AddEquipmentUnit(val context: AddEquipmentUnitContext): FromSearchEquipments()
    }

    sealed class FromShowSearchResult {
        class ScanBarcode(val context: Unit): FromShowSearchResult()
        class AddEquipmentUnit(val context: AddEquipmentUnitContext): FromShowSearchResult()
    }

    sealed class FromManualSearch {
        class ScanBarcode(val context: Unit): FromManualSearch()
        class End(val context: AddEquipmentResult): FromManualSearch()
    }

    sealed class FromAddEquipmentUnit {
        class ScanBarcode(val context: Unit): FromAddEquipmentUnit()
        class End(val context: AddEquipmentResult): FromAddEquipmentUnit()
    }

    sealed class FromEnd {
        class Terminate(val context: AddEquipmentResult): FromEnd()
    }

    sealed class FromFail {
        class Terminate(val context: Throwable): FromFail()
    }

}

// StateMachine
interface AddEquipmentScanStateMachine: StateMachine<AddEquipmentScanState, Unit, AddEquipmentResult> {
    fun onBegin(state: AddEquipmentScanState, context: Unit): Promise<AddEquipmentScanState.FromBegin>
    fun onCameraPermission(state: AddEquipmentScanState, context: Unit): Promise<AddEquipmentScanState.FromCameraPermission>
    fun onInstructions(state: AddEquipmentScanState, context: Unit): Promise<AddEquipmentScanState.FromInstructions>
    fun onScanBarcode(state: AddEquipmentScanState, context: Unit): Promise<AddEquipmentScanState.FromScanBarcode>
    fun onManualSearch(state: AddEquipmentScanState, context: Unit): Promise<AddEquipmentScanState.FromManualSearch>
    fun onSearchEquipments(state: AddEquipmentScanState, context: Barcode): Promise<AddEquipmentScanState.FromSearchEquipments>
    fun onShowSearchResult(state: AddEquipmentScanState, context: ScanSearchResult): Promise<AddEquipmentScanState.FromShowSearchResult>
    fun onAddEquipmentUnit(state: AddEquipmentScanState, context: AddEquipmentUnitContext): Promise<AddEquipmentScanState.FromAddEquipmentUnit>

    fun onEnd(state: AddEquipmentScanState, context: AddEquipmentResult) : Promise<AddEquipmentScanState.FromEnd> =
        Promise.value(AddEquipmentScanState.FromEnd.Terminate(context))
    fun onFail(state: AddEquipmentScanState, context: Throwable) : Promise<AddEquipmentScanState.FromFail> =
        Promise.value(AddEquipmentScanState.FromFail.Terminate(context))

    override fun dispatch(state: AddEquipmentScanState): Promise<AddEquipmentScanState> =
        when (state) {
            is AddEquipmentScanState.Begin ->
                onBegin(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.CameraPermission ->
                onCameraPermission(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.Instructions ->
                onInstructions(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.ScanBarcode ->
                onScanBarcode(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.ManualSearch ->
                onManualSearch(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.SearchEquipments ->
                onSearchEquipments(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.ShowSearchResult ->
                onShowSearchResult(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.AddEquipmentUnit ->
                onAddEquipmentUnit(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.End ->
                onEnd(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.Fail ->
                onFail(state=state, context=state.context)
                    .map { toAddEquipmentState(substate=it) }
            is AddEquipmentScanState.Terminate ->
                onTerminate(state=state, context=state.context)
                    .map { AddEquipmentScanState.Terminate(context= Result.Success(it)) as AddEquipmentScanState }
                    .recover { Promise.value(AddEquipmentScanState.Terminate(Result.Failure(it)) as AddEquipmentScanState) }
        }

    override fun getResult(state: AddEquipmentScanState): Result<AddEquipmentResult>? =
        when (state) {
            is AddEquipmentScanState.Terminate -> state.context
            else -> null
        }

    override fun createState(error: Throwable):  AddEquipmentScanState = AddEquipmentScanState.Fail(context=error)
    override fun createState(context: Unit): AddEquipmentScanState = AddEquipmentScanState.Begin(context=context)

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromBegin): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromBegin.CameraPermission -> AddEquipmentScanState.CameraPermission(context=substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromCameraPermission): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromCameraPermission.Instructions -> AddEquipmentScanState.Instructions(context=substate.context)
            is AddEquipmentScanState.FromCameraPermission.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context=substate.context)
            is AddEquipmentScanState.FromCameraPermission.ManualSearch -> AddEquipmentScanState.ManualSearch(context=substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromInstructions): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromInstructions.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context=substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromScanBarcode): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromScanBarcode.Instructions -> AddEquipmentScanState.Instructions(context=substate.context)
            is AddEquipmentScanState.FromScanBarcode.ManualSearch -> AddEquipmentScanState.ManualSearch(context=substate.context)
            is AddEquipmentScanState.FromScanBarcode.CameraPermission -> AddEquipmentScanState.CameraPermission(context=substate.context)
            is AddEquipmentScanState.FromScanBarcode.SearchEquipments -> AddEquipmentScanState.SearchEquipments(context=substate.context)
            is AddEquipmentScanState.FromScanBarcode.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context=substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromManualSearch): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromManualSearch.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context=substate.context)
            is AddEquipmentScanState.FromManualSearch.End -> AddEquipmentScanState.End(context=substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromSearchEquipments): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromSearchEquipments.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context = substate.context)
            is AddEquipmentScanState.FromSearchEquipments.ShowSearchResult -> AddEquipmentScanState.ShowSearchResult(context = substate.context)
            is AddEquipmentScanState.FromSearchEquipments.AddEquipmentUnit -> AddEquipmentScanState.AddEquipmentUnit(context = substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromShowSearchResult): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromShowSearchResult.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context = substate.context)
            is AddEquipmentScanState.FromShowSearchResult.AddEquipmentUnit -> AddEquipmentScanState.AddEquipmentUnit(context = substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromAddEquipmentUnit): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromAddEquipmentUnit.ScanBarcode -> AddEquipmentScanState.ScanBarcode(context = substate.context)
            is AddEquipmentScanState.FromAddEquipmentUnit.End -> AddEquipmentScanState.End(context = substate.context)
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromEnd): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromEnd.Terminate -> AddEquipmentScanState.Terminate(context= Result.Success(substate.context))
        }

    private fun toAddEquipmentState(substate: AddEquipmentScanState.FromFail): AddEquipmentScanState =
        when (substate) {
            is AddEquipmentScanState.FromFail.Terminate -> AddEquipmentScanState.Terminate(context= Result.Failure(substate.context))
        }
}
