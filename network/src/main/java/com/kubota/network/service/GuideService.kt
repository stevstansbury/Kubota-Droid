package com.kubota.network.service

import com.microsoft.azure.android
import com.microsoft.azure.storage.ResultSegment
import com.microsoft.azure.storage.blob.BlobInputStream
import retrofit2.Call


interface GuideService {

    fun getGuideList(model: String): Call<ResultSegment<String>>
    

}