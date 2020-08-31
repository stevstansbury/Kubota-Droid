package com.android.kubota.ui

import android.annotation.TargetApi
import android.os.Build
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.kubota.R

abstract class BaseWebViewFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_webview

    protected lateinit var webView: WebView
    private lateinit var errorView: ErrorView
    private var showingError = false

    override fun initUi(view: View) {
        webView = view.findViewById(R.id.webView)
        errorView = view.findViewById(R.id.errorView)
    }

    abstract inner class WebViewListener : WebViewClient() {
        @Suppress("DEPRECATION")
        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            if (!showingError) {
                handleError(errorCode)
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (!showingError) {
                handleError(error?.errorCode)
            }
        }

        private fun handleError(errorCode: Int?) {
            showingError = true
            when (errorCode) {
                ERROR_CONNECT, ERROR_HOST_LOOKUP -> errorView.mode = ErrorView.Mode.WEB_FAIL
                else -> errorView.mode = ErrorView.Mode.GENERIC_ERROR
            }
            errorView.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
        }
    }
}
