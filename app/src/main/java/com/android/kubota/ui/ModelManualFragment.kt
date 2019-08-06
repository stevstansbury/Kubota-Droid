package com.android.kubota.ui

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ModelManualViewModel
import com.android.kubota.viewmodel.UIModel

private const val KEY_MODEL_ID = "model_id"
private const val KEY_MODEL_NAME = "model_name"
private const val DEFAULT_MODEL_ID = -1

class ModelManualFragment: BaseWebViewFragment() {

    companion object {

        fun createInstance(uiModel: UIModel): ModelManualFragment {
            val fragment = ModelManualFragment()
            val arguments = Bundle(2)
            arguments.putInt(KEY_MODEL_ID, uiModel.id)
            arguments.putString(KEY_MODEL_NAME, uiModel.modelName)
            fragment.arguments = arguments

            return fragment
        }

    }
    private lateinit var viewModel: ModelManualViewModel
    private var isPdf = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, InjectorUtils.provideModelManualViewModel(requireContext()))
            .get(ModelManualViewModel::class.java)

        arguments?.getString(KEY_MODEL_NAME)?.let {
            activity?.title = getString(R.string.manual_title, it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view  = super.onCreateView(inflater, container, savedInstanceState)

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

        val modelId = arguments?.getInt(KEY_MODEL_ID, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID

        viewModel.getModelManualLocation(modelId).observe(this, Observer {
            if (it == null) {
                flowActivity?.hideProgressBar()
                activity?.onBackPressed()
            } else {
                flowActivity?.showProgressBar()
                isPdf = it.contains("PDF", true)
                webView.loadUrl(it)
            }
        })

        return view
    }
}
