package com.android.kubota.utility

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
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

    private val requests = mutableMapOf<Int, (Array<String>, IntArray) -> Unit >()

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        requests.remove(requestCode)?.invoke(permissions, grantResults)
    }

    private fun callback(permissions: Array<String>, results: IntArray, resolver: Resolver<Unit>) {
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            resolver.fulfill(Unit)
        else
            resolver.reject(SecurityException())
    }

    private fun _requestPermission(activity: FragmentActivity, permission: String, message: Int, recursion: Int): Promise<Unit> {
        return when {
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED -> Promise.value(Unit)

            // if we loop this too many times in a row, don't keep asking
            recursion > 0 -> Promise(error=SecurityException())

            // Permission is not granted
            // Should we show an explanation?
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                MessageDialogFragment
                    .showCancelMessage(manager=activity.supportFragmentManager, titleId=R.string.title_permission, messageId=message)
                    .thenMap { idx ->
                        when (idx) {
                            DialogInterface.BUTTON_POSITIVE -> _requestPermission(activity, permission, message, recursion+1)
                            else -> Promise(error=SecurityException())
                        }
                    }
            }

            // request the permisisons now
            else -> {
                val pending = Promise.pending<Unit>()
                val code = Random.nextInt(0, 65535) // must be the lower 16 bits
                requests[code] = { permissions, results -> callback(permissions, results, pending.second) }
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity, arrayOf(permission), code)
                pending.first
            }
        }
    }

    fun requestPermission(activity: FragmentActivity, permission: String, message: Int): Promise<Unit> =
        _requestPermission(activity, permission, message, 0)
}