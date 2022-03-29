package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.util.PatternsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
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

    private fun validateEmail(editable: Editable?) {
        val isEnabled = editable?.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) ?: false
        actionButton.isEnabled = isEnabled
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_forgot_password, container,false)
        actionButton = view.findViewById(R.id.nextButton)
        actionButton.setOnClickListener { onActionButtonClicked() }

        emailAddress = view.findViewById(R.id.emailEditText)
        emailAddress.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val textEntered = emailAddress.text.toString()

                if(textEntered.isNotEmpty() && textEntered.contains(" ")){
                    emailAddress.setText(emailAddress.text.toString().replace(" ", ""))
                    emailAddress.setSelection(emailAddress.text!!.length)
                }
                validateEmail(emailAddress.text)
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })
        validateEmail(emailAddress.text)

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
