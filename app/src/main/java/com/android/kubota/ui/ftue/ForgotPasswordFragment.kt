package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.util.PatternsCompat
import androidx.lifecycle.lifecycleScope
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.BaseAccountSetUpFragment
import com.android.kubota.viewmodel.ftue.ForgotPasswordViewModel
import com.android.kubota.viewmodel.ftue.ForgotPasswordViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import com.kubota.repository.service.Result
import kotlinx.coroutines.*

class ForgotPasswordFragment: BaseAccountSetUpFragment<ForgotPasswordController>() {

    private lateinit var viewModel: ForgotPasswordViewModel
    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ForgotPasswordViewModelFactory()
            .create(ForgotPasswordViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, null)
        setupUI(view)

        when {
            savedInstanceState != null -> savedInstanceState
            arguments != null -> arguments
            else -> null
        }?.let { emailAddress.setText(it.getCharSequence(EMAIL_ARGUMENT, "")) }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailAddress.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        controller.showProgressBar()

        viewLifecycleOwner.lifecycleScope.launch {
            onResult(viewModel.sendCode(emailAddress.text.toString()))
        }
    }

    private fun onResult(result: Result) {
        controller.hideProgressBar()
        when (result) {
            is Result.Success -> {
                controller.onSuccess(result.response as String)
            }
            is Result.InvalidEmail -> {
                emailLayout.error = getString(R.string.email_incorrect_error)
            }
            is Result.NetworkError -> {
                controller.makeSnackbar().setText(R.string.connectivity_error_message).show()
            }
            else -> {
                controller.makeSnackbar().setText(R.string.server_error_message).show()
            }
        }
    }

    private fun setupUI(view: View) {
        actionButton = view.findViewById(R.id.nextButton)
        emailLayout = view.findViewById(R.id.emailInputLayout)
        emailAddress = view.findViewById(R.id.emailEditText)

        emailAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isEnabled = s?.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) ?: false
                actionButton.isEnabled = isEnabled
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }
}