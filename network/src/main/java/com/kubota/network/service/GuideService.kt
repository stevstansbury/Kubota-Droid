package com.kubota.network.service

import com.kubota.network.model.GuidePage
import com.microsoft.azure.android
import com.microsoft.azure.storage.ResultSegment
import com.microsoft.azure.storage.blob.BlobInputStream
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import retrofit2.Call


interface GuideService {
    fun getGuideList(model: String): Call<ResultSegment<String>>
    fun getGuidePages(model: String, guideName: String): Call<Array<GuidePage>>
    fun getGuidePages(guidePrefix: String): Call<Array<GuidePage>>
}