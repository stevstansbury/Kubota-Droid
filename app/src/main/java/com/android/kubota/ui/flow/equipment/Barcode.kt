package com.android.kubota.ui.flow.equipment

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class Barcode: Parcelable {
    @Parcelize
    data class QR(val value: String): Barcode(), Parcelable
}

//
// Example of Equipment QR code - Format 1
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

//
// Example of Equipment QR code - Format 2
//   "Z421KWT     KBGHGCC0CLGC32739"
//
// Interpretation:
//   PIN: KBGHGCC0CLGC32739
//   Model: Z421KWT

val Barcode.equipmentPIN: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "PIN"
                val codes = this.value.split('\t', ' ').filter { !it.trim().isBlank() }
                when {
                    codes.size == 1 || codes.size == 2 -> {
                        codes.firstOrNull { it.trim().length == 17 }
                    }
                    codes.size > 2 -> {
                        codes.firstOrNull {
                            it.trim().startsWith(prefix, ignoreCase = true) && it.length == 20
                        }?.substring(prefix.length)
                    }
                    else -> {
                        null
                    }
                }
            }
        }
    }

val Barcode.equipmentSerial: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "SN"
                val codes = this.value.split('\t', ' ').filter { it.trim().isNotBlank() }
                when {
                    codes.size == 2 -> {
                        codes.firstOrNull { it.trim().length >= 5 && it.toIntOrNull() != null }
                    }
                    codes.size > 2 -> {
                        codes.firstOrNull {
                            it.trim().startsWith(prefix, ignoreCase = true) && it.length >= 7
                        }?.substring(prefix.length)
                    }
                    else -> {
                        null
                    }
                }
            }
        }
    }

val Barcode.equipmentModel: String?
    get() {
        return when (this) {
            is Barcode.QR -> {
                val prefix = "WGN"
                val codes = this.value.split('\t', ' ').filter { !it.trim().isBlank() }
                when {
                    codes.size == 2 -> {
                        codes.firstOrNull { it.trim().length < 17 }
                    }
                    codes.size > 2 -> {
                        codes.firstOrNull {
                            it.trim().startsWith(prefix, ignoreCase = true)
                        }?.substring(prefix.length)
                    }
                    else -> {
                        null
                    }
                }
            }
        }
    }

val Barcode.isValidEquipmentBarcode: Boolean
    get() {
        if (this.equipmentPIN.isNullOrBlank() && this.equipmentSerial.isNullOrBlank()) return false
        return !this.equipmentPIN.isNullOrBlank() || !this.equipmentModel.isNullOrBlank()
    }
