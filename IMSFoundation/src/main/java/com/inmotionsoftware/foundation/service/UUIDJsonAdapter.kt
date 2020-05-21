//
//  UUIDJsonAdapter.kt
//
//  Copyright © 2019 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.*
import java.util.*

class UUIDJsonAdapter {
    @FromJson
    fun fromJson(uuid: String?): UUID? {
        return uuid?.let { UUID.fromString(it) }
    }
    @ToJson
    fun toJson(value: UUID?): String? {
        return value?.toString()
    }
}
