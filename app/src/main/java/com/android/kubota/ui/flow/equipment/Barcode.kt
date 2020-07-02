package com.android.kubota.ui.flow.equipment

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class Barcode: Parcelable {
    @Parcelize
    data class QR(val value: String): Barcode(), Parcelable
}

//
// Example of Equipment QR code
//   "PINKBCK0804AK3C48391\tMDRD83823280\tSN48391\tWGNKX080-4SR3A\tEMC1J77310000\tESN8KE1031"
//
// Interpretation:
//   PIN: KBCK0804AK3C48391
//   KBT Model Code (MD): RD83823280
//   Serial Number (SN): 48391
//   Whole Good Number (WGN): KX080-4SR3A
//   Engine Model Code (EMC): 1J77310000
//   Engine Serial Number (ESN): 8KE1031
//
// Basic rule is:
//  "PIN" + PIN#(17 digits) + (tab) + "MD" + KBT model code(10 digits) + (tab) + "SN" + serial#(5 digits?)
//  + (tab) + "WGN" + KTC WG#(several kinds of digits) + (tab) + "EMC" + Engine model code(10 digits)
//  + (tab) + "ESN" + Engine serial#
//

val Barcode.equipmentPIN: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "PIN"
                val pin = this.value.split('\t').firstOrNull { code ->
                    code.trim().startsWith(prefix, ignoreCase = true) && code.length == 20
                } ?: return null

                pin.substring(prefix.length)
            }
        }
    }

val Barcode.equipmentSerial: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "SN"
                val serial = this.value.split('\t').firstOrNull { code ->
                    code.trim().startsWith(prefix, ignoreCase = true)
                } ?: return null

                serial.substring(prefix.length)
            }
        }
    }

val Barcode.equipmentModel: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "WGN"
                val model = this.value.split('\t').firstOrNull { code ->
                    code.trim().startsWith(prefix, ignoreCase = true)
                } ?: return null

                model.substring(prefix.length)
            }
        }
    }

val Barcode.isValidEquipmentBarcode: Boolean
    get() {
        if (this.equipmentPIN == null && this.equipmentSerial == null) return false
        return this.equipmentModel != null
    }
