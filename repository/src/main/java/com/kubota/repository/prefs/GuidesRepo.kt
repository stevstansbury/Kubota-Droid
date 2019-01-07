package com.kubota.repository.prefs

import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import java.net.URI

class GuidesRepo(model: String) {

    private val guidesURL = "https://kubotaguides.blob.core.windows.net/"
    private val guidesContainer = "selfmaintenance"
    private val client = CloudBlobClient(URI(guidesURL))
    private val container = client.getContainerReference(guidesContainer)

    var guideList:ArrayList<String>? = null
    val modelName: String = model

    fun getGuideList(): List<String>? {
        val list = ArrayList<String>()
        val blobs = container.listBlobsSegmented()
        val modelContainer = blobs.results.singleOrNull { it is CloudBlobDirectory && it.prefix.trim('/') == modelName }
        if (modelContainer != null) {
            val dir = modelContainer as CloudBlobDirectory
            val pages = container.listBlobsSegmented(dir.prefix).results
            pages?.forEach {
                if (it is CloudBlobDirectory){
                    list.add(it.prefix)
                }
            }
        }
        guideList = list
        return list
    }

    fun getGuidePages(index: Int): List<GuidePage>? {
        val list = ArrayList<GuidePage>()
        val guides = guideList
        if (guides == null || index >= guides.size) { return null }
        val guide = guides[index]
        val listItems = container.listBlobsSegmented(guide)
        for (result in listItems.results) {
            if (result is CloudBlobDirectory){
                val elements = container.listBlobsSegmented(result.prefix).results.map { it as CloudBlockBlob }
                val mp3Path = elements.single { it.uri.toString().toUpperCase().contains("MP3") }.uri.toString()
                val textPath = elements.single { it.uri.toString().toUpperCase().contains("TXT")}.uri.toString()
                val imagePath = elements.single { it.uri.toString().toUpperCase().contains("JPG")}.uri.toString()
                val guidePage = GuidePage(mp3Path, textPath, imagePath)
                list.add(guidePage)
            }
        }
        return list
    }

}