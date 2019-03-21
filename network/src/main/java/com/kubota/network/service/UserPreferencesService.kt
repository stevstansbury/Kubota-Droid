package com.kubota.network.service

import com.kubota.network.model.Dealer
import com.kubota.network.model.Model
import com.kubota.network.model.UserPreference
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface UserPreferencesService {

    @POST("/api/preferences")
    fun updatePreferences(@Header("Authorization") accessToken: String, @Body preference: UserPreference): Call<Any>

    @GET("/api/preferences")
    fun getPreferences(@Header("Authorization") accessToken: String): Call<Any>

    @POST("/api/preferences/add")
    fun addModel(@Header("Authorization") accessToken: String, model: Model): Call<Any>

    @POST("/api/preferences/edit")
    fun editModel(@Header("Authorization") accessToken: String, model: Model): Call<Any>

    @POST("/api/preferences/delete")
    fun deleteModel(@Header("Authorization") accessToken: String, model: Model): Call<Any>

    @POST("/api/preferences/add")
    fun addDealer(@Header("Authorization") accessToken: String, dealer: Dealer): Call<Any>

    @POST("/api/preferences/delete")
    fun deleteDealer(@Header("Authorization") accessToken: String, dealer: Dealer): Call<Any>

}