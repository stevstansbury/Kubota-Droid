package com.android.kubota.ui

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.extensions.getEnum
import com.android.kubota.extensions.putEnum
import com.android.kubota.utility.Utils as Utility

class UrlContext(val url: String, val title: Int)

enum class LegalMode {
    UNKNOWN_MODE,
    PRIVACY_POLICY_MODE,
    TERMS_OF_USE_MODE,
    CALIFORNIA_MODE
}

val LegalMode.context: UrlContext get() =
    when (this) {
        LegalMode.PRIVACY_POLICY_MODE -> UrlContext(Utility.getPrivacyPolicyUrl(),R.string.privacy_policy)
        LegalMode.TERMS_OF_USE_MODE -> UrlContext(Utility.getTermsOfUseUrl(), R.string.terms_of_use)
        LegalMode.CALIFORNIA_MODE -> UrlContext(Utility.getCaliforniaPolicyUrl(), R.string.california_policy)
        else -> UrlContext(Utility.getPrivacyPolicyUrl(), R.string.privacy_policy)
    }

class WebViewFragment : BaseWebViewFragment() {
    companion object {
        fun createInstance(mode: LegalMode): WebViewFragment {
            val fragment = WebViewFragment()
            val arguments = Bundle(1)
            arguments.putEnum(mode)
            fragment.arguments = arguments

            return fragment
        }
    }

    private var viewMode = LegalMode.UNKNOWN_MODE
    private var leaveAppDialog: AlertDialog? = null

    override fun hasRequiredArgumentData(): Boolean {
        viewMode = arguments?.getEnum<LegalMode>() ?: LegalMode.UNKNOWN_MODE
        return viewMode != LegalMode.UNKNOWN_MODE
    }

    override fun initUi(view: View) {
        super.initUi(view)

        webView.webViewClient = object : WebViewListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                flowActivity?.hideProgressBar()
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let {
                    showLeaveAppDialog(uri = it)

                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    showLeaveAppDialog(uri = Uri.parse(it))

                    return true
                }

                return super.shouldOverrideUrlLoading(view, url)
            }

            private fun showLeaveAppDialog(uri: Uri) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.data = uri

                leaveAppDialog = Utility.showLeavingAppDialog(requireContext(), R.string.leave_app_generic_msg, intent)
                leaveAppDialog?.show()
            }
        }

        flowActivity?.showProgressBar()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            when (viewMode) {
                LegalMode.PRIVACY_POLICY_MODE -> activity?.setTitle(R.string.privacy_policy)
                LegalMode.TERMS_OF_USE_MODE -> activity?.setTitle(R.string.terms_of_use)
                LegalMode.CALIFORNIA_MODE -> activity?.setTitle(R.string.california_policy)
                else -> activity?.onBackPressed()
            }
        }
    }

    override fun loadData() {
        when (viewMode) {
            LegalMode.PRIVACY_POLICY_MODE -> {
                activity?.setTitle(R.string.privacy_policy)
                webView.loadUrl(Utility.getPrivacyPolicyUrl())
            }
            LegalMode.TERMS_OF_USE_MODE -> {
                activity?.setTitle(R.string.terms_of_use)
                webView.loadUrl(Utility.getTermsOfUseUrl())
            }
            LegalMode.CALIFORNIA_MODE -> {
                activity?.setTitle(R.string.california_policy)
                webView.loadUrl(Utility.getCaliforniaPolicyUrl())
            }
            else -> activity?.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()

        leaveAppDialog?.dismiss()
        leaveAppDialog = null
    }

}