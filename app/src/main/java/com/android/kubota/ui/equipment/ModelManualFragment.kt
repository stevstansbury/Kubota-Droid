package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseWebViewFragment
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done

class ModelManualFragment: BaseWebViewFragment() {

    companion object {
        private const val KEY_MODEL_NAME = "KEY_MODEL_NAME"
        private const val DEFAULT_MODEL_NAME = ""

        fun createInstance(model: String): ModelManualFragment {
            return ModelManualFragment().apply {
                arguments = Bundle(1).apply {
                    putString(KEY_MODEL_NAME, model)
                }
            }
        }
    }

    private var modelName = DEFAULT_MODEL_NAME
    private var isPdf = false

    override fun hasRequiredArgumentData(): Boolean {
        modelName = arguments?.getString(
            KEY_MODEL_NAME,
            DEFAULT_MODEL_NAME
        ) ?: DEFAULT_MODEL_NAME
        return modelName != DEFAULT_MODEL_NAME
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initUi(view: View) {
        super.initUi(view)

        if (this.modelName.isNotBlank()) {
            activity?.title = (getString(R.string.manual_title, this.modelName))
        }

        webView.webViewClient = object : WebViewListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideProgressBar()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (isPdf && view?.originalUrl != null && request?.url != null && !TextUtils.equals(request.url.toString(), view.originalUrl)) {
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
    }

    override fun loadData() {
        this.showProgressBar()
        AppProxy.proxy.serviceManager.equipmentService.getManualURL(model = modelName)
                .done {
                    val url = it.toString()
                    isPdf = url.contains("PDF", true)
                    webView.loadUrl(url)
                }
                .catch { error ->
                    this.hideProgressBar()
                    this.showError(error)
                }
    }

}
