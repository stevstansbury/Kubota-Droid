package com.android.kubota.ui

import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.android.kubota.R

open class BaseWebViewFragment(): BaseFragment() {
    protected lateinit var webView: WebView

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view  = inflater.inflate(R.layout.fragment_webview, null)
        webView = view.findViewById(R.id.webView)

        return view
    }
}