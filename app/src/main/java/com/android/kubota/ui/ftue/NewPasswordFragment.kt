package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.AuthPromise
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.auth.ResetPasswordToken

const val FORGOT_PASSWORD_TOKEN = "FORGOT_PASSWORD_TOKEN"

class NewPasswordFragment: NewPasswordSetUpFragment<NewPasswordController>() {
    private lateinit var resetPasswordHeader: View
    private lateinit var verificationCodeLayout: TextInputLayout
    private lateinit var verificationCodeEditText: EditText
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var currentPassword: EditText
    private var forgotPasswordToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forgotPasswordToken = arguments?.getString(FORGOT_PASSWORD_TOKEN)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_password, null)
        initializeUI(view)
        return view
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        controller.showProgressBar()

        AuthPromise()
            .then {
                if (controller.getMode() != AccountSetUpController.NEW_PASSWORD_FLOW) {
                    AppProxy.proxy.accountManager.resetPassword(
                        token = ResetPasswordToken(token = forgotPasswordToken!!),
                        verificationCode = verificationCodeEditText.text.toString(),
                        newPassword = newPassword.text.toString()
                    )
                } else {
                    AppProxy.proxy.accountManager.changePassword(
                        currentPassword = currentPassword.text.toString(),
                        newPassword = newPassword.text.toString()
                    )
                }
            }
            .done { controller.onSuccess() }
            .ensure { controller.hideProgressBar() }
            .catch { error ->
                when(error) {
                    is AccountError.InvalidPassword ->
                        newPasswordLayout.error = getString(R.string.password_rule_generic_invalid_password)
                    is AccountError.InvalidPasswordResetCode ->
                        controller.makeSnackbar().setText(R.string.forgot_password_invalid_reset_code).show()
                    is AccountError.InvalidPasswordResetToken ->
                        controller.makeSnackbar().setText(R.string.forgot_password_reset_token_expired).show()
                    is KubotaServiceError.NetworkConnectionLost,
                    is KubotaServiceError.NotConnectedToInternet ->
                        controller.makeSnackbar().setText(R.string.connectivity_error_message).show()
                    else ->
                        controller.makeSnackbar().setText(R.string.server_error_message).show()
                }
            }
    }

    override fun areFieldsValid(): Boolean {
        return when (controller.getMode() == AccountSetUpController.NEW_PASSWORD_FLOW) {
            true -> currentPassword.text.isNotEmpty() && super.areFieldsValid()
            else -> verificationCodeEditText.text.isNotEmpty() && super.areFieldsValid()
        }
    }

    private fun initializeUI(view: View) {
        actionButton = view.findViewById(R.id.nextButton)
        resetPasswordHeader = view.findViewById(R.id.resetPasswordHeader)
        verificationCodeLayout = view.findViewById(R.id.verificationCodeLayout)
        verificationCodeEditText = view.findViewById(R.id.verificationCodeEditText)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        currentPasswordLayout = view.findViewById(R.id.passwordInputLayout)
        currentPassword = view.findViewById(R.id.passwordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)

        if (controller.getMode() == AccountSetUpController.NEW_PASSWORD_FLOW) {
            activity?.title = getString(R.string.new_password)
            resetPasswordHeader.visibility = View.GONE
            verificationCodeLayout.visibility = resetPasswordHeader.visibility
        } else {
            currentPasswordLayout.visibility = View.GONE
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
    }

    private fun signIn(): Promise<Unit> {
        // FIXME: Handle signin
        return Promise.value(Unit)
    }

}