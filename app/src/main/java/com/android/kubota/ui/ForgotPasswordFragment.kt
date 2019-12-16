package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.util.PatternsCompat
import com.android.kubota.R

class ForgotPasswordFragment: BaseAccountSetUpFragment() {

    private lateinit var emailAddress: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, null)
        emailAddress = view.findViewById(R.id.emailEditText)

        emailAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isEnabled = s?.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) ?: false
                accountSetUpContext.setNextButtonEnable(isEnabled)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

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

    override fun onActionButtonClicked() {
        accountSetUpContext.replaceFragment(VerifyCodeFragment().apply {
            val args =  Bundle(1)
            args.putString(EMAIL_ARGUMENT, emailAddress.text.toString())

            this.arguments = args
        })
    }

    override fun getActionButtonText(): Int = R.string.send_verification_code

    override fun onBack() {
        accountSetUpContext.replaceFragment(SignInFragment())
    }
}