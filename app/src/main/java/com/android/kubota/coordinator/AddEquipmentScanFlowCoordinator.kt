package com.android.kubota.coordinator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.flow.*
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.AddEquipmentScanState.*
import com.android.kubota.ui.flow.equipment.*
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.flowkit.back
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.SearchModelType
import com.kubota.service.domain.EquipmentModel

class AddEquipmentScanFlowCoordinator
    : AddEquipmentFlowCoordinator<AddEquipmentScanState, Unit, AddEquipmentResult>(),
    AddEquipmentScanStateMachine {

    companion object {
        fun intent(context: Context): Intent {
            val intent = Intent(context, AddEquipmentScanFlowCoordinator::class.java)
            intent.putExtra(
                FLOWKIT_BUNDLE_CONTEXT,
                Bundle().put(FLOWKIT_BUNDLE_STATE, AddEquipmentScanState.Begin(context = Unit))
            )
            return intent
        }
    }

    private var hasRequestedCameraPermission = false

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        // Don't show the rationale message
        if (permission == Manifest.permission.CAMERA) {
            return false
        }
        return super.shouldShowRequestPermissionRationale(permission)
    }

    override fun onBegin(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromBegin> {
        this.animated = true
        return Promise.value(FromBegin.AddEquipmentType(context = context))
    }

    override fun onAddEquipmentType(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromAddEquipmentType> {
        this.animated = true
        return this.subflow2(
            AddEquipmentTypeFragment::class.java,
            context = context,
            animated = true
        )
            .map { result ->
                when (result) {
                    EquipmentModel.Type.Machine ->
                        FromAddEquipmentType.CameraPermission(context = context)
                    EquipmentModel.Type.Attachment ->
                        FromAddEquipmentType.ManualSearch(context = EquipmentModel.Type.Attachment)
                }
            }
            .back {
                throw FlowError.Canceled()
            }
    }

    override fun onCameraPermission(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromCameraPermission> {
        return this.requestPermission(Manifest.permission.CAMERA, R.string.accept_camera_permission)
            .map {
                // Will not get to here instead the onCameraPermission state will get
                // called again due to pause/resume state of FlowKit.
                this.hasRequestedCameraPermission = true
                if (AppProxy.proxy.preferences.firstTimeScan) {
                    FromCameraPermission.Instructions(context = Unit)
                } else {
                    FromCameraPermission.ScanBarcode(context = context)
                }
            }.recover {
                this.hasRequestedCameraPermission = false
                Promise.value(FromCameraPermission.ManualSearch(context = EquipmentModel.Type.Machine))
            }
    }

    override fun onInstructions(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromInstructions> {
        this.hideToolbar()
        return this.subflow2(
            ScannerIntroFlowFragment::class.java,
            context = context,
            animated = true
        )
            .map {
                FromInstructions.ScanBarcode(context = context) as FromInstructions
            }
            .recover {
                Promise.value(FromInstructions.ScanBarcode(context = context))
            }
            .ensure {
                AppProxy.proxy.preferences.firstTimeScan = false
                this.showToolbar()
            }
    }

    override fun onScanBarcode(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromScanBarcode> {

        return this.subflow2(
            ScannerFlowFragment::class.java,
            context = this.hasRequestedCameraPermission,
            animated = true
        )
            .map { result ->
                when (result) {
                    is ScannerFlowFragment.Result.Info ->
                        FromScanBarcode.Instructions(context = context)
                    is ScannerFlowFragment.Result.ManualEntry ->
                        FromScanBarcode.ManualSearch(context = EquipmentModel.Type.Machine)
                    is ScannerFlowFragment.Result.CameraPermission ->
                        FromScanBarcode.CameraPermission(context = context)
                    is ScannerFlowFragment.Result.ScannedBarcode ->
                        FromScanBarcode.SearchEquipments(context = result.code)
                }
            }
            .back {
                FromScanBarcode.AddEquipmentType(Unit)
            }
    }

    override fun onManualSearch(
        state: AddEquipmentScanState,
        context: EquipmentModel.Type
    ): Promise<FromManualSearch> {
        this.animated = true
        return this.subflow(
            stateMachine = AddEquipmentSearchFlowCoordinator::class.java,
            state = AddEquipmentSearchState.Begin(context = context)
        )
            .map {
                this.animated = false
                FromManualSearch.End(context = it) as FromManualSearch
            }
            .back {
                when (state) {
                    is ScanBarcode -> FromManualSearch.ScanBarcode(Unit) as FromManualSearch
                    else -> FromManualSearch.AddEquipmentType(Unit)
                }
            }
            .recover {
                if (it is FlowError) throw it
                Promise.value(FromManualSearch.AddEquipmentType(context = Unit))
            }

    }

    override fun onSearchEquipments(
        state: AddEquipmentScanState,
        context: Barcode
    ): Promise<FromSearchEquipments> {
        this.showActivityIndicator()

        val modelName = context.equipmentModel
        val serial = context.equipmentSerial
        val pin = context.equipmentPIN

        val searchType: SearchModelType = when {
            // PIN takes priority over serial
            !modelName.isNullOrBlank() && !pin.isNullOrBlank() -> {
                SearchModelType.PartialModelAndPIN(
                    partialModel = modelName,
                    pin = pin
                )
            }
            !modelName.isNullOrBlank() && !serial.isNullOrBlank() -> {
                SearchModelType.PartialModelAndSerial(
                    partialModel = modelName,
                    serial = serial
                )
            }
            else -> {
                SearchModelType.PIN(pin = pin ?: "")
            }
        }

        return AppProxy.proxy.serviceManager.equipmentService.scanSearchModels(searchType)
            .thenMap {
                when {
                    it.isEmpty() -> {
                        MessageDialogFragment
                            .showSimpleMessage(
                                this.supportFragmentManager,
                                titleId = R.string.equipment_search,
                                messageId = R.string.equipment_not_found
                            )
                            .map {
                                FromSearchEquipments.ScanBarcode(context = Unit) as FromSearchEquipments
                            }
                    }
                    it.size == 1 -> {
                        Promise.value(
                            FromSearchEquipments.AddEquipmentUnit(
                                context = AddEquipmentUnitContext(
                                    context,
                                    it.first()
                                )
                            )
                                as FromSearchEquipments
                        )
                    }
                    else -> {
                        Promise.value(
                            FromSearchEquipments.ShowSearchResult(
                                context = ScanSearchResult(
                                    context,
                                    it
                                )
                            )
                                as FromSearchEquipments
                        )
                    }
                }
            }
            .recover {
                MessageDialogFragment
                    .showSimpleMessage(
                        this.supportFragmentManager,
                        titleId = R.string.title_error,
                        messageId = R.string.failed_to_add_equipment
                    )
                    .map {
                        FromSearchEquipments.ScanBarcode(context = Unit)
                    }
            }
            .ensure {
                this.hideActivityIndicator()
            }
    }

    override fun onShowSearchResult(
        state: AddEquipmentScanState,
        context: ScanSearchResult
    ): Promise<FromShowSearchResult> {
        return this.subflow2(
            fragment = ScannerSearchResultFlowFragment::class.java,
            context = context.models
        )
            .map {
                FromShowSearchResult.AddEquipmentUnit(
                    context = AddEquipmentUnitContext(
                        context.barcode,
                        it
                    )
                )
                    as FromShowSearchResult
            }
            .recover {
                Promise.value(FromShowSearchResult.ScanBarcode(context = Unit))
            }
    }

    override fun onAddEquipmentUnit(
        state: AddEquipmentScanState,
        context: AddEquipmentUnitContext
    ): Promise<FromAddEquipmentUnit> {
        return this.isAuthenticated()
            .thenMap { authenticated ->
                val modelName = context.model
                val serial = context.barcode.equipmentSerial
                val pin = context.barcode.equipmentPIN

                if (authenticated) {
                    this.showActivityIndicator()

                    val request = if (!pin.isNullOrBlank())
                        AddEquipmentUnitType.Pin(
                            pin,
                            modelName = modelName,
                            modelType = EquipmentModel.Type.Machine
                        )
                    else {
                        AddEquipmentUnitType.Serial(
                            serial!!,
                            modelName = modelName,
                            modelType = EquipmentModel.Type.Machine
                        )
                    }

                    this.addEquipmentUnitRequest(request)
                        .map {
                            FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentUnit(unit = it))
                                as FromAddEquipmentUnit
                        }
                        .ensure {
                            this.hideActivityIndicator()
                        }
                } else {
                    this.showActivityIndicator()

                    val searchType: SearchModelType = when {
                        // PIN takes priority over serial
                        !pin.isNullOrBlank() -> {
                            SearchModelType.PartialModelAndPIN(
                                partialModel = modelName,
                                pin = pin
                            )
                        }
                        !serial.isNullOrBlank() -> {
                            SearchModelType.PartialModelAndSerial(
                                partialModel = modelName,
                                serial = serial
                            )
                        }
                        else -> {
                            SearchModelType.PIN(pin = pin ?: "")
                        }
                    }

                    AppProxy.proxy.serviceManager.equipmentService.searchModels(type = searchType)
                        .thenMap {
                            if (it.isEmpty()) throw IllegalStateException("Model not found")
                            Promise.value(
                                FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentModel(model = it.first()))
                                    as FromAddEquipmentUnit
                            )
                        }
                        .recover {
                            MessageDialogFragment
                                .showSimpleMessage(
                                    this.supportFragmentManager,
                                    titleId = R.string.equipment_search,
                                    messageId = R.string.equipment_not_found
                                )
                                .map {
                                    FromAddEquipmentUnit.ScanBarcode(context = Unit) as FromAddEquipmentUnit
                                }

                        }
                        .ensure {
                            this.hideActivityIndicator()
                        }
                }
            }
            .recover {
                MessageDialogFragment
                    .showSimpleMessage(
                        this.supportFragmentManager,
                        titleId = R.string.title_error,
                        messageId = R.string.failed_to_add_equipment
                    )
                    .map {
                        FromAddEquipmentUnit.ScanBarcode(context = Unit) as FromAddEquipmentUnit
                    }
            }
    }

}
