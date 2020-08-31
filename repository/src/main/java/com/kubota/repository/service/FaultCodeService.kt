package com.kubota.repository.service

import androidx.annotation.WorkerThread
import com.kubota.network.model.FaultCodeApiResponse
import com.kubota.network.model.FaultCodeModel
import com.kubota.network.service.EquipmentAPIFactory
import com.kubota.network.service.NetworkResponse

class FaultCodeService(model: String) {
    private val api = EquipmentAPIFactory.getFaultCodeAPI(model)

    @WorkerThread
    fun checkFaultCodeForModel(codes: List<Int>): FaultCodeResponse{
        return when (val results = api.getFaultCodeForModel(codes)) {
            is NetworkResponse.Success -> {
                FaultCodeResponse.Success(results.value.toFaultCodes().codes)
            }
            is NetworkResponse.IOException -> FaultCodeResponse.IOError()
            is NetworkResponse.ServerError -> {
                FaultCodeResponse.GenericError()
            }
        }
    }
}

data class FaultCodes(val codes: ArrayList<FaultCodeItem>)

data class FaultCodeItem(
    val code: Int,
    val description: String,
    val accelerationLimited: String?,
    val engineOutputLimited: String?,
    val engineStopped: String?,
    val machinePerformance: String?,
    val provisionalMeasure: String?,
    val dealerTitle: String,
    val customerTitle: String,
    val dealerMessage: String,
    val customerMessage: String)

private fun FaultCodeApiResponse.toFaultCodes() : FaultCodes{
    val codes = this.faultCodes.map {
        it.toFaultCodeItem()
    }
    return FaultCodes(codes as ArrayList<FaultCodeItem>)
}

private fun FaultCodeModel.toFaultCodeItem(): FaultCodeItem{
    return FaultCodeItem(code = code, description = description, accelerationLimited = accelerationLimited,
        engineOutputLimited = engineOutputLimited, engineStopped = engineStopped, machinePerformance = machinePerformance,
        provisionalMeasure = provisionalMeasure, dealerTitle = dealerTitle, customerTitle = customerTitle,
        dealerMessage = dealerMessage, customerMessage = customerMessage)
}

sealed class FaultCodeResponse {
    class Success(val codes: ArrayList<FaultCodeItem>): FaultCodeResponse()
    class GenericError: FaultCodeResponse()
    class IOError: FaultCodeResponse()
}