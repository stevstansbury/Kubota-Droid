package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputLayout

class NewPasswordFlowFragment
    : BaseAccountPasswordFlowFragment<NewPasswordFlowFragment.Input, NewPasswordFlowFragment.Result>() {

    enum class Type {
        RESET_PASSWORD,
        CHANGE_PASSWORD
    }

    data class Input(val type: Type, val error: Throwable?)

    sealed class Result {
        class ResetPassword(val verificationCode: String, val newPassword: String): Result()
        class ChangePassword(val currentPassword: String, val newPassword: String): Result()
    }

    private lateinit var resetPasswordHeader: View
    private lateinit var verificationCodeLayout: TextInputLayout

    private lateinit var verificationCodeEditText: EditText
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var currentPassword: EditText

    private var input: MutableLiveData<Input> = MutableLiveData()

    override fun onInputAttached(input: Input) {
        this.input.postValue(input)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_password, container, false)

        // Super properties
        actionButton = view.findViewById(R.id.nextButton)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)

        // This properties
        resetPasswordHeader = view.findViewById(R.id.resetPasswordHeader)
        verificationCodeLayout = view.findViewById(R.id.verificationCodeLayout)
        currentPasswordLayout = view.findViewById(R.id.passwordInputLayout)
        verificationCodeEditText = view.findViewById(R.id.verificationCodeEditText)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        currentPassword = view.findViewById(R.id.passwordEditText)

        currentPassword.addTextChangedListener(afterTextChanged={
            updateActionButton()
        })

        this.input.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })

        return view
    }

    private fun updateView(input: Input) {
        when (input.type) {
            Type.CHANGE_PASSWORD -> {
                activity?.title = getString(R.string.change_password_preference_title)
                resetPasswordHeader.visibility = View.GONE
                verificationCodeLayout.visibility = resetPasswordHeader.visibility
            }
            Type.RESET_PASSWORD -> {
                activity?.title = getString(R.string.forgot_password)
                currentPasswordLayout.visibility = View.GONE
            }
        }
        when (input.error) {
            is AccountError.InvalidCredentials ->
                currentPasswordLayout.error = getString(R.string.invalid_email_password)
            is AccountError.InvalidPassword ->
                newPasswordLayout.error = getString(R.string.password_rule_generic_invalid_password)
            is AccountError.InvalidPasswordResetCode ->
                newPasswordLayout.error = getString(R.string.forgot_password_invalid_reset_code)
        }

        currentPassword.requestFocus()
        val mgr = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        mgr?.showSoftInput(currentPassword, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()

        val input = this.input.value ?: return
        when (input.type) {
            Type.CHANGE_PASSWORD ->
                this.resolve(
                    Result.ChangePassword(
                        currentPassword = currentPassword.text.toString(),
                        newPassword = newPassword.text.toString()
                    )
                )
            Type.RESET_PASSWORD ->
                this.resolve(
                    Result.ResetPassword(
                        verificationCode = verificationCodeEditText.text.toString(),
                        newPassword = newPassword.text.toString()
                    )
                )
        }
    }

    override fun areFieldsValid(): Boolean {
        if (!super.areFieldsValid()) return false
        val input = this.input.value ?: return false
        return if (input.type == Type.CHANGE_PASSWORD) {
            currentPassword.text.isNotEmpty()
        } else {
            verificationCodeEditText.text.isNotEmpty()
        }
    }
}
