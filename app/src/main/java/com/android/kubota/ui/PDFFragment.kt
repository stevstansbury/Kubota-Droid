package com.android.kubota.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.ManualInfo
import java.net.URL

class PDFFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_pdf

    private lateinit var info: ManualInfo
    private lateinit var pdfView: PDFView
    private var dialog: AlertDialog? = null

    private lateinit var saveDoc: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        this.info = arguments?.getParcelable(KEY_MANUAL_INFO) ?: throw IllegalStateException()

        saveDoc = registerForActivityResult(CreatePDFDocument()) { destinationFile ->
            AppProxy.proxy.logFirebaseEvent("download_manual") {
                param("title", info.title)
            }
            AppProxy.proxy.serviceManager.contentService
                .getContent(url = info.url)
                .map(on = DispatchExecutor.global) {
                    AppProxy.proxy.contentResolver.openOutputStream(destinationFile)!!.write(it!!)
                    Thread.sleep(2000)
                }
                .ensure { hideProgressBar() }
                .cauterize()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.pdf_viewer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.download_pdf) {
            showProgressBar()
            saveDoc.launch("${info.title}.pdf")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadPDF(url: URL): Promise<Int> {
        return AppProxy.proxy.serviceManager.contentService
            .getContent(url = url)
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
        this.activity?.title = info.title
        this.showProgressBar()
        this.loadPDF(info.url)
            .catch { this.showError(it) }
            .finally { this.hideProgressBar() }
    }

    override fun showError(error: Throwable) {
        val errorStringId = when (error) {
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                R.string.connectivity_error_message
            is KubotaServiceError.ServerMaintenance ->
                R.string.server_maintenance
            else -> R.string.server_error_message
        }

        context?.let {
            dialog = AlertDialog.Builder(it)
                .setTitle(R.string.title_error)
                .setMessage(errorStringId)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }

    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        dialog = null
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

class CreatePDFDocument : ActivityResultContracts.CreateDocument() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input)
            .setType("application/pdf")
    }
}
