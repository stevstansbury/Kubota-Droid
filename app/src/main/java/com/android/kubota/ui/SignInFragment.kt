package com.android.kubota.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.util.PatternsCompat
import com.android.kubota.R

class SignInFragment: BaseFragment() {

    private var validEmail = false
    private var validPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.setTitle(R.string.sign_in)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        view.findViewById<TextView>(R.id.forgotPasswordTextView).setOnClickListener {

        }

        val signInButton = view.findViewById<Button>(R.id.signInButton)
        view.findViewById<EditText>(R.id.emailEditText).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validEmail = when(s) {
                    null -> false
                    else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
                }

                signInButton.isEnabled = shouldEnabledSignInButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        view.findViewById<EditText>(R.id.passwordEditText).addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                validPassword = when (s) {
                    null -> false
                    else -> s.isNotBlank() && s.isNotEmpty()
                }

                signInButton.isEnabled = shouldEnabledSignInButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        return view
    }

    private fun shouldEnabledSignInButton() = validEmail && validPassword
}