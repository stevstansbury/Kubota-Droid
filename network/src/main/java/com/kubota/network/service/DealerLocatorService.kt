package com.kubota.network.service

import com.kubota.network.model.Dealer
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DealerLocatorService {

    @GET("/api/dealer/nearest")
    fun getDealers(@Query("latitude")latitude: Double = 32.9792895,
                   @Query("longitude")longitude: Double = -97.0315917,
                   @Query("model") model: String = "",
                   @Query("distance") distance: Int = 300): Call<List<Dealer>>
}