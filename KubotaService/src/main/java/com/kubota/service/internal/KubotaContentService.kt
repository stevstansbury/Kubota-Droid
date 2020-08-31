package com.kubota.service.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.ContentService
import java.net.URL

internal class KubotaContentService(config: Config): HTTPService(config = config), ContentService {

    override fun getContent(url: URL): Promise<ByteArray?> {
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.immortal.interval)
        val p = this.get(route = url.toString(), cacheCriteria = criteria)
        return p.map { result -> result?.body }
    }

    override fun getBitmap(url: URL): Promise<Bitmap?> {
        return this.getContent(url = url)
                    .map(on = DispatchExecutor.global) {
                        val data = it ?: return@map null
                        BitmapFactory.decodeByteArray(data, 0, data.size)
                    }
    }

}
