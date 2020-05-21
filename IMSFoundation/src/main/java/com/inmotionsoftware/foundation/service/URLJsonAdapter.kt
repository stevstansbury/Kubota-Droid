//
//  URLJsonAdapter.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.foundation.service

import com.squareup.moshi.*
import java.net.URL

class URLJsonAdapter {
    @FromJson
    fun fromJson(uri: String?): URL? {
        return uri?.let { URL(it) }
    }
    @ToJson
    fun toJson(value: URL?): String? {
        return value?.toString()
    }
}
