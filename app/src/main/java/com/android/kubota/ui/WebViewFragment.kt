package com.android.kubota.ui

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.android.kubota.R
import com.android.kubota.utility.Utils as Utility

class WebViewFragment : BaseWebViewFragment() {

    companion object {
        private const val VIEW_MODE = "view_mode"

        private const val UNKNOWN_MODE = 0
        const val PRIVACY_POLICY_MODE = 1
        const val TERMS_OF_USE_MODE = 2

        fun createInstance(mode: Int): WebViewFragment {
            val fragment = WebViewFragment()
            val arguments = Bundle(1)
            arguments.putInt(VIEW_MODE, mode)
            fragment.arguments = arguments

            return fragment
        }
    }

    private var viewMode = UNKNOWN_MODE
    private var leaveAppDialog: AlertDialog? = null

    override fun hasRequiredArgumentData(): Boolean {
        viewMode = arguments?.getInt(VIEW_MODE) ?: UNKNOWN_MODE
        return viewMode != UNKNOWN_MODE
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

    override fun loadData() {
        when (viewMode) {
            PRIVACY_POLICY_MODE -> {
                activity?.setTitle(R.string.privacy_policy)
                // FIXME:
//                webView.loadUrl(Utils.getPrivacyPolicyUrl())
            }
            TERMS_OF_USE_MODE -> {
                activity?.setTitle(R.string.terms_of_use)
                // FIXME:
//                webView.loadUrl(Utils.getTermsOfUseUrl())
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