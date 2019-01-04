package com.kubota.network.service

import com.kubota.network.model.ManualMapping
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ManualService {

    @GET("/api/Manuals/{model}")
    fun getManualMapping(@Path("model") modelName: String): Call<ManualMapping>

}