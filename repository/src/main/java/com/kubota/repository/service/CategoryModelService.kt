package com.kubota.repository.service

import com.kubota.network.service.ModelAPI
import com.kubota.network.service.NetworkResponse
import com.kubota.repository.uimodel.*


class CategoryModelService {

    private val api = ModelAPI()

    fun getCategories(): CategorySyncResults<KubotaEquipmentCategory> {
        return when (val categories = api.getModels()) {
            is NetworkResponse.ServerError -> CategorySyncResults.ServerError()
            is NetworkResponse.IOException -> CategorySyncResults.IOException()
            is NetworkResponse.Success -> {
                val results = mutableListOf<KubotaEquipmentCategory>()
                categories.value.distinctBy {
                    it.category
                }
                    .forEach {
                        when (it.category) {
                            CONSTRUCTION_CATEGORY -> KubotaEquipmentCategory(EquipmentCategory.Construction(), true)
                            MOWERS_CATEGORY -> KubotaEquipmentCategory(EquipmentCategory.Mowers(), true)
                            TRACTORS_CATEGORY -> KubotaEquipmentCategory(EquipmentCategory.Tractors(), true)
                            UTILITY_VEHICLES_CATEGORY -> KubotaEquipmentCategory(EquipmentCategory.UtilityVehicles(), false)
                            else -> null
                        }
                            ?.let {kubotaCategory ->
                                results.add(kubotaCategory)
                            }
                    }

                CategorySyncResults.Success(results)
            }
        }
    }

    fun getSubCategories(category: EquipmentCategory): CategorySyncResults<KubotaModelSubCategory> {
        return when (category) {
                is EquipmentCategory.Construction -> {
                    CategorySyncResults.Success(
                        listOf(
                            KubotaModelSubCategory(category, "Compact Tractor"),
                            KubotaModelSubCategory(category, "Wheel Loaders"),
                            KubotaModelSubCategory(category, "Track Loaders"),
                            KubotaModelSubCategory(category, "Skid Steer Loaders"),
                            KubotaModelSubCategory(category, "Tractor Loader Backhoe")
                        )
                    )
                }
                is EquipmentCategory.Tractors -> {
                    CategorySyncResults.Success(
                        listOf(
                            KubotaModelSubCategory(category, "Sub-Compact"),
                            KubotaModelSubCategory(category, "Compact"),
                            KubotaModelSubCategory(category, "Economy Utility"),
                            KubotaModelSubCategory(category, "Utility"),
                            KubotaModelSubCategory(category, "Specialty"),
                            KubotaModelSubCategory(category, "Agriculture"),
                            KubotaModelSubCategory(category, "Tractor Loader Backhoe")
                        )
                    )
                }
                is EquipmentCategory.Mowers -> {
                    CategorySyncResults.Success(
                        listOf(
                            KubotaModelSubCategory(category, "Zero-Turn Mowers"),
                            KubotaModelSubCategory(category, "Stand-on Mowers"),
                            KubotaModelSubCategory(category, "Walk-Behind Mowers"),
                            KubotaModelSubCategory(category, "Fonrt Mount Mowers"),
                            KubotaModelSubCategory(category, "Lawn & Garden Tractors")
                        )
                    )
                }
                else -> CategorySyncResults.Success(emptyList())
        }
    }

    fun getModels(category: EquipmentCategory): CategorySyncResults<KubotaModelSubCategory> {
        return when (val categories = api.getModels()) {
            is NetworkResponse.ServerError -> CategorySyncResults.ServerError()
            is NetworkResponse.IOException -> CategorySyncResults.IOException()
            is NetworkResponse.Success -> {
                val results: List<KubotaModelSubCategory> =
                    categories.value
                        .filter { it.category == category.toString() }
                        .map { KubotaModelSubCategory(category, it.model) }

                CategorySyncResults.Success(results)
            }
        }
    }

    fun getModels(subCategory: KubotaModelSubCategory) = getModels(subCategory.category)
}

sealed class CategorySyncResults<out T> {
    class Success<T>(val results: List<T>): CategorySyncResults<T>()
    class ServerError: CategorySyncResults<Nothing>()
    class IOException: CategorySyncResults<Nothing>()
}