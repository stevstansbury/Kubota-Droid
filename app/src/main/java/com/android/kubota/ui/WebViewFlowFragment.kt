package com.android.kubota.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import com.android.kubota.R
import com.android.kubota.utility.Utils
import com.inmotionsoftware.flowkit.android.FlowFragment

open class WebViewFlowFragment: FlowFragment<UrlContext, Unit>() {
    protected lateinit var webView: WebView
    private lateinit var errorView: ErrorView
    private var showingError = false
    private var leaveAppDialog: AlertDialog? = null
    protected var flowActivity: FlowActivity? = null

    private val context =
        MutableLiveData<UrlContext>()

    override fun onInputAttached(input: UrlContext) {
        context.value = input
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_webview, null)
        errorView = view.findViewById(R.id.errorView)

        webView = view.findViewById(R.id.webView)
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
                val intent =
                    Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.data = uri

                leaveAppDialog =
                    Utils.showLeavingAppDialog(
                        requireContext(),
                        R.string.leave_app_generic_msg,
                        intent
                    )
                leaveAppDialog?.show()
            }
        }

        context.observe(viewLifecycleOwner, Observer { input ->
            activity?.setTitle(input.title)
            webView.loadUrl(input.url)
        })

        flowActivity?.showProgressBar()
        return view
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
                ERROR_CONNECT, ERROR_HOST_LOOKUP -> errorView.mode =
                    ErrorView.Mode.WEB_FAIL
                else -> errorView.mode =
                    ErrorView.Mode.GENERIC_ERROR
            }
            errorView.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
        }
    }
}