package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import java.net.URL

interface ContentService {

    fun getContent(url: URL): Promise<ByteArray?>

}
