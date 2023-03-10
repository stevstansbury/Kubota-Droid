package com.kubota.service.api

import android.graphics.Bitmap
import com.inmotionsoftware.promisekt.Promise
import java.net.URL

interface ContentService {

    fun getContent(url: URL): Promise<ByteArray?>

    fun getBitmap(url: URL): Promise<Bitmap?>

}
