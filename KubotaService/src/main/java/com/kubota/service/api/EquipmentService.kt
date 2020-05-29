//
//  EquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.FaultCode
import com.kubota.service.domain.ManualInfo
import com.kubota.service.domain.EquipmentMaintenance
import java.net.URL

interface EquipmentService {

    fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>>

    fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>>

    fun getManualInfo(model: String): Promise<List<ManualInfo>>

    fun getModel(model: String): Promise<EquipmentModel?>

    fun getModels(): Promise<List<EquipmentModel>>

    fun searchModels(partialModel: String, serial: String): Promise<List<EquipmentModel>>

    fun getModels(category: String): Promise<List<EquipmentModel>>

    fun getCategories(parentCategory: String? = null): Promise<List<EquipmentCategory>>

}
