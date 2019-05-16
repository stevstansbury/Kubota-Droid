package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.kubota.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view  = super.onCreateView(inflater, container, savedInstanceState)

        val bundle = arguments ?: Bundle.EMPTY
        val viewMode = bundle.getInt(VIEW_MODE, UNKNOWN_MODE)

        webView.webViewClient = object : WebViewListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                flowActivity?.hideProgressBar()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                    intent.data = it
                    requireContext().startActivity(intent)

                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
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

}