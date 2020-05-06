//
//  Encoder.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

class EncoderException : Throwable {
    constructor(): super()
    constructor(message: String): super(message = message)
    constructor(cause: Throwable): super(cause = cause)
    constructor(message: String, cause: Throwable): super(message = message, cause = cause)
}

/**
 * A type that can encode values into a native format for external representation.
 */
interface Encoder {

    @Throws(EncoderException::class)
    fun <T : Any> encode(value: T): ByteArray?

}
