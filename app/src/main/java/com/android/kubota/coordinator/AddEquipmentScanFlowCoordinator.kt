package com.android.kubota.coordinator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.flow.*
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromBegin
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromCameraPermission
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromInstructions
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromManualSearch
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromAddEquipmentUnit
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromScanBarcode
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromSearchEquipments
import com.android.kubota.coordinator.state.AddEquipmentScanState.FromShowSearchResult
import com.android.kubota.extensions.hasCameraPermissions
import com.android.kubota.ui.flow.equipment.*
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.SearchModelType
import java.lang.IllegalStateException

class AddEquipmentScanFlowCoordinator
    : AddEquipmentFlowCoordinator<AddEquipmentScanState, Unit, AddEquipmentResult>()
    , AddEquipmentScanStateMachine {

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
        return Promise.value(FromBegin.CameraPermission(context=context))
    }

    override fun onCameraPermission(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromCameraPermission> {
        val permission: (Boolean) -> Promise<FromCameraPermission> = { hasPermission ->
            val value = if (hasPermission) {
                if (AppProxy.proxy.preferences.firstTimeScan) {
                    FromCameraPermission.Instructions(context=Unit)
                } else {
                    FromCameraPermission.ScanBarcode(context=context)
                }
            } else {
                FromCameraPermission.ManualSearch(context=Unit)
            }
            Promise.value(value as FromCameraPermission)
        }

        return when {
            this.hasCameraPermissions() -> {
                permission(true)
            }
            // Use this flag to work around the pause/resume state of FlowKit
            this.hasRequestedCameraPermission -> {
                permission(this.hasCameraPermissions())
            }
            else -> {
                this.hasRequestedCameraPermission = true
                this.requestPermission(permission = Manifest.permission.CAMERA, message = R.string.accept_camera_permission)
                    .thenMap {
                        // Will not get to here instead the onCameraPermission state will get
                        // called again due to pause/resume state of FlowKit.
                        permission(this.hasCameraPermissions())
                    }
            }
        }
    }

    override fun onInstructions(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromInstructions> {
        this.hideToolbar()
        return this.subflow2(ScannerIntroFlowFragment::class.java, context=context, animated= true)
            .map {
                FromInstructions.ScanBarcode(context=context) as FromInstructions
            }
            .recover {
                Promise.value(FromInstructions.ScanBarcode(context=context))
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
        return this.subflow2(ScannerFlowFragment::class.java, context=context, animated = true)
            .map { result ->
                when (result) {
                    is ScannerFlowFragment.Result.Info ->
                        FromScanBarcode.Instructions(context=context)
                    is ScannerFlowFragment.Result.ManualEntry ->
                        FromScanBarcode.ManualSearch(context=context)
                    is ScannerFlowFragment.Result.CameraPermission ->
                        FromScanBarcode.CameraPermission(context=context)
                    is ScannerFlowFragment.Result.ScannedBarcode ->
                        FromScanBarcode.SearchEquipments(context=result.code)
                }
            }
    }

    override fun onManualSearch(
        state: AddEquipmentScanState,
        context: Unit
    ): Promise<FromManualSearch> {
        this.animated = true
        return this.subflow(stateMachine = AddEquipmentSearchFlowCoordinator::class.java, state = AddEquipmentSearchState.Begin(context=Unit))
                    .map {
                        this.animated = false
                        FromManualSearch.End(context = it) as FromManualSearch
                    }
                    .recover {
                        Promise.value(FromManualSearch.ScanBarcode(context=Unit))
                    }
    }

    override fun onSearchEquipments(
        state: AddEquipmentScanState,
        context: Barcode
    ): Promise<FromSearchEquipments> {
        this.showActivityIndicator()

        val modelName = context.equipmentModel
        val serial =  context.equipmentSerial
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
                                    FromSearchEquipments.ScanBarcode(context=Unit) as FromSearchEquipments
                                }
                        }
                        it.size == 1 -> {
                            Promise.value(
                                FromSearchEquipments.AddEquipmentUnit(context = AddEquipmentUnitContext(context, it.first()))
                                        as FromSearchEquipments
                            )
                        }
                        else -> {
                            Promise.value(
                                FromSearchEquipments.ShowSearchResult(context = ScanSearchResult(context, it))
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
                            FromSearchEquipments.ScanBarcode(context=Unit)
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
        return this.subflow2(fragment = ScannerSearchResultFlowFragment::class.java, context = context.models)
                    .map {
                        FromShowSearchResult.AddEquipmentUnit(context = AddEquipmentUnitContext(context.barcode, it))
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
                        AddEquipmentUnitType.Pin(pin, modelName = modelName, nickName = null)
                    else {
                        AddEquipmentUnitType.Serial(serial!!, modelName = modelName, nickName = null)
                    }

                    this.addEquipmentUnitRequest(request, isFromScan = true)
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
                                        FromAddEquipmentUnit.ScanBarcode(context=Unit) as FromAddEquipmentUnit
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
                        FromAddEquipmentUnit.ScanBarcode(context=Unit) as FromAddEquipmentUnit
                    }
            }
    }

}
