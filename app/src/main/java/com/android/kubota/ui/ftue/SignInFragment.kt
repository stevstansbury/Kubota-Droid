package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.EmailTextWatcher
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.KubotaServiceError

private const val PASSWORD_ARGUMENT = "password"

class SignInFragment: BaseAccountSetUpFragment<SignInController>() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var forgotPasswordLink: View
    private lateinit var passwordLayout: TextInputLayout

    private var validEmail = false
    private var validPassword = false

    private val passwordTextWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            validPassword = when (s) {
                null -> false
                else -> s.isNotBlank() && s.isNotEmpty()
            }

            actionButton.isEnabled = shouldEnabledSignInButton()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.sign_in)

        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        actionButton = view.findViewById(R.id.signInButton)
        forgotPasswordLink = view.findViewById<TextView>(R.id.forgotPasswordTextView)
        forgotPasswordLink.setOnClickListener {
            controller.onForgotPassword()
        }

        emailField = view.findViewById(R.id.emailEditText)
        emailField.addTextChangedListener(
            EmailTextWatcher(emailField) { isValidEmail ->
                validEmail = isValidEmail
                actionButton.isEnabled = shouldEnabledSignInButton()
            }
        )

        passwordLayout = view.findViewById(R.id.passwordLayout)
        passwordField = view.findViewById(R.id.passwordEditText)
        passwordField.addTextChangedListener(passwordTextWatcher)

        val bundle = when {
            savedInstanceState != null -> savedInstanceState
            arguments != null -> arguments
            else -> null
        }
        bundle?.let { restoreState(it) }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, passwordField.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        controller.showProgressBar()

        AppProxy.proxy.accountManager.authenticate(username = emailField.text.toString(), password = passwordField.text.toString())
            .done { controller.onSuccess() }
            .ensure { controller.hideProgressBar() }
            .catch {error ->
                when (error) {
                    is AccountError.InvalidCredentials -> {
                        passwordLayout.error = getString(R.string.invalid_email_password)
                    }
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet -> {
                        passwordLayout.error = getString(R.string.connectivity_error_message)
                    }
                    is KubotaServiceError.ServerMaintenance ->
                        passwordLayout.error = getString(R.string.server_maintenance)
                    else -> {
                        passwordLayout.error = getString(R.string.server_error_message)
                    }
                }
            }
    }

    private fun restoreState(bundle: Bundle) {
        emailField.setText(bundle.getCharSequence(EMAIL_ARGUMENT, ""))
        passwordField.setText(bundle.getCharSequence(PASSWORD_ARGUMENT, ""))
    }

    private fun shouldEnabledSignInButton() = validEmail && validPassword
}