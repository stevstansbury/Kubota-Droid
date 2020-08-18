package com.android.kubota.ui.flow.account

import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.PatternsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputLayout
import com.kubota.service.api.KubotaServiceError

class CreateAccountFlowFragment
    : BaseAccountPasswordFlowFragment<Throwable?, CreateAccountFlowFragment.Result>() {

    companion object {
        const val EMAIL_ARGUMENT = "account_email"
        private val PASSWORD_ARGUMENT = "password"
        private val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"
    }

    sealed class Result {
        class CreateAccount(val email: String, val password: String, val phoneNumber: String): Result()
        object TermsAndConditions: Result()
    }

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailField: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var phoneNumberEditLayout: TextInputLayout

    private var validEmail = false
    private var input: MutableLiveData<Throwable?> = MutableLiveData()

    override fun onInputAttached(input: Throwable?) {
        this.input.postValue(input)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.title = getString(R.string.create_account)
        val view = inflater.inflate(R.layout.fragment_create_account, container, false)

        // Super properties
        actionButton = view.findViewById(R.id.createAccountButton)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)

        // This properties
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        emailField = view.findViewById(R.id.emailEditText)
        phoneNumber = view.findViewById(R.id.phoneNumberEditText)
        phoneNumberEditLayout = view.findViewById(R.id.phoneNumberEditLayout)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)

        emailField.onLoseFocus {
            val text = emailField.text
            val result = text.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
            emailInputLayout.error = if (result) null else getString(R.string.email_incorrect_error)
        }

        emailField.addTextChangedListener(afterTextChanged = {
            validEmail = when(it) {
                null -> false
                else -> it.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
            }
            emailInputLayout.error = null
            updateActionButton()
        })

        phoneNumber.addTextChangedListener {
            updateActionButton()
            phoneNumberEditLayout.error = null
        }

        phoneNumber.onLoseFocus {
            phoneNumberEditLayout.error = if (validatePhone(phoneNumber.text)) null else getString(R.string.phone_incorrect_error)
        }

        phoneNumber.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                phoneNumberEditLayout.error = if (validatePhone(phoneNumber.text)) null else getString(R.string.phone_incorrect_error)
            }

            false
        }

        view.findViewById<TextView>(R.id.termsAndConditionsLink).apply {
            val linkPortion = getString(R.string.kubota_terms_and_conditions_link)
            val fullText = getString(R.string.create_account_terms_conditions_link, linkPortion)
            val startIdx = fullText.indexOf(linkPortion)
            val endIdx = startIdx + linkPortion.length

            this.setOnClickListener {
                actionButton.hideKeyboard()
                resolve(Result.TermsAndConditions)
            }

            val spannableString = SpannableString(fullText)
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {}
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }, startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            this.text = spannableString
            this.movementMethod = LinkMovementMethod.getInstance()
            this.highlightColor = Color.TRANSPARENT
        }

        this.input.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            emailField.setText(it.getCharSequence(EMAIL_ARGUMENT, ""))
            newPassword.setText(it.getCharSequence(PASSWORD_ARGUMENT, ""))
            confirmPassword.setText(it.getCharSequence(CONFIRM_PASSWORD_ARGUMENT, ""))
        }
    }

    private fun focusOn(view: View) {
        view.requestFocus()
        val mgr = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        mgr?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateView(input: Throwable?) {
        when (input) {
            null -> {
                emailInputLayout.error = ""
                newPasswordLayout.error = ""
                focusOn(emailField)
            }
            is AccountError.AccountExists -> {
                emailInputLayout.error = getString(R.string.duplicate_account_error)
                focusOn(emailField)
            }
            is AccountError.BlacklistedPassword -> {
                newPasswordLayout.error = getString(R.string.password_rule_blacklisted)
                focusOn(newPassword)
            }
            is AccountError.InvalidPassword -> {
                newPasswordLayout.error = getString(R.string.password_rule_generic_invalid_password)
                focusOn(newPassword)
            }
            is AccountError.NotMobilePhoneNumber -> {
                phoneNumberEditLayout.error =getString(R.string.not_a_mobile_phone_number)
                focusOn(phoneNumber)
            }
            is AccountError.InvalidPhoneNumber-> {
                phoneNumberEditLayout.error =getString(R.string.invalid_phone_number)
                focusOn(phoneNumber)
            }
            is KubotaServiceError.NotConnectedToInternet,
            is KubotaServiceError.NetworkConnectionLost -> {
                newPasswordLayout.error = getString(R.string.connectivity_error_message)
                focusOn(newPassword)
            }
            else -> {
                newPasswordLayout.error = getString(R.string.server_error_message)
                focusOn(newPassword)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun onActionButtonClicked() {
//        actionButton.hideKeyboard()
        phoneNumber.clearFocus()
        actionButton.isEnabled = false
        resolve(
            Result.CreateAccount(
                email = emailField.text.toString(),
                password = newPassword.text.toString(),
                phoneNumber = phoneNumber.text.toString()
            )
        )
    }

    private fun validatePhone(number: Editable): Boolean = number.length >= 10

    override fun areFieldsValid(): Boolean {
        if (!super.areFieldsValid()) return false
        return  validEmail && validatePhone(phoneNumber.text)
    }

}
