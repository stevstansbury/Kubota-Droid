package com.android.kubota.ui

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ModelManualViewModel

private const val KEY_MODEL_ID = "model_id"
private const val KEY_MODEL_NAME = "model_name"
private const val DEFAULT_MODEL_ID = -1

class ModelManualFragment: BaseWebViewFragment() {

    companion object {

        fun createInstance(equipmentId: Int, model: String): ModelManualFragment {
            val fragment = ModelManualFragment()
            val arguments = Bundle(2)
            arguments.putInt(KEY_MODEL_ID, equipmentId)
            arguments.putString(KEY_MODEL_NAME, model)
            fragment.arguments = arguments

            return fragment
        }
    }
    private lateinit var viewModel: ModelManualViewModel
    private var modelId = DEFAULT_MODEL_ID
    private var isPdf = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideModelManualViewModel(requireContext())
        viewModel = ViewModelProvider(this, factory)
            .get(ModelManualViewModel::class.java)
    }

    override fun hasRequiredArgumentData(): Boolean {
        modelId = arguments?.getInt(KEY_MODEL_ID, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID
        return modelId != DEFAULT_MODEL_ID
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
        viewModel.getModelManualLocation(modelId).observe(viewLifecycleOwner, Observer {
            if (it == null) {
                flowActivity?.hideProgressBar()
                activity?.onBackPressed()
            } else {
                flowActivity?.showProgressBar()
                isPdf = it.contains("PDF", true)
                webView.loadUrl(it)
            }
        })
    }
}
