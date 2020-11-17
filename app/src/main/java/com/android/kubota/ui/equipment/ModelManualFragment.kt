package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.ui.BaseWebViewFragment
import com.kubota.service.domain.ManualInfo
import java.net.URI

private fun URI.hasPDF(): Boolean {
    return this.path?.endsWith("pdf", ignoreCase = true) ?: false
}

private fun URI.googleDrivePDF(): Uri =
    Uri.parse("https://docs.google.com/viewer?url=${this}")


class ModelManualFragment: BaseWebViewFragment() {
    class ManualUrlViewModel: ViewModel() {
        var manual = MutableLiveData<ManualInfo>()
    }

    private val viewModel by lazy {
        ViewModelProvider(this.requireActivity()).get(ManualUrlViewModel::class.java)
    }

    companion object {
        private const val KEY_MANUAL = "KEY_MANUAL"

        fun createInstance(model: ManualInfo): ModelManualFragment {
            return ModelManualFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable(KEY_MANUAL, model)
                }
            }
        }
    }

    override fun hasRequiredArgumentData(): Boolean {
        val model: ManualInfo? = arguments?.getParcelable(KEY_MANUAL)
        viewModel.manual.value = model
        return model != null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            val title = this.viewModel.manual.value?.title

            if (title != null && title.isNotBlank()) {
                activity?.title = (getString(R.string.manual_title, title))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val webView: WebView? = this.webView
        webView?.destroy() // make sure we cleanup...
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initUi(view: View) {
        super.initUi(view)

        val model = this.viewModel.manual.value
        val name = model?.title

        if (name != null && name.isNotBlank()) {
            activity?.title = (getString(R.string.manual_title, name))
        }

        webView.webViewClient = object : WebViewListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideProgressBar()
            }

            @SuppressWarnings("deprecation")
            @Override
            override fun shouldOverrideUrlLoading(webView: WebView?, url: String?): Boolean {
                return shouldOverrideUrlLoading(webView, url)
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url
                return shouldOverrideUrlLoading(view, if (url != null) URI.create(url.toString()) else null)
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                failingUrl?.let {
                    val uri = URI.create(failingUrl)
                    if (uri.hasPDF()) { handlePDF(uri) }
                }
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                request?.let {
                    val uri = URI.create(it.url.toString())
                    if (uri.hasPDF()) { handlePDF(uri) }
                }
                super.onReceivedError(view, request, error)
            }

            fun shouldOverrideUrlLoading(view: WebView?, uri: URI?): Boolean {
                if (view != null && uri != null && uri.hasPDF()) {
                    view.loadUrl(uri.googleDrivePDF().toString())
                    return true
                }
                return false
            }

            fun handlePDF(url: URI) {
                if (activity == null) return

                val uri = Uri.parse(url.toString())

                this@ModelManualFragment
                    .parentFragmentManager
                    .popBackStack()

                // display the content in a quick view
                startActivity(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val intent = Intent(Intent.ACTION_QUICK_VIEW, uri)
                        if (context?.packageManager?.resolveActivity(intent, 0) == null) {
                            intent.action = Intent.ACTION_VIEW
                        }
                        intent
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                )
            }
        }
        webView.settings.pluginState = WebSettings.PluginState.ON
        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
    }

    override fun loadData() {
        this.viewModel.manual.value?.let {
            if (it.url.toURI().hasPDF()) {
                webView.loadUrl(it.url.toURI().googleDrivePDF().toString())
            } else {
                webView.loadUrl(it.url.toString())
            }
        }
    }
}
