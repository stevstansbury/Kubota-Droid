package com.android.kubota.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.ManualInfo
import java.net.URL

class PDFFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_pdf

    private var info: ManualInfo? = null
    private lateinit var pdfView: PDFView
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            this.info = arguments?.getParcelable(KEY_MANUAL_INFO)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.pdf_viewer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.download_pdf) {
            this.info?.let {
                val uri = Uri.parse(it.url.toString());

                val request = DownloadManager.Request(uri);
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setTitle("${it.title}.pdf")
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${it.title}.pdf")
                val downloadManager: DownloadManager =
                    this.context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.title = info?.title
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
                .setPositiveButton(R.string.ok) {dialog, _ -> dialog.dismiss() }
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
