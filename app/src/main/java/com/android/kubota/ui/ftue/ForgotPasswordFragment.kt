package com.android.kubota.ui.ftue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.EmailTextWatcher
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.KubotaServiceError

class ForgotPasswordFragment: BaseAccountSetUpFragment<ForgotPasswordController>() {
    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailAddress: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, null)
        setupUI(view)

        when {
            savedInstanceState != null -> savedInstanceState
            arguments != null -> arguments
            else -> null
        }?.let { emailAddress.setText(it.getCharSequence(EMAIL_ARGUMENT, "")) }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(EMAIL_ARGUMENT, emailAddress.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        controller.showProgressBar()

        AuthPromise()
            .then {
                AppProxy.proxy.accountManager.sendForgotPasswordVerificationCode(email = emailAddress.text.toString())
            }
            .done { controller.onSuccess(it.token) }
            .ensure { controller.hideProgressBar() }
            .catch { error ->
                when (error) {
                    is AccountError.InvalidEmail ->
                        // FIXME: We should not display this error.
                        // Just tell the user a verification code has been sent to the email address
                        emailLayout.error = getString(R.string.email_incorrect_error)
                    is KubotaServiceError.NotConnectedToInternet,
                    is KubotaServiceError.NetworkConnectionLost ->
                        controller.makeSnackbar().setText(R.string.connectivity_error_message).show()
                    is KubotaServiceError.ServerMaintenance ->
                        controller.makeSnackbar().setText(R.string.server_maintenance).show()
                    else ->
                        controller.makeSnackbar().setText(R.string.server_error_message).show()
                }
            }
    }

    private fun setupUI(view: View) {
        actionButton = view.findViewById(R.id.nextButton)
        emailLayout = view.findViewById(R.id.emailInputLayout)
        emailAddress = view.findViewById(R.id.emailEditText)
        emailAddress.addTextChangedListener(
            EmailTextWatcher(emailAddress) { isValidEmail ->
                actionButton.isEnabled = isValidEmail
            }
        )
    }
}