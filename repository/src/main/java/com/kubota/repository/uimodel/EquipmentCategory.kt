package com.kubota.repository.uimodel

const val CONSTRUCTION_CATEGORY = "Construction"
const val MOWERS_CATEGORY = "Mowers"
const val TRACTORS_CATEGORY = "Tractors"
const val UTILITY_VEHICLES_CATEGORY = "Utility Vehicles"

sealed class EquipmentCategory {
    override fun toString(): String {
        return when(this) {
            is Construction -> CONSTRUCTION_CATEGORY
            is Mowers -> MOWERS_CATEGORY
            is Tractors -> TRACTORS_CATEGORY
            is UtilityVehicles -> UTILITY_VEHICLES_CATEGORY
        }
    }

    class Construction: EquipmentCategory()
    class Mowers: EquipmentCategory()
    class Tractors: EquipmentCategory()
    class UtilityVehicles: EquipmentCategory()
}

data class KubotaEquipmentCategory(
    val category: EquipmentCategory,
    val hasSubCategories: Boolean
)

data class KubotaModelSubCategory(
    val category: EquipmentCategory,
    val title: String
)

data class KubotaModel(
    val category: EquipmentCategory,
    val subCategory: KubotaModelSubCategory?,
    val name: String,
    val guidesUrl: String?
)