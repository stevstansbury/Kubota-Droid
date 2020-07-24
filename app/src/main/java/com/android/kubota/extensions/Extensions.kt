package com.android.kubota.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.kubota.R
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.kubota.service.domain.*
import com.squareup.moshi.Json
import java.lang.Exception
import java.net.URL
import java.net.URLDecoder
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
            CONSTRUCTION_CATEGORY -> R.drawable.ic_construction_category_thumbnail
            MOWERS_CATEGORY -> R.drawable.ic_mower_category_thumbnail
            TRACTORS_CATEGORY -> R.drawable.ic_tractor_category_thumbnail
            UTILITY_VEHICLES_CATEGORY -> R.drawable.ic_utv_category_thumbnail
            else -> R.drawable.ic_construction_category_thumbnail
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
        return when(this.telematics?.engineRunning) {
            true -> R.drawable.ic_ignition_on
            false -> R.drawable.ic_ignition_off
            else -> 0
        }
    }

val EquipmentUnit.motionDrawableResId: Int
    get() {
        return when(this.telematics?.motionState) {
            EquipmentMotionState.MOVING -> R.drawable.ic_in_motion
            EquipmentMotionState.IN_TRANSPORT -> R.drawable.ic_in_transport
            EquipmentMotionState.STATIONARY -> R.drawable.ic_parked
            else -> 0
        }
    }

val EquipmentUnit.numberOfWarnings: Int
    get() { return this.telematics?.faultCodes?.size ?: 0 }

val EquipmentUnit.errorMessage: String?
    get() {
        return this.telematics?.faultCodes?.firstOrNull()?.let {
            "E:${it.code} - ${it.description}"
        }
    }

val EquipmentUnit.engineHours: Double
    get() {
        return this.telematics?.cumulativeOperatingHours ?: this.userEnteredEngineHours ?: 0.0
    }

val EquipmentUnit.hasTelematics: Boolean
    get() {
        return this.telematics != null
    }

//--
fun clamp(value: Double, min: Double, max: Double): Double =
    Math.max(Math.min(value, max), min)

fun clamp(value: Float, min: Float, max: Float): Float =
    Math.max(Math.min(value, max), min)

fun clamp(value: Int, min: Int, max: Int): Int =
    Math.max(Math.min(value, max), min)

fun clamp(value: Long, min: Long, max: Long): Long =
    Math.max(Math.min(value, max), min)

//--

val EquipmentCategory.equipmentImageResId: Int?
    get() {
        return when (this.parentCategory ?: this.category) {
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
            "searchModel" to (this.searchModel ?: ""),
            "description" to (this.description ?: ""),
            "category" to this.category,
            "subcategory" to this.subcategory,
            "heroUrl" to (this.imageResources?.heroUrl?.toString() ?: ""),
            "fullUrl" to (this.imageResources?.fullUrl?.toString() ?: ""),
            "iconUrl" to (this.imageResources?.iconUrl?.toString() ?: ""),
            "guideUrl" to (this.guideUrl?.toString() ?: ""),
            "manualUrls" to (this.manualUrls ?: emptyList()).foldRight("") {
                    uri, acc -> "${if (acc.isEmpty()) "" else "\t"} ${URLEncoder.encode(uri.toString(), "UTF-8")}"
            },
            "warrantyUrl" to (this.warrantyUrl?.toString() ?: ""),
            "hasFaultCodes" to (this.hasFaultCodes.toString()),
            "hasMaintenanceSchedules" to (this.hasMaintenanceSchedules.toString())
        )
    )
}

fun RecentViewedItem.toEquipmentModel(): EquipmentModel? {
    if (this.type != EquipmentModel::class.simpleName.toString()) return null
    val model = this.metadata?.get("model")
    val searchModel = this.metadata?.get("searchModel")
    val description = this.metadata?.get("description")
    val category = this.metadata?.get("category")
    val subcategory = this.metadata?.get("subcategory")
    val heroUrl = this.metadata?.get("heroUrl")
    val fullUrl = this.metadata?.get("fullUrl")
    val iconUrl = this.metadata?.get("iconUrl")
    val guideUrl = this.metadata?.get("guideUrl")
    val warrantyUrl = this.metadata?.get("warrantyUrl")
    val hasFaultCodes = this.metadata?.get("hasFaultCodes")?.toBoolean() ?: false
    val hasMaintenanceSchedules = this.metadata?.get("hasMaintenanceSchedules")?.toBoolean() ?: false
    val manualUrls: List<URL>? = this.metadata?.get("manualUrls")?.let {
        if (it.isEmpty()) return@let emptyList<URL>()
        val urls = ArrayList<URL>()
        if (it.contains('\t')) {
            it.split(delimiters = *charArrayOf('\t')).forEach {str ->
                try {
                    urls.add(URL(URLDecoder.decode(str, "UTF-8")))
                } catch(e: Throwable) {
                }
            }
        } else {
            try {
                urls.add(URL(URLDecoder.decode(it, "UTF-8")))
            } catch(e: Throwable) {
            }
        }
        urls
    } ?: emptyList()

    val imageResources =
        if (heroUrl.isNullOrBlank() && fullUrl.isNullOrBlank() && iconUrl.isNullOrBlank())
            null
        else
            ImageResources(
                if (heroUrl.isNullOrBlank()) null else try { URL(heroUrl) } catch(e: Throwable) { null },
                if (fullUrl.isNullOrBlank()) null else try { URL(fullUrl) } catch(e: Throwable) { null },
                if (iconUrl.isNullOrBlank()) null else try { URL(iconUrl) } catch(e: Throwable) { null }
            )

    if (model.isNullOrBlank() || category.isNullOrBlank() || subcategory.isNullOrBlank()) return null
    return EquipmentModel(
                model = model,
                searchModel =  if (searchModel.isNullOrBlank()) null else searchModel,
                description = if (description.isNullOrBlank()) null else description,
                imageResources = imageResources,
                category = category,
                subcategory = subcategory,
                guideUrl = guideUrl.toURL(),
                manualUrls = manualUrls,
                warrantyUrl = warrantyUrl.toURL(),
                hasFaultCodes = hasFaultCodes,
                hasMaintenanceSchedules = hasMaintenanceSchedules
            )
}

private fun String?.toURL(): URL? {
    if (!this.isNullOrBlank()) {
        try {
            return URL(this)
        } catch (e: Exception) {

        }
    }

    return null
}

//
// Context extension methods
//
fun Context.isLocationEnabled(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.hasCameraPermissions(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
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
        nickname = if (!this.nickName.isNullOrBlank()) this.nickName!! else this.model,
        engineHours = "${this.engineHours}"
    )
}

/**
 * Live Data Extensions
 **/
fun <T, A, B> LiveData<A>.combineAndCompute(other: LiveData<B>, onChange: (A, B) -> T): MediatorLiveData<T> {

    var source1emitted = false
    var source2emitted = false

    val result = MediatorLiveData<T>()

    val mergeF = {
        val source1Value = this.value
        val source2Value = other.value

        if (source1emitted && source2emitted) {
            result.value = onChange.invoke(source1Value!!, source2Value!! )
        }
    }

    result.addSource(this) { source1emitted = true; mergeF.invoke() }
    result.addSource(other) { source2emitted = true; mergeF.invoke() }

    return result
}