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
import com.android.kubota.extensions.hasCameraPermissions
import com.android.kubota.ui.flow.equipment.*
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.*

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
                        FromScanBarcode.AddEquipmentUnit(context=result.code)
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

    override fun onAddEquipmentUnit(
        state: AddEquipmentScanState,
        context: Barcode
    ): Promise<FromAddEquipmentUnit> {
        this.showActivityIndicator()
        return this.isAuthenticated()
            .thenMap { authenticated ->
                if (authenticated) {
                    val request = if (context.equipmentPIN != null)
                        AddEquipmentUnitType.Pin(context.equipmentPIN!!, modelName = context.equipmentModel ?: "")
                    else {
                        AddEquipmentUnitType.Serial(context.equipmentSerial!!, modelName = context.equipmentModel ?: "")
                    }
                    this.addEquipmentUnitRequest(request)
                        .map {
                            FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentUnit(unit = it))
                                    as FromAddEquipmentUnit
                        }
                } else {
                    AppProxy.proxy.serviceManager.equipmentService
                        .searchModels(partialModel = context.equipmentModel ?: "", serial = context.equipmentSerial ?: "")
                        .thenMap {
                            val model = it.firstOrNull()
                            if (model != null) {
                                Promise.value(
                                    FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentModel(model = model))
                                        as FromAddEquipmentUnit
                                )
                            } else {
                                MessageDialogFragment
                                    .showSimpleMessage(
                                        this.supportFragmentManager,
                                        titleId = R.string.equipment_search,
                                        messageId = R.string.equipment_not_found
                                    )
                                    .map {
                                        FromAddEquipmentUnit.ScanBarcode(context=Unit)
                                                as FromAddEquipmentUnit
                                    }
                            }
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
            .ensure {
                this.hideActivityIndicator()
            }
    }

}
