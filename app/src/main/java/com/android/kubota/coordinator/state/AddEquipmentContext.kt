package com.android.kubota.coordinator.state

import android.os.Parcelable
import com.android.kubota.ui.flow.equipment.Barcode
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import kotlinx.android.parcel.Parcelize

sealed class AddEquipmentResult: Parcelable {
    @Parcelize
    class ViewEquipmentUnit(val unit: EquipmentUnit): AddEquipmentResult(), Parcelable
    @Parcelize
    class ViewEquipmentModel(val model: EquipmentModel): AddEquipmentResult(), Parcelable
}

@Parcelize
data class ScanSearchResult(
    val barcode: Barcode,
    val models: List<EquipmentModel>
): Parcelable

@Parcelize
data class AddEquipmentUnitContext(
    val barcode: Barcode,
    val model: EquipmentModel
): Parcelable
