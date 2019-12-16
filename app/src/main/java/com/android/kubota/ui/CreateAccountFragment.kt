package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.util.PatternsCompat
import com.android.kubota.R
import com.google.android.material.textfield.TextInputLayout

private const val PASSWORD_ARGUMENT = "password"
private const val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"

class CreateAccountFragment: BaseAccountSetUpFragment() {

    private lateinit var emailField: EditText
    private lateinit var newPassword: EditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPassword: EditText

    private var validEmail = false
    private var passwordsMatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.title = getString(R.string.create_account)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_account, null)

        emailField = view.findViewById(R.id.emailEditText)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)

        emailField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validEmail = when(s) {
                    null -> false
                    else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
                }

                accountSetUpContext.setNextButtonEnable(passwordsMatch && validEmail)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        newPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                accountSetUpContext.setNextButtonEnable(passwordsMatch && validEmail)
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
                accountSetUpContext.setNextButtonEnable(passwordsMatch && validEmail)
                if (passwordsMatch && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch && confirmPasswordLayout.error == null) {
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
                accountSetUpContext.setNextButtonEnable(passwordsMatch && validEmail)
                if (passwordsMatch && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch && confirmPasswordLayout.error == null) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
            }

            false
        }

        confirmPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                passwordsMatch = verifyPasswordMatchAndNotEmpty()
                accountSetUpContext.setNextButtonEnable(passwordsMatch && validEmail)
                if (passwordsMatch && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch && confirmPasswordLayout.error == null) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
            }
        }

        savedInstanceState?.let {
            emailField.setText(it.getCharSequence(EMAIL_ARGUMENT, ""))
            newPassword.setText(it.getCharSequence(PASSWORD_ARGUMENT, ""))
            confirmPassword.setText(it.getCharSequence(CONFIRM_PASSWORD_ARGUMENT, ""))
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun getActionButtonText(): Int = R.string.create_account

    override fun onActionButtonClicked() {
        accountSetUpContext.showProgressBar()
    }

    override fun onBack() {
        activity?.finish()
    }

    private fun verifyPasswordMatchAndNotEmpty(): Boolean {
        val newPassword = newPassword.text
        return newPassword.isNotEmpty() && newPassword.isNotBlank() && TextUtils.equals(newPassword, confirmPassword.text)
    }
}