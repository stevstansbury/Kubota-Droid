package com.kubota.service.internal

import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
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

}
