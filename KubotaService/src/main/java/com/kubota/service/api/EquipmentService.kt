//
//  EquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.FaultCode
import java.net.URL

interface EquipmentService {

    fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>>

    fun getManualURL(model: String): Promise<URL>

    fun getModel(model: String): Promise<EquipmentModel?>

    fun getModels(): Promise<List<EquipmentModel>>

}
