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
import com.android.kubota.ui.FlowActivity
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.android.kubota.utility.CategoryUtils
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.inmotionsoftware.promisekt.PMKError
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject
import com.kubota.service.domain.*
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

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
    get() = this.manualInfo.isNotEmpty()

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

fun EquipmentModel.toRecentViewedItem(): RecentViewedItem {
    return RecentViewedItem(
        id = this.model,
        type = EquipmentModel::class.simpleName.toString(),
        title = this.model,
        viewedDate = Date(),
        metadata = mapOf(
            "model" to this.model,
            "category" to this.category,
            "guideUrl" to (this.guideUrl?.toString() ?: ""),
            "manualUrls" to (this.manualUrls ?: emptyList()).foldRight("") {
                    uri, acc -> "${if (acc.isEmpty()) "" else " "} ${URLEncoder.encode(uri.toString(), "UTF-8")}"
            }
        )
    )
}

fun RecentViewedItem.toEquipmentMode(): EquipmentModel? {
    if (this.type != EquipmentModel::class.simpleName.toString()) return null
    val model = this.metadata?.get("model")
    val category = this.metadata?.get("category")
    val guideUrl = this.metadata?.get("guideUrl")
    val manualUrls: List<URL>? = this.metadata?.get("manualUrls")?.let {
        if (it.isEmpty()) return@let emptyList<URL>()
        val urls = ArrayList<URL>()
        it.split(delimiters = *charArrayOf(' ')).forEach {str ->
            try { urls.add(URL(str)) } catch(e: Throwable) { }
        }
        urls
    } ?: emptyList()

    if (model.isNullOrEmpty() || category.isNullOrEmpty()) return null
    return EquipmentModel(model!!, category!!, if (guideUrl.isNullOrEmpty()) null else URL(guideUrl!!), manualUrls)
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

fun EquipmentUnit.displayInfo(context: Fragment): BaseEquipmentUnitFragment.EquipmentUnitDisplayInfo {
    return BaseEquipmentUnitFragment.EquipmentUnitDisplayInfo(
        imageResId = this.imageResId,
        modelName = this.model,
        serialNumber = if (this.serial.isNullOrBlank()) {
            context.getString(R.string.equipment_serial_number)
        } else {
            context.getString(R.string.equipment_serial_number_fmt, this.serial)
        },
        nickname = if (this.nickName.isNullOrBlank()) context.getString(
            R.string.no_equipment_name_fmt,
            this.model
        ) else this.nickName!!,
        engineHours = "${this.engineHours ?: 0.0}"
    )
}