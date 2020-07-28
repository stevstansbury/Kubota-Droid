package com.android.kubota.coordinator

import android.os.Bundle
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.flow.AuthStateMachineFlowCoordinator
import com.android.kubota.coordinator.flow.hideActivityIndicator
import com.android.kubota.coordinator.flow.showActivityIndicator
import com.android.kubota.coordinator.state.OnboardUserState
import com.android.kubota.coordinator.state.OnboardUserType
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier

abstract class AddEquipmentFlowCoordinator<S: FlowState,I,O>: AuthStateMachineFlowCoordinator<S, I, O>() {

    sealed class AddEquipmentUnitType {
        class Pin(val pin: String, val modelName: String, val nickName: String?): AddEquipmentUnitType()
        class Serial(val serial: String, val modelName: String, val nickName: String?): AddEquipmentUnitType()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setTitle(R.string.add_equipment)
    }

    protected fun isAuthenticated(): Promise<Boolean> {
        if (AppProxy.proxy.accountManager.isAuthenticated.value == true) return Promise.value(true)
        return this.subflow(stateMachine =  OnboardUserFlowCoordinator::class.java,
                            state = OnboardUserState.Begin(context = OnboardUserType.ADD_EQUIPMENT)
                    )
                   .recover { Promise.value(false) }
    }

    protected fun addEquipmentUnitRequest(type: AddEquipmentUnitType, isFromScan: Boolean): Promise<EquipmentUnit> {
        val request = when (type) {
            is AddEquipmentUnitType.Pin ->
                AddEquipmentUnitRequest(
                    identifierType = EquipmentUnitIdentifier.Pin,
                    pinOrSerial = type.pin,
                    model = type.modelName,
                    nickName = type.nickName,
                    engineHours = 0.0
                )
            is AddEquipmentUnitType.Serial ->
                AddEquipmentUnitRequest(
                    identifierType = EquipmentUnitIdentifier.Serial,
                    pinOrSerial = type.serial,
                    model = type.modelName,
                    nickName = type.nickName,
                    engineHours = 0.0
                )
        }

        return AuthPromise(this)
                    .then {
                        this.showActivityIndicator()
                        AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request, isFromScan)
                    }
                    .map { equipment ->
                        equipment.first { it.model == request.model || it.nickName == request.model || it.nickName == request.nickName }
                    }
                    .ensure { this.hideActivityIndicator() }
    }
}
