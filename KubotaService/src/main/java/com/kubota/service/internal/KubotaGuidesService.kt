package com.kubota.service.internal

import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.GuidesService
import com.kubota.service.domain.GuidePage
import com.microsoft.azure.storage.blob.*
import java.net.URI
import java.net.URL

internal class KubotaGuidesService: GuidesService {
    private val blobContainer = CloudBlobClient(URI("https://kubotaguides.blob.core.windows.net/"))
                                        .getContainerReference("selfmaintenance")

    override fun getGuideList(model: String): Promise<List<String>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            val list = ArrayList<String>()
            val blobs = this.blobContainer.listBlobsSegmented()
            val modelContainer = blobs.results.singleOrNull {
                it is CloudBlobDirectory && it.prefix.trim('/') == model
            }
            modelContainer?.let {
                val dir = modelContainer as CloudBlobDirectory
                val pages = this.blobContainer.listBlobsSegmented(dir.prefix).results
                pages.forEach {
                    if (it is CloudBlobDirectory) {
                        val newVal = it.prefix.replace("/", "").replace(model, "")
                        list.add(newVal)
                    }
                }
            }
            list
        }
    }

    override fun getGuide(model: String, guideName: String): Promise<List<GuidePage>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            val list = ArrayList<GuidePage>()
            val listItems = this.blobContainer.listBlobsSegmented("$model/$guideName/")
            listItems?.results?.forEach { result ->
                when (result) {
                    is CloudBlobDirectory -> {
                        val elements =
                            this.blobContainer.listBlobsSegmented(result.prefix).results.map { it as CloudBlockBlob }
                        val mp3Path = elements.firstOrNull { it.uri.toString().toUpperCase().contains("MP3") }?.uri.toString()
                        val textPath = elements.firstOrNull { it.uri.toString().toUpperCase().contains("TXT") }?.uri.toString()
                        val imagePath = elements.firstOrNull { it.uri.toString().toUpperCase().contains("JPG") }?.uri.toString()
                        val guidePage = GuidePage(mp3Path, textPath, imagePath)
                        list.add(guidePage)
                    }
                }
            }
            list
        }
    }

    override fun getGuidePageWording(url: URL): Promise<String?> {
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.immortal.interval)
        val p = HTTPService(config = HTTPService.Config(baseUrl = url)).get(route = "", cacheCriteria = criteria)
        return p.map { result -> result?.body?.let { String(bytes = it) } }
    }

}
