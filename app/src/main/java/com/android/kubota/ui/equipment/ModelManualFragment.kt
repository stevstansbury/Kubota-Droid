package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseWebViewFragment
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ModelManualViewModel
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure

private const val KEY_MODEL_NAME = "model_name"
private const val DEFAULT_MODEL_NAME = ""

class ModelManualFragment: BaseWebViewFragment() {

    companion object {

        fun createInstance(model: String): ModelManualFragment {
            val fragment = ModelManualFragment()
            val arguments = Bundle(2)
            arguments.putString(KEY_MODEL_NAME, model)
            fragment.arguments = arguments

            return fragment
        }
    }
    private lateinit var viewModel: ModelManualViewModel
    private var modelName = DEFAULT_MODEL_NAME
    private var isPdf = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideModelManualViewModel(requireContext())
        viewModel = ViewModelProvider(this, factory)
            .get(ModelManualViewModel::class.java)
    }

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

        arguments?.getString(KEY_MODEL_NAME)?.let {
            activity?.setTitle(getString(R.string.manual_title, it))
        }

        webView.webViewClient = object : WebViewListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                flowActivity?.hideProgressBar()
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
        flowActivity?.showProgressBar()
        AppProxy.proxy.serviceManager.equipmentService.getManualURL(model = modelName)
                .done {
                    val url = it.toString()
                    isPdf = url.contains("PDF", true)
                    webView.loadUrl(url)
                }
                .ensure {
                    flowActivity?.hideProgressBar()
                }
                .catch { error ->
                    // TODO: Handle error
                }
    }

}
