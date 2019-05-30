package com.kubota.repository.prefs

import com.kubota.network.service.GenericNetworkAPI
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.*
import java.lang.Exception
import java.net.URI

private val BLOB_CONTAINER = CloudBlobClient(URI("https://kubotaguides.blob.core.windows.net/"))
    .getContainerReference("selfmaintenance")

class GuidesRepo(private val modelName: String) {

    sealed class Response<T> {
        class Success<T>(val data: T) : Response<T>()
        class Failure<T>(val error: Exception) : Response<T>()
    }

    fun getGuideList(): Response<List<String>> {
        val list = ArrayList<String>()

        try {
            val blobs = BLOB_CONTAINER.listBlobsSegmented()
            val modelContainer =
                blobs.results.singleOrNull { it is CloudBlobDirectory && it.prefix.trim('/') == modelName }
            if (modelContainer != null) {
                val dir = modelContainer as CloudBlobDirectory
                val pages = BLOB_CONTAINER.listBlobsSegmented(dir.prefix).results
                pages.forEach {
                    if (it is CloudBlobDirectory) {
                        val newVal = it.prefix.replace("/", "").replace(modelName, "")
                        list.add(newVal)
                    }
                }
            }
        } catch (e: StorageException) {
            return Response.Failure(e)
        }

        return Response.Success(list)
    }

    fun getGuidePages(index: Int, guideList: List<String>): Response<List<GuidePage>?> {
        val list = ArrayList<GuidePage>()
        if (index >= guideList.size) { return Response.Success(null) }
        val guide = guideList[index]

        try {
            val listItems = BLOB_CONTAINER.listBlobsSegmented("$modelName/$guide/")

            for (result in listItems.results) {
                if (result is CloudBlobDirectory) {
                    //TODO: (JC) This is incredibly inefficient. Re-write algorithm also we are not handling possible exceptions
                    val elements = BLOB_CONTAINER.listBlobsSegmented(result.prefix).results.map { it as CloudBlockBlob }
                    val mp3Path = elements.firstOrNull { it.uri.toString().toUpperCase().contains("MP3") }?.uri.toString()
                    val textPath = elements.firstOrNull { it.uri.toString().toUpperCase().contains("TXT") }?.uri.toString()
                    val imagePath = elements.firstOrNull { it.uri.toString().toUpperCase().contains("JPG") }?.uri.toString()
                    val guidePage = GuidePage(mp3Path, textPath, imagePath)
                    list.add(guidePage)
                }
            }
        } catch (e: StorageException) {
            return Response.Failure(e)
        }

        return Response.Success(list)
    }

    fun getGuidePageWording(textPath: String): String? = GenericNetworkAPI().request(textPath)
}
