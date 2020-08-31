package com.android.kubota.utility

import com.crashlytics.android.Crashlytics

object Constants {
    private const val VIEW_MODE_KEY = "ViewMode"
    const val VIEW_MODE_EQUIPMENT = "Equipment"
    const val VIEW_MODE_MY_DEALERS = "MyDealers"
    const val VIEW_MODE_RESOURCES = "Resources"
    const val VIEW_MODE_PROFILE = "Profile"
    const val VIEW_MODE_MAINTENANCE_GUIDE = "MaintenanceGuide"
    const val VIEW_MODE_DEALERS_SEARCH = "DealersSearch"

    const val DEFAULT_MAP_LATITUDE = 32.9792895
    const val DEFAULT_MAP_LONGITUDE = -97.0315917
    const val DEFAULT_MAP_ZOOM = 8f

    object Analytics {
        fun setViewMode(viewMode: String) = Crashlytics.setString(VIEW_MODE_KEY, viewMode)
    }
}