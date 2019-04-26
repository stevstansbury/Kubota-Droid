package com.kubota.repository.prefs

import com.kubota.network.service.GenericNetworkAPI
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import java.net.URI

private val BLOB_CONTAINER = CloudBlobClient(URI("https://kubotaguides.blob.core.windows.net/"))
    .getContainerReference("selfmaintenance")

class GuidesRepo(private val modelName: String) {

    fun getGuideList(): List<String> {
        val list = ArrayList<String>()
        val blobs = BLOB_CONTAINER.listBlobsSegmented()
        val modelContainer = blobs.results.singleOrNull { it is CloudBlobDirectory && it.prefix.trim('/') == modelName }
        if (modelContainer != null) {
            val dir = modelContainer as CloudBlobDirectory
            val pages = BLOB_CONTAINER.listBlobsSegmented(dir.prefix).results
            pages.forEach {
                if (it is CloudBlobDirectory){
                    val newVal = it.prefix.replace("/", "").replace(modelName, "")
                    list.add(newVal)
                }
            }
        }

        return list
    }

    fun getGuidePages(index: Int, guideList: List<String>): List<GuidePage>? {
        val list = ArrayList<GuidePage>()
        if (index >= guideList.size) { return null }
        val guide = guideList[index]
        val listItems = BLOB_CONTAINER.listBlobsSegmented("$modelName/$guide/")

        for (result in listItems.results) {
            if (result is CloudBlobDirectory){
                val elements = BLOB_CONTAINER.listBlobsSegmented(result.prefix).results.map { it as CloudBlockBlob }
                val mp3Path = elements.single { it.uri.toString().toUpperCase().contains("MP3") }.uri.toString()
                val textPath = elements.single { it.uri.toString().toUpperCase().contains("TXT")}.uri.toString()
                val imagePath = elements.single { it.uri.toString().toUpperCase().contains("JPG")}.uri.toString()
                val guidePage = GuidePage(mp3Path, textPath, imagePath)
                list.add(guidePage)
            }
        }

        return list
    }

    fun getGuidePageWording(textPath: String): String? = GenericNetworkAPI().request(textPath)
}