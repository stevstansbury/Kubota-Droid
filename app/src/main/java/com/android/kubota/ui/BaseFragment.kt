package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.inmotionsoftware.promisekt.Promise
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

    open fun showProgressBar() = this.flowActivity?.showProgressBar()
    open fun hideProgressBar() = this.flowActivity?.hideProgressBar()

    open fun showError(error: Throwable) {
        when (error) {
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                flowActivity?.makeSnackbar()?.setText(R.string.connectivity_error_message)?.show()
            else ->
                this.flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
        }
    }

    open fun showError(message: String) {
        flowActivity?.makeSnackbar()?.setText(message)?.show()
    }

    open fun signInAsync(): Promise<Unit> {
        return (this.requireActivity() as? AccountController)?.signInAsync() ?: Promise.value(Unit)
    }

    protected abstract fun initUi(view: View)

    protected open fun hasRequiredArgumentData() = true

    protected abstract fun loadData()
}

abstract class BaseBindingFragment<B: ViewDataBinding, VM: ViewModel>: Fragment() {

    protected abstract val layoutResId: Int

    private var b: B? = null
    protected val binding get() = b!!
    protected abstract val viewModel: VM

    protected var flowActivity: FlowActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = DataBindingUtil.inflate(
            inflater, layoutResId,
            container,
            false
        )
        binding.lifecycleOwner = this

        return binding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b = null
    }

    protected abstract fun loadData()
}