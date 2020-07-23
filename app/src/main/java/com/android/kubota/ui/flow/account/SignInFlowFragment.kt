package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.PatternsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.flowkit.android.FlowFragment
import com.kubota.service.api.KubotaServiceError


class SignInFlowFragment: FlowFragment<Throwable?, SignInFlowFragment.Result>() {

    sealed class Result {
        class SignIn(val username: String, val password: String): Result()
        object ForgotPassword : Result()
    }

    companion object {
        val PASSWORD_ARGUMENT = "password"
        val EMAIL_ARGUMENT = "account_email"
    }

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var forgotPasswordView: TextView
    private lateinit var actionButton: Button

    private var input: MutableLiveData<Throwable?> = MutableLiveData()

    private var validEmail = false
    private var validPassword = false

    private val emailTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            validEmail = when(s) {
                null -> false
                else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
            }
            actionButton.isEnabled = shouldEnabledSignInButton()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private val passwordTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            validPassword = when (s) {
                null -> false
                else -> s.isNotBlank() && s.isNotEmpty()
            }
            actionButton.isEnabled = shouldEnabledSignInButton()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    override fun onInputAttached(input: Throwable?) {
        this.input.postValue(input)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        activity?.setTitle(R.string.sign_in)

        forgotPasswordView = view.findViewById(R.id.forgotPasswordTextView)
        emailField = view.findViewById(R.id.emailEditText)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        passwordField = view.findViewById(R.id.passwordEditText)
        actionButton = view.findViewById(R.id.signInButton)

        emailField.addTextChangedListener(emailTextWatcher)
        passwordField.addTextChangedListener(passwordTextWatcher)
        forgotPasswordView.setOnClickListener {
            this.resolve(Result.ForgotPassword)
        }
        actionButton.setOnClickListener { onActionButtonClicked() }

        val bundle = when {
            savedInstanceState != null -> savedInstanceState
            arguments != null -> arguments
            else -> null
        }
        bundle?.let { restoreState(it) }

        this.input.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        emailField.requestFocus()
        val mgr = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        mgr?.showSoftInput(emailField, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateView(input: Throwable?) {
        when (input) {
            null -> {
                passwordLayout.error = ""
            }
            is AccountError.InvalidCredentials -> {
                passwordLayout.error = getString(R.string.invalid_email_password)
            }
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet -> {
                passwordLayout.error = getString(R.string.connectivity_error_message)
            }
            else -> {
                passwordLayout.error = getString(R.string.server_error_message)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, passwordField.text)
    }

    private fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        this.resolve(
            Result.SignIn(
                username = emailField.text.toString(),
                password = passwordField.text.toString()
            )
        )
    }

    private fun restoreState(bundle: Bundle) {
        emailField.setText(bundle.getCharSequence(EMAIL_ARGUMENT, ""))
        passwordField.setText(bundle.getCharSequence(PASSWORD_ARGUMENT, ""))
    }

    private fun shouldEnabledSignInButton() = validEmail && validPassword

}
