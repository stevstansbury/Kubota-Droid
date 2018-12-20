package com.kubota.network.service

import com.kubota.network.model.GuidePage
import com.microsoft.azure.storage.blob.ListBlobItem
import retrofit2.Call

interface GuideService {
    fun getGuideList(model: String): Call<List<ListBlobItem>?>
    fun getGuidePages(guidePrefix: String): Call<List<GuidePage>>
}