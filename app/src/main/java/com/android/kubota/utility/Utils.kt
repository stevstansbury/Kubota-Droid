package com.android.kubota.utility

import android.content.Context
import android.content.Intent
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.ui.SignUpActivity


object Utils {

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
}