package com.android.kubota.ui

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.android.kubota.R
import com.android.kubota.utility.Utils as Utility
import com.kubota.repository.utils.Utils

class WebViewFragment(): BaseWebViewFragment() {

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

    private var leaveAppDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view  = super.onCreateView(inflater, container, savedInstanceState)

        val bundle = arguments ?: Bundle.EMPTY
        val viewMode = bundle.getInt(VIEW_MODE, UNKNOWN_MODE)

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

        when (viewMode) {
            PRIVACY_POLICY_MODE -> {
                activity?.title = getString(R.string.privacy_policy)
                webView.loadUrl(Utils.getPrivacyPolicyUrl())
            }
            TERMS_OF_USE_MODE -> {
                activity?.title = getString(R.string.terms_of_use)
                webView.loadUrl(Utils.getTermsOfUseUrl())
            }
            else -> activity?.onBackPressed()
        }

        return view
    }

    override fun onPause() {
        super.onPause()

        leaveAppDialog?.dismiss()
        leaveAppDialog = null
    }

}