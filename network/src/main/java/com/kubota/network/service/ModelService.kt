package com.kubota.network.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ModelService {

    @GET("/api/KubotaModels/categories")
    fun getCategories(): Call<List<String>>

    @GET("/api/KubotaModels")
    fun getModels(@Query("category") category: String): Call<List<String>>

}