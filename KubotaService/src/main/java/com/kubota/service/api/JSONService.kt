package com.kubota.service.api

import com.inmotionsoftware.foundation.service.JSONDecoder
import com.inmotionsoftware.foundation.service.JSONEncoder
import java.lang.reflect.Type

class JSONService {

    @Throws
    fun <T : Any> encode(value: T): ByteArray? {
        return JSONEncoder().encode(value)
    }

    @Throws
    fun <T> decode(type: Class<T>, value: ByteArray): T? {
        return JSONDecoder().decode(type = type, value = value)
    }

    @Throws
    fun <T> decode(type: Type, value: ByteArray): T? {
        return JSONDecoder().decode(type = type, value = value)
    }

}
