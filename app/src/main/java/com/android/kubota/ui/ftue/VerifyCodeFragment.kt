package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.BaseAccountSetUpFragment

const val VERIFY_CODE = "verify_code"

class VerifyCodeFragment: BaseAccountSetUpFragment<VerifyCodeController>() {

    private lateinit var verifyCode: EditText
    private lateinit var sendNewCode: View

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val isEnabled = when(s) {
                null -> false
                else -> s.isNotBlank() && s.isNotEmpty()
            }
            actionButton.isEnabled = isEnabled
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        activity?.setTitle(R.string.forgot_password)

        val view = inflater.inflate(R.layout.fragment_verify_code, null)

        actionButton = view.findViewById(R.id.nextButton)
        verifyCode = view.findViewById(R.id.verificationCodeEditText)
        sendNewCode = view.findViewById(R.id.sendNewCodeTextView)

        savedInstanceState?.getString(VERIFY_CODE)?.let {
            verifyCode.setText(it)
        }

        verifyCode.addTextChangedListener(textWatcher)

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(VERIFY_CODE, verifyCode.text)
    }

    override fun onActionButtonClicked() {
        verifyCode.hideKeyboard()
        controller.onSuccess(verifyCode.text.toString())
    }
}