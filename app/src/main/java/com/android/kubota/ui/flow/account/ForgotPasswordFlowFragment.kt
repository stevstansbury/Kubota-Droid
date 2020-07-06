package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.util.PatternsCompat
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.inmotionsoftware.flowkit.android.FlowFragment

class ForgotPasswordFlowFragment: FlowFragment<Unit, ForgotPasswordFlowFragment.Result>() {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
    }

    sealed class Result {
        class SendVerificationCode(val email: String): Result()
    }

    private lateinit var emailAddress: EditText
    private lateinit var actionButton: Button

    override fun onInputAttached(input: Unit) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, container,false)
        actionButton = view.findViewById(R.id.nextButton)
        actionButton.setOnClickListener { onActionButtonClicked() }

        emailAddress = view.findViewById(R.id.emailEditText)
        emailAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isEnabled = s?.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) ?: false
                actionButton.isEnabled = isEnabled
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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

    private fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()

        this.resolve(Result.SendVerificationCode(email = emailAddress.text.toString()))
    }

}
