package com.android.kubota.utility

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.crashlytics.android.Crashlytics
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


object Utils {
    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)
    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)

    fun backgroundTask(block: suspend () -> Unit): Job {
        return backgroundScope.launch {
            block()
        }
    }

    fun uiTask(block: () -> Unit): Job {
        return uiScope.launch {
            block()
        }
    }

    enum class LogInDialogMode(@StringRes val messageResId: Int) {
        EQUIPMENT_MESSAGE(R.string.sign_in_modal_equipment_message),
        DEALER_MESSAGE(R.string.sign_in_modal_dealer_message)
    }

    fun createMustLogInDialog(context: Context, mode: LogInDialogMode): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.sign_in_modal_title)
            .setMessage(mode.messageResId)
            .setNegativeButton(android.R.string.cancel) {dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AccountSetupActivity.startActivityForSignIn(context)
            }
            .create()
    }

    fun showLeavingAppDialog(context: Context, @StringRes messageResId: Int, intent: Intent): AlertDialog {
        val hasInstalledApp = intent.resolveActivity(context.packageManager) != null

        if (!hasInstalledApp) Crashlytics.logException(ActivityNotFoundException("No Activity found to handle Intent {${Uri.decode(intent.dataString ?: "")}}"))

        return AlertDialog.Builder(context)
            .setTitle(if (hasInstalledApp) R.string.leave_app_dialog_title else R.string.no_app_found_error_title)
            .setMessage(if (hasInstalledApp) messageResId else R.string.no_app_found_error_msg)
            .setPositiveButton(if (hasInstalledApp) android.R.string.ok else R.string.no_app_found_error_positive_button_text) { _, _ ->
                val targetIntent = if (hasInstalledApp) intent else Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store")
                    setPackage("com.android.vending")
                }

                context.startActivity(targetIntent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    fun hideBottomNavigation(activity: Activity?) {
        activity?.findViewById<BottomNavigationView>(R.id.navigation)?.let { it.visibility = View.GONE }
    }

    fun showBottomNavigation(activity: Activity?) {
        activity?.findViewById<BottomNavigationView>(R.id.navigation)?.let { it.visibility = View.VISIBLE }
    }
}
