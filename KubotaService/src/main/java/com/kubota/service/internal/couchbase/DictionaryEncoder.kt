package com.kubota.service.internal.couchbase

import com.inmotionsoftware.foundation.service.EncoderException
import com.inmotionsoftware.foundation.service.UUIDJsonAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Buffer
import java.util.*

//
// A quick, but inefficient, implementation for encoding an Encodable object
// into a JSON dictionary using JSONEncoder. If performance becomes an issue, we
// would need to implement a custom Encoder.
//
class DictionaryEncoder {

    private val moshi: Moshi

    init {
        val builder = Moshi.Builder()
        builder.add(KotlinJsonAdapterFactory())
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .add(UUID::class.java, UUIDJsonAdapter())
        this.moshi = builder.build()
    }

    @Throws(EncoderException::class)
    fun <T:Any> encode(value: T): Map<String, Any>? {
        val adapter: JsonAdapter<T> = this.moshi.adapter(value.javaClass)
        val dictionary = adapter.toJsonValue(value) as? Map<String, Any>
        return dictionary
    }

}
