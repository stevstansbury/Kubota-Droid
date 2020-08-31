//
//  GuidesService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.GuidePage
import java.net.URL

interface GuidesService {

    fun getGuideList(model: String): Promise<List<String>>

    fun getGuide(model: String, guideName: String): Promise<List<GuidePage>>

    fun getGuidePageWording(url: URL): Promise<String?>

}
