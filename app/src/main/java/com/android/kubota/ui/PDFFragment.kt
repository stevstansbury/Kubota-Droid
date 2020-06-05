package com.android.kubota.ui

import android.os.Bundle
import android.view.View
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.ManualInfo
import java.net.URL

class PDFFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_pdf

    private var info: ManualInfo? = null
    private lateinit var pdfView: PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.info = arguments?.getParcelable(KEY_MANUAL_INFO)
        }
    }

    fun loadPDF(url: URL): Promise<Int> {
        return AppProxy.proxy.serviceManager.contentService
            .getContent(url=url)
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
        this.info?.let {
            this.activity?.title = it.title
            this.showProgressBar()
            this.loadPDF(it.url)
                .done {pages -> }
                .catch { this.showError(it) }
                .finally { this.hideProgressBar() }
        }
    }

    companion object {
        private const val KEY_MANUAL_INFO = "KEY_MANUAL_INFO"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 URL for the PDF document
         * @return A new instance of fragment PDFFragment.
         */
        @JvmStatic
        fun createInstance(manual: ManualInfo) =
            PDFFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_MANUAL_INFO, manual)
                }
            }
    }
}
