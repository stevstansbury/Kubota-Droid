//
//  ManualInfo.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.net.URI

data class ManualInfo(
    val title: String,
    val url: URI
)

private fun URI.deletePathExtension(): URI {
    val absoluteStr = this.toString()
    val dot = absoluteStr.lastIndexOf('.')
    val withoutExt = if (dot > 0) absoluteStr.substring(startIndex = 0, endIndex = dot) else absoluteStr
    return URI(withoutExt)
}

private val URI.pathComponents: List<String>
    get() {
        return this.path.split(delimiters = *charArrayOf('/')).filter { it.isNotEmpty() }
    }

val URI.manualInfo: ManualInfo
    get() {
        //
        // Example urls:
        //    https://mykubota.azurewebsites.net/PDFs/TC650-19717END.pdf
        //    https://mykubota.azurewebsites.net/PDFs/MX5400_MX6000_201911.pdf
        //

        // Remove the extension
        val url = this.deletePathExtension()
        val lastPath = url.pathComponents.lastOrNull() ?: return ManualInfo(title = "", url = this)

        // Replace last component, ie, MX5400_MX6000_201911, chars "_" with "/"
        // to produce "MX5400/MX6000/201911" for the manual name.
        return ManualInfo(title = lastPath.replace(oldChar = '_', newChar = '/', ignoreCase = true), url = this)
}
