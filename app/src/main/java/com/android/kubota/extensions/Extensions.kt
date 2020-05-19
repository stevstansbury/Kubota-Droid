package com.android.kubota.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.viewmodel.SearchDealer
import com.android.kubota.ui.FlowActivity
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.viewmodel.UIDealer
import com.android.kubota.viewmodel.UIEquipment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.inmotionsoftware.promisekt.PMKError
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject
import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Equipment
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import kotlin.random.Random
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

//--
val EquipmentUnit.ignitionDrawableResId: Int
    get() {
        return when(this.engineRunning) {
            true -> R.drawable.ic_ignition_on
            false -> R.drawable.ic_ignition_off
            else -> 0
        }
    }

val EquipmentUnit.motionDrawableResId: Int
    get() {
        return when(Random.nextInt(0, 3)) {
            1 -> R.drawable.ic_in_motion
            2 -> R.drawable.ic_in_transport
            3 -> R.drawable.ic_parked
            else -> 0
        }
    }

val EquipmentUnit.numberOfWarnings: Int
    get() { return Random.nextInt(0, 10) }

val EquipmentUnit.errorMessage: String?
    get() {
        return if (numberOfWarnings > 0) {
            "E:9200 –mass air flow sensor failure. Contact your dealer."
        } else {
            null
        }
    }

//--

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
    val categoryStringResId = when (category) {
        CONSTRUCTION_CATEGORY -> R.string.equipment_construction_category
        MOWERS_CATEGORY -> R.string.equipment_mowers_category
        TRACTORS_CATEGORY -> R.string.equipment_tractors_category
        UTILITY_VEHICLES_CATEGORY -> R.string.equipment_utv_category
        else -> 0
    }

    return UIEquipment(
        id,
        nickname,
        model,
        serialNumber,
        categoryStringResId,
        CategoryUtils.getEquipmentImage(category, model),
        !manualLocation.isNullOrEmpty(),
        hasGuide,
        engineHours,
        battery,
        fuelLevel,
        defLevel,
        engineState
    )
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

fun Activity.showDialog(message: Int, positiveButton: Int, cancelable: Boolean = true): Promise<Unit> {
    val pending = Promise.pending<Unit>()
    android.app.AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(cancelable)
        .setOnCancelListener() { pending.second.reject(PMKError.cancelled()) }
        .setOnDismissListener {
            if (pending.first.result == null) pending.second.reject(PMKError.cancelled())
        }
        .setPositiveButton(positiveButton) { _: DialogInterface, _: Int -> pending.second.fulfill(Unit) }
        .show()
    return pending.first
}

fun Activity.showDialog(message: CharSequence, positiveButton: CharSequence = "Ok", cancelable: Boolean = true): Promise<Unit> {
    val pending = Promise.pending<Unit>()
    android.app.AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(cancelable)
        .setOnCancelListener() { pending.second.reject(PMKError.cancelled()) }
        .setOnDismissListener {
            if (pending.first.result == null) pending.second.reject(PMKError.cancelled())
        }
        .setPositiveButton(positiveButton) { _: DialogInterface, _: Int -> pending.second.fulfill(Unit) }
        .show()
    return pending.first
}

fun Fragment.showDialog(message: CharSequence, positiveButton: CharSequence = "Ok", cancelable: Boolean = true): Promise<Unit> =
    this.requireActivity().showDialog(message, positiveButton, cancelable)

//
// Context extension methods
//
fun Context.isLocationEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.isCameraEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
