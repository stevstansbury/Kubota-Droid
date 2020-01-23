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
import com.android.kubota.R
import com.google.android.material.textfield.TextInputLayout

class NewPasswordFragment: BaseAccountSetUpFragment() {

    private lateinit var newPassword: EditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPassword: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (accountSetUpContext.getMode() == AccountSetUpContext.NEW_PASSWORD_FLOW) {
            activity?.title = getString(R.string.new_password)
        }

        val view = inflater.inflate(R.layout.fragment_new_password, null)

        actionButton = view.findViewById(R.id.nextButton)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)

        newPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val matches = verifyPasswordMatchAndNotEmpty()
                actionButton.isEnabled =  matches
                if (matches && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!matches) {
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
                val matches =verifyPasswordMatchAndNotEmpty()
                actionButton.isEnabled =  matches
                if (matches && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!matches && confirmPasswordLayout.error == null) {
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
                val matches =verifyPasswordMatchAndNotEmpty()
                actionButton.isEnabled =  matches
                if (matches && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!matches && confirmPasswordLayout.error == null) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
            }

            false
        }

        confirmPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val matches =verifyPasswordMatchAndNotEmpty()
                actionButton.isEnabled = matches
                if (matches && confirmPasswordLayout.error != null) {
                    confirmPasswordLayout.error = null
                } else if (!matches && confirmPasswordLayout.error == null) {
                    confirmPasswordLayout.error = getString(R.string.matching_password_error)
                }
            }
        }

        return view
    }

    override fun onActionButtonClicked() {
        accountSetUpContext.clearBackStack()
        accountSetUpContext.addFragmentToBackStack(SignInFragment().apply {
            this.arguments = this@NewPasswordFragment.arguments
        })
    }

    private fun verifyPasswordMatchAndNotEmpty(): Boolean {
        val newPassword = newPassword.text
        return newPassword.isNotEmpty() && newPassword.isNotBlank() && TextUtils.equals(newPassword, confirmPassword.text)
    }
}