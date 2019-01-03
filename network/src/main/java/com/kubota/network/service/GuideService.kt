package com.kubota.network.service

import com.kubota.network.model.GuidePage

interface GuideService {
    fun getGuideList(): List<String>?
    fun getGuidePages(index: Int): List<GuidePage>?
}