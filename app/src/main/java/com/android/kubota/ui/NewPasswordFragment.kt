package com.android.kubota.ui

import android.os.Bundle
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

        return view
    }

    override fun onActionButtonClicked() {
        accountSetUpContext.clearBackStack()
        accountSetUpContext.addFragmentToBackStack(SignInFragment().apply {
            this.arguments = this@NewPasswordFragment.arguments
        })
    }

    override fun areFieldsValid(): Boolean {
        return when (accountSetUpContext.getMode() == AccountSetUpContext.NEW_PASSWORD_FLOW) {
            true -> currentPassword.text.isNotEmpty() && super.areFieldsValid()
            else -> super.areFieldsValid()
        }
    }
}