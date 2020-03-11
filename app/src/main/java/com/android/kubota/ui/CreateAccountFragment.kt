package com.android.kubota.ui

import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.util.PatternsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.ui.AccountSetupActivity.Companion.startActivityForSignIn
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.CreateAccountViewModel
import com.google.android.material.textfield.TextInputLayout
import com.kubota.repository.service.Response

private const val PASSWORD_ARGUMENT = "password"
private const val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"

class CreateAccountFragment: NewPasswordSetUpFragment() {

    private lateinit var viewModel: CreateAccountViewModel
    private lateinit var emailField: EditText

    private var validEmail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this,
            InjectorUtils.provideCreateAccountViewModel()
        ).get(CreateAccountViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        activity?.title = getString(R.string.create_account)
        val view = inflater.inflate(R.layout.fragment_create_account, null)

        val emailInputLayout = view.findViewById<TextInputLayout>(R.id.emailInputLayout)
        emailField = view.findViewById(R.id.emailEditText)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)
        actionButton = view.findViewById(R.id.createAccountButton)

        emailField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validEmail = when(s) {
                    null -> false
                    else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
                }
                emailInputLayout.error?.let {
                    emailInputLayout.error = null
                }

                updateActionButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })


        val termsAndConditionsLink = view.findViewById<TextView>(R.id.termsAndConditionsLink)
        val linkPortion = getString(R.string.kubota_terms_and_conditions_link)
        val fullText = getString(R.string.create_account_terms_conditions_link, linkPortion)
        val startIdx = fullText.indexOf(linkPortion)
        val endIdx = startIdx + linkPortion.length
        val spannableString = SpannableString(fullText)
        spannableString.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                accountSetUpContext.addFragmentToBackStack(LegalTermsFragment())
            }

        }, startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        termsAndConditionsLink.text = spannableString
        termsAndConditionsLink.movementMethod = LinkMovementMethod.getInstance()

        viewModel.isLoading.observe(this, Observer {isLoading ->
            when (isLoading) {
                true -> accountSetUpContext.showProgressBar()
                else -> accountSetUpContext.hideProgressBar()
            }
        })

        viewModel.result.observe(this, Observer {response ->
            when (response){
                is Response.Success -> {
                    startActivityForSignIn(requireContext())
                }
                is Response.DuplicateAccount -> {
                    emailInputLayout.error = getString(R.string.duplicate_account_error)
                }
                is Response.BlacklistedPassword -> {
                    newPasswordLayout.error = getString(R.string.password_rule_blacklisted)
                }
                is Response.InvalidPassword -> {
                    newPasswordLayout.error = getString(R.string.password_rule_generic_invalid_password)
                }
                is Response.IOError -> {
                    newPasswordLayout.error = getString(R.string.connectivity_error_message)
                }
                is Response.GenericError -> {
                    newPasswordLayout.error = getString(R.string.server_error_message)
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            emailField.setText(it.getCharSequence(EMAIL_ARGUMENT, ""))
            newPassword.setText(it.getCharSequence(PASSWORD_ARGUMENT, ""))
            confirmPassword.setText(it.getCharSequence(CONFIRM_PASSWORD_ARGUMENT, ""))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        accountSetUpContext.showProgressBar()
        viewModel.createAccount(emailField.text.toString(), newPassword.text.toString())
    }

    override fun areFieldsValid(): Boolean {
        return super.areFieldsValid() && validEmail
    }
}