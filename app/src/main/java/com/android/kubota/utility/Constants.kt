package com.android.kubota.utility

import com.crashlytics.android.Crashlytics

object Constants {
    const val FORGOT_PASSWORD_EXCEPTION = "AADB2C90118"

    private const val VIEW_MODE_KEY = "ViewMode"
    const val VIEW_MODE_EQUIPMENT = "Equipment"
    const val VIEW_MODE_MY_DEALERS = "MyDealers"
    const val VIEW_MODE_DEALER_LOCATOR = "DealerLocator"
    const val VIEW_MODE_PROFILE = "Profile"
    const val VIEW_MODE_MAINTENANCE_GUIDE = "MaintenanceGuide"
    const val VIEW_MODE_EQUIPMENT_SEARCH = "EquipmentSearch"
    const val VIEW_MODE_DEALERS_SEARCH = "DealersSearch"

    object Analytics {
        fun setViewMode(viewMode: String) = Crashlytics.setString(VIEW_MODE_KEY, viewMode)
    }
}