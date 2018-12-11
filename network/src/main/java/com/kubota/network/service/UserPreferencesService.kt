package com.kubota.network.service

import com.kubota.network.model.UserPreference
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface UserPreferencesService {

    @POST("/api/preferences")
    fun updatePreferences(@Header("X-API-Key") accessToken: String, @Body preference: UserPreference): Call<UserPreference>

    @GET("/api/preferences")
    fun getPreferences(@Header("X-API-Key") accessToken: String): Call<UserPreference>
}