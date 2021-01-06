package com.android.kubota.utility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics

object Utils {

    private const val BASE_URL = "https://ktcictstorage.blob.core.windows.net/legal"
    private const val TERMS_OF_USE_PATH = "TermsOfUse.html"
    private const val PRIVACY_POLICY_PATH = "PrivacyPolicyDraft.html"
    private const val CALIFORNIA_POLICY_PATH = "CaliforniaPrivacyRights.html"

    fun getTermsOfUseUrl() = "$BASE_URL/$TERMS_OF_USE_PATH"

    fun getPrivacyPolicyUrl() = "$BASE_URL/$PRIVACY_POLICY_PATH"

    fun getCaliforniaPolicyUrl() = "$BASE_URL/$CALIFORNIA_POLICY_PATH"

    enum class LogInDialogMode(@StringRes val messageResId: Int) {
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

        if (!hasInstalledApp) {
            val activityNotFoundException = ActivityNotFoundException(
                "No Activity found to handle Intent {${Uri.decode(intent.dataString ?: "")}}"
            )
            FirebaseCrashlytics
                .getInstance()
                .recordException(activityNotFoundException)
        }

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
}
