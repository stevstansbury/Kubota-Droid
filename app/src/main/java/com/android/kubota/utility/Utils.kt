package com.android.kubota.utility

import android.content.Context
import android.content.Intent
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.ui.SignUpActivity
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
                context.startActivity(Intent(context, SignUpActivity::class.java))
            }
            .create()
    }

    fun showLeavingAppDialog(context: Context, @StringRes messageResId: Int, intent: Intent): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.leave_app_dialog_title)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    @DrawableRes
    fun getModelImage(category: String, model: String): Int {
        return when (category) {
            "Construction" -> {
                when {
                    model.startsWith("R", true) -> R.drawable.ic_equipment_r_series
                    model.startsWith("SSV", true) -> R.drawable.ic_equipment_ssv_series
                    model.startsWith("SVL", true) -> R.drawable.ic_equipment_svl_series
                    model.startsWith("K", true) ||
                    model.startsWith("KX", true) ||
                    model.startsWith("U", true) -> R.drawable.ic_equipment_k_kx_u_series
                    else -> R.drawable.ic_construction_category_thumbnail
                }
            }
            "Mowers" -> R.drawable.ic_mower_category_thumbnail
            "Tractors" -> R.drawable.ic_tractor_category_thumbnail
            else -> R.drawable.ic_utv_category_thumbnail
        }
    }
}