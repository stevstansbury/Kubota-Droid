package com.android.kubota.ui.ftue

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

abstract class BaseAccountSetUpFragment<T : AccountSetUpController> : Fragment() {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
        const val ACCESS_TOKEN = "access_token"
    }

    protected lateinit var controller: T
    protected lateinit var actionButton: Button

    @Suppress("UNCHECKED_CAST")
    override fun onAttach(context: Context) {
        super.onAttach(context)

        controller = context as T
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionButton.setOnClickListener { onActionButtonClicked() }
    }

    private fun onBackPressed(): Boolean {
        activity?.onBackPressed()
        return true
    }

    abstract fun onActionButtonClicked()
}

abstract class NewPasswordSetUpFragment<T : AccountSetUpController> : BaseAccountSetUpFragment<T>() {
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

        confirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                passwordsMatch = verifyPasswordMatch()
                updateActionButton()
                if (actionButton.isEnabled) {
                    onActionButtonClicked()
                }
            }

            false
        }

        confirmPassword.setOnFocusChangeListener { _, hasFocus ->
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