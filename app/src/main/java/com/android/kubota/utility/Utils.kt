package com.android.kubota.utility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.ftue.AccountSetupActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.*

object Utils {

    private const val BASE_URL = "https://ktcictstorage.blob.core.windows.net/\$web/legal"

    fun getTermsOfUseUrl() = "$BASE_URL/" + when (AppProxy.proxy.currentLocale) {
        Locale.CANADA_FRENCH -> "TermsOfUse_fr-CA.html"
        Locale.CANADA -> "TermsOfUse_en-CA.html"
        else -> "TermsOfUse_en-US.html"
    }

    fun getPrivacyPolicyUrl() = "$BASE_URL/" + when (AppProxy.proxy.currentLocale) {
        Locale.CANADA_FRENCH -> "PrivacyPolicy_fr-CA.html"
        Locale.CANADA -> "PrivacyPolicy_en-CA.html"
        else -> "PrivacyPolicy_en-US.html"
    }

    fun getCaliforniaPolicyUrl() = "$BASE_URL/CCPA_en-US.html"

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

    fun regionalKubotaWebsite(): Uri {
        return when (AppProxy.proxy.currentLocale) {
            Locale.CANADA_FRENCH -> "https://kubota.ca/fr/home"
            Locale.CANADA -> "https://kubota.ca/en/home"
            else -> "https://www.kubotausa.com"
        }.let(Uri::parse)
    }
}
