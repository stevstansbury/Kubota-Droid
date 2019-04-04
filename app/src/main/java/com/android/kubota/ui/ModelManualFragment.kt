package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ModelManualViewModel
import com.android.kubota.viewmodel.UIModel

private const val KEY_MODEL_ID = "model_id"
private const val DEFAULT_MODEL_ID = -1

class ModelManualFragment: BaseWebViewFragment() {

    companion object {

        fun createInstance(uiModel: UIModel): ModelManualFragment {
            val fragment = ModelManualFragment()
            val arguments = Bundle(1)
            arguments.putInt(KEY_MODEL_ID, uiModel.id)
            fragment.arguments = arguments

            return fragment
        }

    }
    private lateinit var viewModel: ModelManualViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, InjectorUtils.provideModelManualViewModel(requireContext()))
            .get(ModelManualViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view  = super.onCreateView(inflater, container, savedInstanceState)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                flowActivity?.hideProgressBar()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (view?.originalUrl != null && request?.url != null && !TextUtils.equals(request.url.toString(), view.originalUrl)) {
                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webView.settings.javaScriptEnabled = true

        val modelId = arguments?.getInt(KEY_MODEL_ID, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID

        viewModel.getModelManualLocation(modelId).observe(this, Observer {
            if (it == null) {
                flowActivity?.hideProgressBar()
                activity?.onBackPressed()
            } else {
                flowActivity?.showProgressBar()
                webView.loadUrl(it)
            }
        })

        return view
    }
}