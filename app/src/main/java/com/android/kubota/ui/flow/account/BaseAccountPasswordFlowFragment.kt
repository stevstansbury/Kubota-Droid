package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import com.android.kubota.R
import com.android.kubota.ui.ftue.PasswordRulesLayout
import com.android.kubota.utility.PasswordUtils
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.flowkit.android.FlowFragment

abstract class BaseAccountPasswordFlowFragment<Input, Output>
    : FlowFragment<Input, Output>() {

    protected lateinit var actionButton: Button
    protected lateinit var newPassword: EditText
    protected lateinit var confirmPasswordLayout: TextInputLayout
    protected lateinit var confirmPassword: EditText
    protected lateinit var passwordRulesLayout: PasswordRulesLayout

    protected var validPassword = false
    protected var passwordsMatch = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actionButton.setOnClickListener { onActionButtonClicked() }

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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

    abstract fun onActionButtonClicked()

    protected open fun areFieldsValid(): Boolean = validPassword && passwordsMatch

    protected open fun updateActionButton() {
        actionButton.isEnabled = areFieldsValid()
    }

    private fun verifyPasswordMatch() = TextUtils.equals(newPassword.text, confirmPassword.text)

}
