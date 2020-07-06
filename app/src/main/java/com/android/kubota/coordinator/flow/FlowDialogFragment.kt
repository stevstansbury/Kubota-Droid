package com.android.kubota.coordinator.flow

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.inmotionsoftware.flowkit.FlowError
import com.inmotionsoftware.flowkit.android.FlowFragment
import com.inmotionsoftware.flowkit.android.FlowViewModel
import com.inmotionsoftware.promisekt.*

abstract class FlowDialogFragment<Input, Output>: DialogFragment() {

    lateinit var viewModel: FlowViewModel<Input, Output>
    val input: Input? get() { return viewModel.input.value }

    fun resolve(result: com.inmotionsoftware.promisekt.Result<Output>) {
        if (!this::viewModel.isInitialized) {
            Log.e(FlowDialogFragment::class.java.name, "Resolver has not been initialized")
            return
        }

        viewModel.resolver.resolve(result)
        dismiss()
    }

    fun resolve(value: Output) {
        resolve(Result.fulfilled(value))
        dismiss()
    }

    fun reject(error: Throwable) {
        resolve(Result.rejected(error))
        dismiss()
    }

    fun cancel() {
        this.reject(FlowError.Canceled())
        dismiss()
    }

    private fun loadViewModel() {
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity()).get(FlowViewModel::class.java) as FlowViewModel<Input,Output>
        Log.d(this.javaClass.name, "input: ${viewModel.input.value}")
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        loadViewModel()
        // TODO: Load bundle
    }

}

fun <Input,Output> FlowFragment<Input, Output>.showActivityIndicator() {
    this.view?.findViewById<ProgressBar>(R.id.progressBar)?.visibility =  View.VISIBLE
}

fun <Input,Output> FlowFragment<Input, Output>.hideActivityIndicator() {
    this.view?.findViewById<ProgressBar>(R.id.progressBar)?.visibility =  View.GONE
}
