//
//  JSONDecoder.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okio.Buffer
import java.io.ByteArrayInputStream
import java.lang.reflect.Type
import java.net.URI
import java.net.URL
import java.util.*

open class JSONDecoder(
        private val adapters: Array<JSONAdapter>? = null,
        private val factories: Array<JsonAdapter.Factory>? = null
) : Decoder {
    
    private val moshi: Moshi

    init {
        val builder = Moshi.Builder()
        this.factories?.forEach { builder.add(it) }
        builder.add(KotlinJsonAdapterFactory())
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .add(UUIDJsonAdapter())
                .add(URLJsonAdapter())
                .add(URIJsonAdapter())
        this.adapters?.forEach { builder.add(it) }
        this.moshi = builder.build()
    }

    override fun <T> decode(type: Class<T>, value: ByteArray): T? {
        val buffer = Buffer().readFrom(ByteArrayInputStream(value))
        val adapter: JsonAdapter<T> = this.moshi.adapter(type)
        return adapter.fromJson(buffer)
    }

    override fun <T> decode(type: Type, value: ByteArray): T? {
        val buffer = Buffer().readFrom(ByteArrayInputStream(value))
        val adapters = this.moshi.adapter<T>(type)
        return adapters.fromJson(buffer)
    }

    @Throws(DecoderException::class)
    inline fun <reified T> decode(value: ByteArray): T? = this.decode(type = T::class.java, value = value)

}
