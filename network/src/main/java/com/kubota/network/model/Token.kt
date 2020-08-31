package com.kubota.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Token(
    val token: String
)