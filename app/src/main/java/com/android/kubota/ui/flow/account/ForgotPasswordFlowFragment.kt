package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.EmailTextWatcher
import com.inmotionsoftware.flowkit.android.FlowFragment

class ForgotPasswordFlowFragment: FlowFragment<String?, ForgotPasswordFlowFragment.Result>() {
    companion object {
        const val EMAIL_ARGUMENT = "account_email"
    }

    sealed class Result {
        class SendVerificationCode(val email: String): Result()
    }

    private lateinit var emailAddress: EditText
    private lateinit var actionButton: Button

    val input = MutableLiveData<String?>()

    override fun onInputAttached(input: String?) {
        this.input.postValue(input)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, container,false)
        actionButton = view.findViewById(R.id.nextButton)
        actionButton.setOnClickListener { onActionButtonClicked() }

        emailAddress = view.findViewById(R.id.emailEditText)
        emailAddress.addTextChangedListener(
            EmailTextWatcher(emailAddress) { isValidEmail ->
                actionButton.isEnabled = isValidEmail
            }
        )

        emailAddress.setText(savedInstanceState?.getCharSequence(EMAIL_ARGUMENT, ""))

        this.input.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })

        return view
    }

    private fun updateView(email: String?) {
        emailAddress.requestFocusWithKeyboard()
        emailAddress.setText(email)
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
