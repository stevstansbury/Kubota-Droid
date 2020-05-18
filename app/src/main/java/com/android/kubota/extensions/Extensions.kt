package com.android.kubota.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.android.kubota.R
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.ui.FlowActivity
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.viewmodel.UIDealer
import com.android.kubota.viewmodel.UIEquipment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Equipment
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import com.kubota.repository.service.SearchDealer as ServiceDealer

private fun String?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

//
// Model classes' related extension methods
//

const val CONSTRUCTION_CATEGORY = "Construction"
const val MOWERS_CATEGORY = "Mowers"
const val TRACTORS_CATEGORY = "Tractors"
const val UTILITY_VEHICLES_CATEGORY = "Utility Vehicles"

val EquipmentUnit.imageResId: Int
    get() = when(category) {
            CONSTRUCTION_CATEGORY,
            MOWERS_CATEGORY,
            TRACTORS_CATEGORY,
            UTILITY_VEHICLES_CATEGORY -> CategoryUtils.getEquipmentImage(category ?: "", model)
            else -> 0
        }

val EquipmentUnit.categoryResId: Int
    get() = when(category) {
        CONSTRUCTION_CATEGORY -> R.string.equipment_construction_category
        MOWERS_CATEGORY -> R.string.equipment_mowers_category
        TRACTORS_CATEGORY -> R.string.equipment_tractors_category
        UTILITY_VEHICLES_CATEGORY -> R.string.equipment_utv_category
        else -> 0
    }

val EquipmentUnit.hasManual: Boolean
    get() = manualLocation.isNullOrEmpty().not()

val EquipmentCategory.equipmentImageResId: Int?
    get() {
        return when (this.category) {
            CONSTRUCTION_CATEGORY -> R.drawable.ic_construction_category_thumbnail
            MOWERS_CATEGORY -> R.drawable.ic_mower_category_thumbnail
            TRACTORS_CATEGORY -> R.drawable.ic_tractor_category_thumbnail
            UTILITY_VEHICLES_CATEGORY -> R.drawable.ic_utv_category_thumbnail
            else -> null
    }
}

val EquipmentCategory.displayNameStringRes: Int?
    get() {
        return when (this.category) {
            CONSTRUCTION_CATEGORY -> R.string.equipment_construction_category
            MOWERS_CATEGORY -> R.string.equipment_mowers_category
            TRACTORS_CATEGORY -> R.string.equipment_tractors_category
            UTILITY_VEHICLES_CATEGORY -> R.string.equipment_utv_category
            else -> null
        }
    }

val EquipmentModel.equipmentImageResId: Int?
    get() {
        return when (this.category) {
            CONSTRUCTION_CATEGORY -> R.drawable.ic_construction_category_thumbnail
            MOWERS_CATEGORY -> R.drawable.ic_mower_category_thumbnail
            TRACTORS_CATEGORY -> R.drawable.ic_tractor_category_thumbnail
            UTILITY_VEHICLES_CATEGORY -> R.drawable.ic_utv_category_thumbnail
            else -> null
        }
    }

//
//

fun Equipment.toUIEquipment(): UIEquipment {
    return when (category) {
        CONSTRUCTION_CATEGORY -> UIEquipment(id, nickname, model, serialNumber, R.string.equipment_construction_category, CategoryUtils.getEquipmentImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide, engineHours)
        MOWERS_CATEGORY -> UIEquipment(id, nickname, model, serialNumber, R.string.equipment_mowers_category, CategoryUtils.getEquipmentImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide, engineHours)
        TRACTORS_CATEGORY -> UIEquipment(id, nickname, model, serialNumber, R.string.equipment_tractors_category, CategoryUtils.getEquipmentImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide, engineHours)
        UTILITY_VEHICLES_CATEGORY -> UIEquipment(id, nickname, model, serialNumber, R.string.equipment_utv_category, CategoryUtils.getEquipmentImage(category, model),
            !manualLocation.isNullOrEmpty(), hasGuide, engineHours)
        else -> UIEquipment(id, nickname, model, serialNumber, 0, 0, !manualLocation.isNullOrEmpty(), hasGuide, engineHours)
    }

}

fun Dealer.toUIDealer(): UIDealer {
    return UIDealer(id = id, name = name, address = streetAddress, city = city, state = stateCode, postalCode = postalCode, phone = phone, website = webAddress, dealerNumber = number)
}

fun ServiceDealer.toDealer(isFavorited: Boolean): SearchDealer {
    return SearchDealer(
        serverId = serverId, name = name, streetAddress = streetAddress, city = city,
        stateCode = stateCode, postalCode = postalCode, countryCode = countryCode, phone = phone,
        webAddress = webAddress, dealerNumber = dealerNumber, latitude = latitude, longitude = longitude,
        distance = distance, isFavorited = isFavorited
    )
}

//
// FlowActivity extension methods
//
fun FlowActivity.showServerErrorSnackBar() {
    makeSnackbar()?.apply {
        setText(this.context.getString(R.string.server_error_message))
        duration = BaseTransientBottomBar.LENGTH_INDEFINITE
        setAction(this.context.getString(R.string.dismiss)) {}
        show()
    }
}

//
// Context extension methods
//
fun Context.isLocationEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.isCameraEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
