package com.kubota.service.internal.mock

import com.couchbase.lite.Database
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.EquipmentService
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.FaultCode
import com.kubota.service.domain.ManualInfo
import com.kubota.service.domain.EquipmentMaintenance
import com.kubota.service.internal.KubotaEquipmentService
import java.net.URL

internal class MockKubotaEquipmentService(config: HTTPService.Config, couchbaseDb: Database?): EquipmentService {

    companion object {
        val MOCK_MAINTENANCE_LIST = listOf(
            EquipmentMaintenance(
                checkPoint = "Radiator and oil cooler",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "Coolant Level",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "Fuel Level",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "Engine oil level",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "DEF level",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "Hydraulic oil level",
                measures = "check",
                firstCheckType = "Daily",
                firstCheckValue = 0,
                intervalType = "Every X Hours",
                intervalValue = 10
            ),
            EquipmentMaintenance(
                checkPoint = "Fuel tank - drain water",
                measures = "drain",
                firstCheckType = "X Hours",
                firstCheckValue = 50,
                intervalType = "Every X Hours",
                intervalValue = 50
            ),
            EquipmentMaintenance(
                checkPoint = "Track tension",
                measures = "check",
                firstCheckType = "X Hours",
                firstCheckValue = 50,
                intervalType = "Every X Hours",
                intervalValue = 50
            ),
            EquipmentMaintenance(
                checkPoint = "Air cleaner element",
                measures = "clean",
                firstCheckType = "X Hours",
                firstCheckValue = 50,
                intervalType = "Every X Hours",
                intervalValue = 50
            ),
            EquipmentMaintenance(
                checkPoint = "Radiator hoses and clamps",
                measures = "check",
                firstCheckType = "X hours",
                firstCheckValue = 250,
                intervalType = "Every X Hours",
                intervalValue = 250
            ),
            EquipmentMaintenance(
                checkPoint = "Fuel line and intake air line",
                measures = "check",
                firstCheckType = "X Hours",
                firstCheckValue = 250,
                intervalType = "Every X Hours",
                intervalValue = 250
            )
        )

        private val MOCK_SUB_CATEGORIES = mapOf(
            "Tractors" to listOf(
                "Sub-Compact", "Compact", "Economy Utility", "Utility", "Specialty", "Agriculture", "Tractor Loader Backhoe"
            ),
            "Mowers" to listOf(
                "Zero-Turn Mowers", "Stand-on Mowers", "Walk-Behind Mowers", "Front Mount Mowers", "Lawn & Garden Tractors"
            ),
            "Construction" to listOf (
                "Compact Excavators", "Wheel Loaders", "Track Loaders", "Skid Steer Loaders", "Tractor Loader Backhoe"
            ),
            "Utility Vehicles" to listOf(
                "Mid-Size Utility Vehicles", "Full-Size Gas Utility Vehicles", "Full-Size Diesel Utility Vehicles"
            )
        )

        private val MOCK_REVERSE_SUB_CATEGORIES = mapOf(
            //
            // Tractors
            //

            "Sub-Compact" to "Tractors",
            "Compact" to "Tractors",
            "Economy Utility" to "Tractors",
            "Utility" to "Tractors",
            "Specialty" to "Tractors",
            "Agriculture" to "Tractors",
            "Tractor Loader Backhoe" to "Tractors",

            //
            // Mowers
            //

            "Zero-Turn Mowers" to "Mowers",
            "Stand-on Mowers" to "Mowers",
            "Walk-Behind Mowers" to "Mowers",
            "Front Mount Mowers" to "Mowers",
            "Lawn & Garden Tractors" to "Mowers",

            //
            // Construction
            //

            "Compact Excavators" to "Construction",
            "Wheel Loaders" to "Construction",
            "Track Loaders" to "Construction",
            "Skid Steer Loaders" to "Construction",
            "Tractor Loader Backhoe" to "Construction",

            //
            // Utility Vehicles
            //

            "Mid-Size Utility Vehicles" to "Utility Vehicles",
            "Full-Size Gas Utility Vehicles" to "Utility Vehicles",
            "Full-Size Diesel Utility Vehicles" to "Utility Vehicles"
        )
    }

    private val equipmentService = KubotaEquipmentService(config = config, couchbaseDb = couchbaseDb)

    override fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>> {
        return this.equipmentService.getFaultCodes(model = model, codes = codes)
    }

    override fun getManualInfo(model: String): Promise<List<ManualInfo>> {
        return this.equipmentService.getManualInfo(model = model)
    }

    override fun getModel(model: String): Promise<EquipmentModel?> {
        return this.equipmentService.getModel(model = model)
    }

    override fun getModels(): Promise<List<EquipmentModel>> {
        return this.equipmentService.getModels()
    }

    override fun searchModels(partialModel: String, serial: String): Promise<List<EquipmentModel>> {
        return this.equipmentService.searchModels(partialModel, serial)
    }

    override fun getModels(category: String): Promise<List<EquipmentModel>> {
        val parentCategory = MOCK_REVERSE_SUB_CATEGORIES[category] ?: category
        return this.getModels().map { models -> models.filter { it.category == parentCategory } }
    }

    override fun getCategories(parentCategory: String?): Promise<List<EquipmentCategory>> {
        return parentCategory?.let { category ->
            val categories: List<EquipmentCategory> =
                MOCK_SUB_CATEGORIES[parentCategory]?.let { subCategories ->
                    subCategories.map { EquipmentCategory(category, it, false) }
                } ?: emptyList()
            return Promise.value(categories)
        } ?: this.getModels().map { models ->
            models.map { it.category }.toSet().map { EquipmentCategory(category = it, title = it, hasSubCategories = it.isNotBlank()) }
        }
    }

    override fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>> {
        return Promise.value(MOCK_MAINTENANCE_LIST)
    }
}
