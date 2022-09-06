package com.android.kubota.extensions

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.kubota.R
import com.android.kubota.ui.equipment.BaseEquipmentUnitFragment
import com.kubota.service.api.JSONService
import com.kubota.service.domain.*
import java.lang.Exception
import java.net.URL
import java.util.*

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

val EquipmentUnit.hasManual: Boolean
    get() = this.manualInfo.isNotEmpty()

val EquipmentUnit.hasInstrucationalVideo: Boolean
    get() = this.instructionalVideos.isNotEmpty()


//--
val EquipmentUnit.ignitionDrawableResId: Int
    get() {
        return when(this.telematics?.engineRunning) {
            true -> {
                if (telematics?.restartInhibitStatus?.equipmentStatus == RestartInhibitStatusCode.RestartDisabled) {
                    R.drawable.ic_ignition_on_inhibited
                } else {
                    R.drawable.ic_ignition_on
                }
            }
            false -> {
                if (telematics?.restartInhibitStatus?.equipmentStatus == RestartInhibitStatusCode.RestartDisabled) {
                    R.drawable.ic_ignition_off_inhibited
                } else {
                    R.drawable.ic_ignition_off
                }
            }
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

val EquipmentModel.displayName: String
    get() {
        val searchModel = this.searchModel ?: return this.model
        return if (searchModel.isBlank()) this.model else searchModel
    }

private data class ManualWrapper(val wrapper: List<ManualInfo>)
private data class VideoWrapper(val wrapper: List<VideoInfo>)


fun EquipmentUnit.errorMessage(resources: Resources): String? {
    return this.telematics?.faultCodes?.firstOrNull()?.let {
        when (it.j1939Spn != null && it.j1939Fmi != null) {
            true -> resources.getString(
                R.string.equipment_unit_error_message_j1939,
                "${it.j1939Spn}/${it.j1939Fmi} - ${it.description}"
            )
            false -> resources.getString(
                R.string.equipment_unit_error_message,
                "${it.code} - ${it.description}"
            )
        }
    }
}

fun EquipmentModel.toRecentViewedItem(): RecentViewedItem {
    val jsonService = JSONService()
    return RecentViewedItem(
        id = this.model,
        type = EquipmentModel::class.simpleName.toString(),
        title = this.displayName,
        viewedDate = Date(),
        metadata = mapOf(
            "model" to this.model,
            "searchModel" to (this.searchModel ?: ""),
            "modelType" to this.type.toString(),
            "description" to (this.description ?: ""),
            "category" to this.category,
            "heroUrl" to (this.imageResources?.heroUrl?.toString() ?: ""),
            "fullUrl" to (this.imageResources?.fullUrl?.toString() ?: ""),
            "iconUrl" to (this.imageResources?.iconUrl?.toString() ?: ""),
            "guideUrl" to (this.guideUrl?.toString() ?: ""),
            "manualInfo" to (jsonService.encode(ManualWrapper(manualEntries))!!.toString(Charsets.UTF_8)),
            "instructionalVideos" to (jsonService.encode(VideoWrapper(videoEntries))!!.toString(Charsets.UTF_8)),
            "warrantyUrl" to (this.warrantyUrl?.toString() ?: ""),
            "hasFaultCodes" to (this.hasFaultCodes.toString()),
            "hasMaintenanceSchedules" to (this.hasMaintenanceSchedules.toString()),
            "compatibleAttachments" to this.compatibleAttachments.joinToString(","),
            "discontinuedDate" to (this.discontinuedDate?.time?.toString() ?: "")
        )
    )
}

fun RecentViewedItem.toEquipmentModel(): EquipmentModel? {
    if (this.type != EquipmentModel::class.simpleName.toString()) return null
    val model = this.metadata?.get("model")
    val searchModel = this.metadata?.get("searchModel")
    val modelType = this.metadata?.get("modelType")?.let { EquipmentModel.Type.valueOf(it) }
    val description = this.metadata?.get("description")
    val category = this.metadata?.get("category")
    val heroUrl = this.metadata?.get("heroUrl")
    val fullUrl = this.metadata?.get("fullUrl")
    val iconUrl = this.metadata?.get("iconUrl")
    val guideUrl = this.metadata?.get("guideUrl")
    val warrantyUrl = this.metadata?.get("warrantyUrl")
    val hasFaultCodes = this.metadata?.get("hasFaultCodes")?.toBoolean() ?: false
    val hasMaintenanceSchedules = this.metadata?.get("hasMaintenanceSchedules")?.toBoolean() ?: false
    val instructionalVideos = this.metadata?.get("instructionalVideos")?.let {
        JSONService().decode<VideoWrapper>(VideoWrapper::class.java, it.toByteArray(Charsets.UTF_8))
    }?.wrapper ?: emptyList()
    val manualInfo = this.metadata?.get("manualInfo")?.let {
        JSONService().decode<ManualWrapper>(ManualWrapper::class.java, it.toByteArray(Charsets.UTF_8))
    }?.wrapper ?: emptyList()
    val compatibleAttachments = this.metadata?.get("compatibleAttachments")
        ?.split(",") ?: emptyList()
    val discontinuedDateString = this.metadata?.get("discontinuedDate") ?: ""
    val discontinuedDate = if (discontinuedDateString.isEmpty()) {
        null
    } else {
        Date(discontinuedDateString.toLong())
    }

    val imageResources =
        if (heroUrl.isNullOrBlank() && fullUrl.isNullOrBlank() && iconUrl.isNullOrBlank())
            null
        else
            ImageResources(
                if (heroUrl.isNullOrBlank()) null else try { URL(heroUrl) } catch(e: Throwable) { null },
                if (fullUrl.isNullOrBlank()) null else try { URL(fullUrl) } catch(e: Throwable) { null },
                if (iconUrl.isNullOrBlank()) null else try { URL(iconUrl) } catch(e: Throwable) { null }
            )

    if (model.isNullOrBlank() || category.isNullOrBlank() || modelType == null) return null

    return EquipmentModel(
        model = model,
        searchModel = if (searchModel.isNullOrBlank()) null else searchModel,
        type = modelType,
        description = if (description.isNullOrBlank()) null else description,
        imageResources = imageResources,
        category = category,
        guideUrl = guideUrl.toURL(),
        manualEntries = manualInfo,
        videoEntries = instructionalVideos,
        warrantyUrl = warrantyUrl.toURL(),
        hasFaultCodes = hasFaultCodes,
        hasMaintenanceSchedules = hasMaintenanceSchedules,
        compatibleAttachments = compatibleAttachments,
        discontinuedDate = discontinuedDate
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

//fun Context.hasCameraPermissions(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
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

inline fun <reified E: Enum<E>> Bundle.putEnum(key: String?, enum: E): Bundle {
    this.putString(key, enum.toString())
    return this
}
inline fun <reified E: Enum<E>> Bundle.getEnum(key: String?): E? =
    this.getString(key)?.let { enumValueOf<E>(it) }

inline fun <reified E: Enum<E>> Bundle.putEnum(enum: E): Bundle
        = putEnum(E::class.java.canonicalName, enum)

inline fun <reified E: Enum<E>> Bundle.getEnum(): E? =
    this.getEnum<E>(E::class.java.canonicalName)

fun Context.dpToPx(value: Int): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value.toFloat(),
    resources.displayMetrics
).toInt()

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