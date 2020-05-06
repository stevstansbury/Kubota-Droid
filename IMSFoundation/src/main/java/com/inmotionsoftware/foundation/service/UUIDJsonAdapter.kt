//
//  UUIDJsonAdapter.kt
//
//  Copyright © 2019 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.util.*

class UUIDJsonAdapter: JsonAdapter<UUID>() {
    override fun fromJson(reader: JsonReader): UUID? {
        val string = reader.nextString()
        return UUID.fromString(string)
    }

    override fun toJson(writer: JsonWriter, value: UUID?) {
        value?.let { writer.value(value.toString()) }
    }
}
