package com.android.kubota.ui

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.kubota.R

open class BaseWebViewFragment : BaseFragment() {
    protected lateinit var webView: WebView
    private lateinit var errorView: ErrorView
    private var showingError = false

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_webview, null)
        webView = view.findViewById(R.id.webView)
        errorView = view.findViewById(R.id.errorView)

        return view
    }

    abstract inner class WebViewListener : WebViewClient() {
        // ApiLevel < 23
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
