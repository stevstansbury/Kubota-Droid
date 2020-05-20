package com.android.kubota.utility

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.kubota.R
import com.android.kubota.extensions.showDialog
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.fulfill
import com.inmotionsoftware.promisekt.reject
import com.inmotionsoftware.promisekt.thenMap
import java.lang.IllegalArgumentException
import kotlin.random.Random


object PermissionRequestManager {
    private val requests = mutableMapOf<Int, (IntArray) -> Unit >()

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        requests.remove(requestCode)?.let { it(grantResults) }
    }

    fun requestPermission(activity: Activity, permission: String): Promise<Unit> {
        return when (ContextCompat.checkSelfPermission(activity, permission)) {
            PackageManager.PERMISSION_GRANTED -> Promise.value(Unit)
            PackageManager.PERMISSION_DENIED -> {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    activity
                        .showDialog(activity.getString(R.string.accept_phone_permission))
                        .thenMap { requestPermission(activity, permission) }

                } else {
                    val pending = Promise.pending<Unit>()
                    val code = Random.nextInt(0, 65536)
                    requests[code] = {
                        val status = it.first() ?: PackageManager.PERMISSION_DENIED
                        when (status) {
                            PackageManager.PERMISSION_GRANTED -> pending.second.fulfill(Unit)
                            else -> pending.second.reject(SecurityException())
                        }
                    }
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), code)
                    pending.first
                }
            }
            else -> Promise(error= IllegalArgumentException())
        }
    }
}