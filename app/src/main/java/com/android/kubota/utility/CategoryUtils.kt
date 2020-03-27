package com.android.kubota.utility

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.kubota.R

object CategoryUtils {

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

    @DrawableRes
    fun getEquipmentImage(category: String, model: String): Int {
        return when (category) {
            CONSTRUCTION_CATEGORY -> {
                when {
                    model.startsWith("R", true) -> R.drawable.ic_equipment_r_series
                    model.startsWith("SSV", true) -> R.drawable.ic_equipment_ssv_series
                    model.startsWith("SVL", true) -> R.drawable.ic_equipment_svl_series
                    model.startsWith("K", true) ||
                            model.startsWith("KX", true) ||
                            model.startsWith("U", true) -> R.drawable.ic_equipment_k_kx_u_series
                    else -> R.drawable.ic_construction_category_thumbnail
                }
            }
            MOWERS_CATEGORY -> R.drawable.ic_mower_category_thumbnail
            TRACTORS_CATEGORY -> R.drawable.ic_tractor_category_thumbnail
            UTILITY_VEHICLES_CATEGORY -> R.drawable.ic_utv_category_thumbnail
            else -> 0
        }
    }

    @DrawableRes
    fun getEquipmentImage(category: EquipmentCategory, model: String): Int {
        return when (category) {
            is EquipmentCategory.Construction -> {
                when {
                    model.startsWith("R", true) -> R.drawable.ic_equipment_r_series
                    model.startsWith("SSV", true) -> R.drawable.ic_equipment_ssv_series
                    model.startsWith("SVL", true) -> R.drawable.ic_equipment_svl_series
                    model.startsWith("K", true) ||
                            model.startsWith("KX", true) ||
                            model.startsWith("U", true) -> R.drawable.ic_equipment_k_kx_u_series
                    else -> R.drawable.ic_construction_category_thumbnail
                }
            }
            is EquipmentCategory.Mowers -> R.drawable.ic_mower_category_thumbnail
            is EquipmentCategory.Tractors -> R.drawable.ic_tractor_category_thumbnail
            is EquipmentCategory.UtilityVehicles -> R.drawable.ic_utv_category_thumbnail
        }
    }

    @StringRes
    fun getEquipmentName(category: EquipmentCategory): Int {
        return when (category) {
            is EquipmentCategory.Construction -> R.string.equipment_construction_category
            is EquipmentCategory.Mowers -> R.string.equipment_mowers_category
            is EquipmentCategory.Tractors -> R.string.equipment_tractors_category
            is EquipmentCategory.UtilityVehicles -> R.string.equipment_utv_category
        }
    }

    const val CONSTRUCTION_CATEGORY = "Construction"
    const val MOWERS_CATEGORY = "Mowers"
    const val TRACTORS_CATEGORY = "Tractors"
    const val UTILITY_VEHICLES_CATEGORY = "Utility Vehicles"
    val CATEGORY_MAP = mapOf(
        Pair(CONSTRUCTION_CATEGORY, EquipmentCategory.Construction()),
        Pair(MOWERS_CATEGORY, EquipmentCategory.Mowers()),
        Pair(TRACTORS_CATEGORY, EquipmentCategory.Tractors()),
        Pair(UTILITY_VEHICLES_CATEGORY, EquipmentCategory.UtilityVehicles()))

}