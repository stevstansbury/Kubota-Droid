package com.android.kubota.utility

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.android.kubota.R
import com.inmotionsoftware.promisekt.*
import java.lang.IllegalArgumentException
import kotlin.random.Random


object PermissionRequestManager {

    private val requests = mutableMapOf<Int, (IntArray) -> Unit >()

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        requests.remove(requestCode)?.let { it(grantResults) }
    }

    private fun callback(permissions: IntArray, resolver: Resolver<Unit>) {
        if (permissions.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            resolver.fulfill(Unit)
        else
            resolver.reject(SecurityException())
    }

    private fun _requestPermission(activity: FragmentActivity, permission: String, message: Int, recursion: Int): Promise<Unit> {
        return when (ContextCompat.checkSelfPermission(activity, permission)) {
            PackageManager.PERMISSION_GRANTED -> Promise.value(Unit)
            PackageManager.PERMISSION_DENIED -> {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                    if (recursion > 0) {
                        Promise(error=SecurityException())
                    } else {
                        MessageDialogFragment
                            .showSimpleMessage(manager=activity.supportFragmentManager, titleId = R.string.title_permission, messageId = message, onButtonAction = null)
                            .thenMap {
                                _requestPermission(activity, permission, message, recursion+1)
                            }
                    }
                } else {
                    val pending = Promise.pending<Unit>()
                    val code = Random.nextInt(0, 65535) // must be the lower 16 bits
                    requests[code] = { callback(it, pending.second) }
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity, arrayOf(permission), code)
                    pending.first
                }
            }
            else -> Promise(error= IllegalArgumentException())
        }
    }

    fun requestPermission(activity: FragmentActivity, permission: String, message: Int): Promise<Unit> =
        _requestPermission(activity, permission, message, 0)
}