package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.util.PatternsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.SignInViewModel
import com.google.android.material.textfield.TextInputLayout
import com.kubota.repository.service.AuthCredentials
import com.kubota.repository.service.AuthResponse

private const val PASSWORD_ARGUMENT = "password"

class SignInFragment: BaseAccountSetUpFragment() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var forgotPasswordLink: View
    private lateinit var viewModel: SignInViewModel

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

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideSignInViewModel(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(SignInViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.sign_in)

        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        actionButton = view.findViewById(R.id.signInButton)
        forgotPasswordLink = view.findViewById<TextView>(R.id.forgotPasswordTextView)
        forgotPasswordLink.setOnClickListener {
            accountSetUpContext.addFragmentToBackStack(ForgotPasswordFragment())
        }

        emailField = view.findViewById(R.id.emailEditText)
        emailField.addTextChangedListener(emailTextWatcher)

        val passwordLayout: TextInputLayout = view.findViewById(R.id.passwordLayout)
        passwordField = view.findViewById(R.id.passwordEditText)
        passwordField.addTextChangedListener(passwordTextWatcher)

        val bundle = when {
            savedInstanceState != null -> savedInstanceState
            arguments != null -> arguments
            else -> null
        }
        bundle?.let { restoreState(it) }

        viewModel.signInResults.observe(viewLifecycleOwner, Observer {
            accountSetUpContext.hideProgressBar()
            when (it) {
                is AuthResponse.Success -> {
                    requireActivity().finish()
                }
                is AuthResponse.AuthenticationError -> {
                    passwordLayout.error = getString(R.string.invalid_email_password)
                }
                is AuthResponse.GenericError -> {
                    passwordLayout.error = getString(R.string.server_error_message)
                }
                is AuthResponse.IOError -> {
                    passwordLayout.error = getString(R.string.connectivity_error_message)
                }
            }
        })

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, passwordField.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        accountSetUpContext.showProgressBar()
        viewModel.signIn(
            credentials =
            AuthCredentials(
                userName = emailField.text.toString(), password = passwordField.text.toString()
            )
        )
    }

    private fun restoreState(bundle: Bundle) {
        emailField.setText(bundle.getCharSequence(EMAIL_ARGUMENT, ""))
        passwordField.setText(bundle.getCharSequence(PASSWORD_ARGUMENT, ""))
    }

    private fun shouldEnabledSignInButton() = validEmail && validPassword
}