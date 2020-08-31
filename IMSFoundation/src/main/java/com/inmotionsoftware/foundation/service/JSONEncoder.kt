//
//  JSONEncoder.kt
//
//  Copyright © 2018 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.Moshi
import okio.Buffer
import java.net.URI
import java.net.URL
import java.util.*

open class JSONEncoder(private val adapters: Array<JSONAdapter>? = null) : Encoder {
    private val moshi: Moshi

    init {
        val builder = Moshi.Builder()
        builder.add(KotlinJsonAdapterFactory())
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .add(UUIDJsonAdapter())
                .add(URLJsonAdapter())
                .add(URIJsonAdapter())

        this.adapters?.forEach { builder.add(it) }
        this.moshi = builder.build()
    }

    @Throws(EncoderException::class)
    override fun <T:Any> encode(value: T): ByteArray? {
        val adapter: JsonAdapter<T> = this.moshi.adapter(value.javaClass)
        val buffer = Buffer()
        adapter.toJson(buffer, value)
        return buffer.readByteArray()
    }

}
