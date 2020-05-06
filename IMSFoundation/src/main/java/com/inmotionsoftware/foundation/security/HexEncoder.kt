//
//  HexEncoder.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.security

object HexEncoder {

    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun toHex(data: ByteArray): String {
        val hexChars = CharArray(data.size * 2)

        var j = 0
        var n = 0
        while (j < data.size) {
            val v = data[j].toInt() and 0xFF
            hexChars[n++] = hexArray[(v ushr 4)]
            hexChars[n++] = hexArray[v and 0x0F]
            j++
        }
        return String(hexChars)
    }

    fun toBytes(hex: String): ByteArray {
        val hexChars = hex.toCharArray()
        val bytes = ByteArray(hexChars.size / 2)

        var i = 0
        var n = 0
        while (i < hexChars.size) {
            val b1 = hexCharToByte(hexChars[i])
            val b2 = hexCharToByte(hexChars[i + 1])
            bytes[n++] = (b1.toInt() shl 4 or (b2.toInt() and 0xF)).toByte()
            i += 2
        }
        return bytes
    }

    //
    // Private Methods
    //

    private fun hexCharToByte(ch: Char): Byte {
        when (ch) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> return (ch - '0').toByte()
            'A', 'a' -> return 0xA
            'B', 'b' -> return 0xB
            'C', 'c' -> return 0xC
            'D', 'd' -> return 0xD
            'E', 'e' -> return 0xE
            'F', 'f' -> return 0xF
            else -> throw IllegalArgumentException("Invalid hex char: " + ch)
        }
    }

}
