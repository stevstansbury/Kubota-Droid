package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.android.kubota.R
import com.google.android.material.textfield.TextInputLayout

class NewPasswordFragment: NewPasswordSetUpFragment() {

    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var currentPassword: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_new_password, null)

        actionButton = view.findViewById(R.id.nextButton)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        currentPasswordLayout = view.findViewById(R.id.passwordInputLayout)
        currentPassword = view.findViewById(R.id.passwordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)

        if (accountSetUpContext.getMode() == AccountSetUpContext.NEW_PASSWORD_FLOW) {
            activity?.title = getString(R.string.new_password)
        } else {
            currentPasswordLayout.visibility = View.GONE
            val layoutParams = newPasswordLayout.layoutParams as LinearLayout.LayoutParams
            layoutParams.topMargin = resources.getDimension(R.dimen.current_password_top_margin).toInt()
            newPasswordLayout.layoutParams = layoutParams
        }

        currentPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateActionButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        return view
    }

    override fun onActionButtonClicked() {
        if (accountSetUpContext.getMode() == AccountSetUpContext.NEW_PASSWORD_FLOW) {
            activity?.finish()
        } else {
            accountSetUpContext.clearBackStack()
        }
    }

    override fun areFieldsValid(): Boolean {
        return when (accountSetUpContext.getMode() == AccountSetUpContext.NEW_PASSWORD_FLOW) {
            true -> currentPassword.text.isNotEmpty() && super.areFieldsValid()
            else -> super.areFieldsValid()
        }
    }
}