package com.android.kubota.ui.notification

import android.app.Activity
import android.graphics.drawable.LayerDrawable
import android.view.Menu
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.KubotaBadgeDrawables

class NotificationMenuController(private val activity: Activity) {

    private val badeDrawable: KubotaBadgeDrawables = KubotaBadgeDrawables(
        context = activity,
        sizeDimeResId = R.dimen.small_notification_unread_counter_size
    )
    private val notificationMenuIcon: LayerDrawable

    val unreadNotificationsObserver: Observer<Int> = Observer {
        badeDrawable.unreadCounter = it
        activity.invalidateOptionsMenu()
    }

    init {
        val leftOffSet = activity.resources.getDimensionPixelSize(R.dimen.notification_bell_size) -
                activity.resources.getDimensionPixelSize(R.dimen.small_notification_unread_counter_size)
        val notificationBell = ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_notification_bell, null)!!

        val layerDrawable = LayerDrawable(arrayOf(notificationBell, badeDrawable))
        layerDrawable.setLayerInset(0, -leftOffSet, 0, leftOffSet, 0)
        notificationMenuIcon = layerDrawable
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.notifications)?.let {
            it.isVisible = AppProxy.proxy.accountManager.isAuthenticated.value ?: false
            it.icon = notificationMenuIcon
        }
    }
}