package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.kubota.service.api.KubotaServiceError


abstract class BaseFragment : Fragment() {
    protected var flowActivity: FlowActivity? = null

    protected abstract val layoutResId: Int
    private var hasRequiredArgs = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasRequiredArgs = hasRequiredArgumentData()
        if (!hasRequiredArgs) {
            activity?.onBackPressed()
        }
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (hasRequiredArgs) {
            val view = inflater.inflate(layoutResId, null)
            initUi(view)
            loadData()

            return view
        } else {
            null
        }
    }

    fun showProgressBar() = this.flowActivity?.showProgressBar()
    fun hideProgressBar() = this.flowActivity?.hideProgressBar()

    fun showError(error: Throwable) {
        when (error) {
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
            else ->
                this.flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
        }
    }

    fun showError(message: String) {
        flowActivity?.makeSnackbar()?.setText(message)?.show()
    }

    protected abstract fun initUi(view: View)

    protected open fun hasRequiredArgumentData() = true

    protected abstract fun loadData()
}