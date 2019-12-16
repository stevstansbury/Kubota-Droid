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
import com.android.kubota.R

private const val PASSWORD_ARGUMENT = "password"

class SignInFragment: BaseAccountSetUpFragment() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var forgotPasswordLink: View

    private var validEmail = false
    private var validPassword = false

    private val emailTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            validEmail = when(s) {
                null -> false
                else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
            }

            accountSetUpContext.setNextButtonEnable(shouldEnabledSignInButton())
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

            accountSetUpContext.setNextButtonEnable(shouldEnabledSignInButton())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.sign_in)

        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        forgotPasswordLink = view.findViewById<TextView>(R.id.forgotPasswordTextView)
        forgotPasswordLink.setOnClickListener {
            accountSetUpContext.replaceFragment(ForgotPasswordFragment())
        }

        emailField = view.findViewById(R.id.emailEditText)
        emailField.addTextChangedListener(emailTextWatcher)

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
        accountSetUpContext.showProgressBar()
    }

    override fun getActionButtonText(): Int = R.string.sign_in

    override fun onBack() {
        activity?.finish()
    }

    private fun restoreState(bundle: Bundle) {
        emailField.setText(bundle.getCharSequence(EMAIL_ARGUMENT, ""))
        passwordField.setText(bundle.getCharSequence(PASSWORD_ARGUMENT, ""))
    }

    private fun shouldEnabledSignInButton() = validEmail && validPassword
}