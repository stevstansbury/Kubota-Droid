package com.inmotionsoftware.foundation.service

import android.net.Uri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.net.URI
import java.net.URL

class URLJsonAdapter: JsonAdapter<URL>() {
    override fun fromJson(reader: JsonReader): URL? {
        val string = reader.nextString()
        return URL(string)
    }

    override fun toJson(writer: JsonWriter, value: URL?) {
        value?.let { writer.value(value.toString()) }
    }
}

class URIJsonAdapter: JsonAdapter<URI>() {
    override fun fromJson(reader: JsonReader): URI? {
        val string = reader.nextString()
        return URI(string)
    }

    override fun toJson(writer: JsonWriter, value: URI?) {
        value?.let { writer.value(value.toString()) }
    }
}
