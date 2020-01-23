package com.android.kubota.ui

import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.util.PatternsCompat
import com.android.kubota.R
import com.android.kubota.utility.PasswordUtils
import com.google.android.material.textfield.TextInputLayout

private const val PASSWORD_ARGUMENT = "password"
private const val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"

class CreateAccountFragment: BaseAccountSetUpFragment() {

    private lateinit var emailField: EditText
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var newPassword: EditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPassword: EditText
    private lateinit var passwordRulesLayout: PasswordRulesLayout

    private var validEmail = false
    private var validPassword = false
    private var passwordsMatch = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        activity?.title = getString(R.string.create_account)
        val view = inflater.inflate(R.layout.fragment_create_account, null)

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

                updateActionButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        newPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString() ?: ""
                passwordRulesLayout.verifyNewPassword(input)
                validPassword = PasswordUtils.isValidPassword(input)

                if (!PasswordUtils.containsInvalidCharacters(input)) {
                    newPasswordLayout.error = null
                } else {
                    newPasswordLayout.error = getString(R.string.invalid_character_error)
                }

                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                updateActionButton()
                if (passwordsMatch && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch) {
                    val confirmPasswordText = confirmPassword.text
                    if (confirmPasswordText.isNotBlank() && confirmPasswordText.isNotEmpty()) {
                        confirmPasswordLayout.error = getString(R.string.matching_password_error)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        confirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                updateActionButton()
                if (passwordsMatch) {
                    confirmPasswordLayout.error = null
                } else {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        confirmPassword.setOnEditorActionListener { v, actionId, event ->
            if ((actionId and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                updateActionButton()
                if (actionButton.isEnabled) {
                    onActionButtonClicked()
                }
            }

            false
        }

        confirmPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                if (confirmPassword.text.isNullOrBlank() || passwordsMatch) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
                updateActionButton()
            }
        }

        savedInstanceState?.let {
            emailField.setText(it.getCharSequence(EMAIL_ARGUMENT, ""))
            newPassword.setText(it.getCharSequence(PASSWORD_ARGUMENT, ""))
            confirmPassword.setText(it.getCharSequence(CONFIRM_PASSWORD_ARGUMENT, ""))
        }


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

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun onActionButtonClicked() {
        accountSetUpContext.showProgressBar()
    }

    private fun verifyPasswordMatchAndNotEmpty() = TextUtils.equals(newPassword.text, confirmPassword.text)

    private fun updateActionButton() {
        actionButton.isEnabled = validPassword && passwordsMatch && validEmail
    }
}