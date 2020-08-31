//
//  Decoder.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import java.lang.reflect.Type

class DecoderException : Throwable {
    constructor(): super()
    constructor(message: String): super(message = message)
    constructor(cause: Throwable): super(cause = cause)
    constructor(message: String, cause: Throwable): super(message = message, cause = cause)
}

/**
 * A type that can decode values from a native format into in-memory representations.
 */
interface Decoder {

    @Throws(DecoderException::class)
    fun <T> decode(type: Class<T>, value: ByteArray): T?

    @Throws(DecoderException::class)
    fun <T> decode(type: Type, value: ByteArray): T?

}
