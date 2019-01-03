package com.kubota.network.service

import com.kubota.network.model.GuidePage
import com.microsoft.azure.storage.ResultSegment
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.ListBlobItem
import java.net.URI

class GuideAPIService(model: String): GuideService {

    private val guidesURL = "https://kubotaguides.blob.core.windows.net/"
    private val guidesContainer = "selfmaintenance"
    private val client = CloudBlobClient(URI(guidesURL))
    private val container = client.getContainerReference(guidesContainer)

    var guidePages:ArrayList<ListBlobItem>? = null
    val modelName: String = model

    override fun getGuideList(): List<String>? {
        val list = ArrayList<String>()
        val blobs = container.listBlobsSegmented()
        blobs.results.forEach {
            val dir = it as CloudBlobDirectory
            val prefix = dir.prefix.trim('/')
            list.add(prefix)
            if (prefix == modelName) {
                guidePages = container.listBlobsSegmented(dir.prefix).results
                val pages = guidePages
                pages?.forEach {
                    if (it is CloudBlobDirectory){
                        list.add(it.prefix)
                    }
                }
            }
        }
        return list
    }

    override fun getGuidePages(index: Int): List<GuidePage>? {

        val list = ArrayList<GuidePage>()
        return list
    }

}