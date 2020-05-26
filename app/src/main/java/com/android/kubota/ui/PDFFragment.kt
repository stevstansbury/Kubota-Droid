package com.android.kubota.ui

import android.os.Bundle
import android.view.View
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.inmotionsoftware.promisekt.*
import java.net.URL

class PDFFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_pdf

    private var url: String? = null
    private lateinit var pdfView: PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val url = arguments?.getString(KEY_URL)
            this.url = url
        }
    }

    fun loadPDF(url: URL): Promise<Int> {
        return AppProxy.proxy.serviceManager.contentService.getContent(url = url)
                        .thenMap {
                            val pending = Promise.pending<Int>()
                            this.pdfView.fromBytes(it)
                                .onLoad { pending.second.fulfill(it) }
                                .onError { pending.second.reject(it) }
                                .scrollHandle(DefaultScrollHandle(this.context))
                                .autoSpacing(false)
                                .fitEachPage(true)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .enableAntialiasing(true)
                                .load()
                            pending.first
                        }
    }

    override fun initUi(view: View) {
        this.pdfView = view.findViewById(R.id.pdfView) as PDFView
    }

    override fun loadData() {
        this.showProgressBar()
        this.loadPDF(URL(url))
            .done {pages -> }
            .catch { this.showError(it) }
            .finally { this.hideProgressBar() }
    }

    companion object {
        private const val KEY_URL = "KEY_URL"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 URL for the PDF document
         * @return A new instance of fragment PDFFragment.
         */
        @JvmStatic
        fun createInstance(url: String) =
            PDFFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_URL, url)
                }
            }
    }
}
