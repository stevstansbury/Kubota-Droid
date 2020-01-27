package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.utility.PasswordUtils
import com.google.android.material.textfield.TextInputLayout


abstract class BaseFragment : Fragment() {
    protected var flowActivity: FlowActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FlowActivity) {
            flowActivity = context
        }
    }
}

abstract class BaseAccountSetUpFragment : Fragment() {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
    }

    protected lateinit var accountSetUpContext: AccountSetUpContext
    protected lateinit var actionButton: Button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountSetUpContext = context as AccountSetUpContext
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionButton.setOnClickListener { onActionButtonClicked() }
    }

    abstract fun onActionButtonClicked()
}

abstract class NewPasswordSetUpFragment : BaseAccountSetUpFragment() {
    protected lateinit var newPasswordLayout: TextInputLayout
    protected lateinit var newPassword: EditText
    protected lateinit var confirmPasswordLayout: TextInputLayout
    protected lateinit var confirmPassword: EditText
    protected lateinit var passwordRulesLayout: PasswordRulesLayout

    protected var validPassword = false
    protected var passwordsMatch = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                passwordsMatch = verifyPasswordMatch()
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
                passwordsMatch = verifyPasswordMatch()
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
                passwordsMatch = verifyPasswordMatch()
                updateActionButton()
                if (actionButton.isEnabled) {
                    onActionButtonClicked()
                }
            }

            false
        }

        confirmPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                passwordsMatch = verifyPasswordMatch()
                if (confirmPassword.text.isNullOrBlank() || passwordsMatch) {
                    confirmPasswordLayout.error = null
                } else if (!passwordsMatch) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
                updateActionButton()
            }
        }
    }

    private fun verifyPasswordMatch() = TextUtils.equals(newPassword.text, confirmPassword.text)

    protected open fun areFieldsValid(): Boolean {
        return validPassword && passwordsMatch
    }

    protected fun updateActionButton() {
        actionButton.isEnabled = areFieldsValid()
    }
}