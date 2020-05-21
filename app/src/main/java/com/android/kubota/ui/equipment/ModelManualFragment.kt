package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.webkit.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseWebViewFragment
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import kotlinx.android.parcel.Parcelize

@Parcelize data class ManualLink(val title: String, val uri: Uri): Parcelable

private fun Uri.hasPDF(): Boolean {
    return this.lastPathSegment?.endsWith("pdf", ignoreCase = true) ?: false
}

class ModelManualFragment: BaseWebViewFragment() {
    class ManualUrlViewModel: ViewModel() {
        var manual = MutableLiveData<ManualLink>()
    }

    private val viewModel by lazy {
        ViewModelProvider(this.requireActivity()).get(ManualUrlViewModel::class.java)
    }

    companion object {
        private const val KEY_MANUAL = "KEY_MANUAL"

        fun createInstance(model: ManualLink): ModelManualFragment {
            return ModelManualFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable(KEY_MANUAL, model)
                }
            }
        }
    }

    override fun hasRequiredArgumentData(): Boolean {
        val model: ManualLink? = arguments?.getParcelable(KEY_MANUAL)
        viewModel.manual.value = model
        return model != null
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

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                failingUrl?.let {
                    val uri = Uri.parse(failingUrl)
                    if (uri.hasPDF()) { handlePDF(uri) }
                }
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                request?.let {
                    val uri = it.url
                    if (uri.hasPDF()) { handlePDF(uri) }
                }
                super.onReceivedError(view, request, error)
            }

            fun handlePDF(url: Uri) {
                if (activity == null) return

                this@ModelManualFragment
                    .parentFragmentManager
                    .popBackStack()

                // display the content in a quick view
                startActivity(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val intent = Intent(Intent.ACTION_QUICK_VIEW, url)
                        if (context?.packageManager?.resolveActivity(intent, 0) == null) {
                            intent.action = Intent.ACTION_VIEW
                        }
                        intent
                    } else {
                        Intent(Intent.ACTION_VIEW, url)
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
        this.viewModel.manual.value?.let { webView.loadUrl(it.uri.toString()) }
    }
}
