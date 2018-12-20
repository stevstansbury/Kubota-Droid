package com.kubota.network.service

import com.kubota.network.model.GuidePage
import com.microsoft.azure.storage.ResultSegment
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.ListBlobItem
import retrofit2.Call
import java.net.URI

class GuideAPIService: GuideService {

    val guidesURL = "https://kubotaguides.blob.core.windows.net/"
    val guidesContainer = "selfmaintenance"
    val client = CloudBlobClient(URI(guidesURL))
    val container = client.getContainerReference(guidesContainer)

    var guidePages:ArrayList<ListBlobItem>? = null

    override fun getGuideList(model: String): Call<List<ListBlobItem>?> {
        var blobs = container.listBlobsSegmented()
        if (blobs.results.any() != null) {
            var guide: CloudBlobDirectory? = null
            for (res in blobs.results) {
                val dir = res as CloudBlobDirectory
                val prefix = dir.prefix.substringAfter('/').substringBefore('/')
                if (prefix == model) {
                    guide = dir
                    break
                }
            }
            if (guide != null && !guide.prefix.isNullOrEmpty()) {
                  guidePages = container.listBlobsSegmented(guide.prefix).results
            }
        }
        return guidePages
        //needs to use the Call Interface
    }

    override fun getGuidePages(guidePrefix: String): Call<Array<GuidePage>> {
        if (guidePages != null && guidePrefix.count() > 0) {
            for (res in guidePages) {
                val dir = res as CloudBlobDirectory
                val guideElements = container.listBlobsSegmented(dir.prefix).results.map { it as CloudBlockBlob }

                if (guideElements.count() > 0){
                    //TODO: extract all text MP3 and image paths and package in a list of GuidePages
                }

            }
        }
    }

}