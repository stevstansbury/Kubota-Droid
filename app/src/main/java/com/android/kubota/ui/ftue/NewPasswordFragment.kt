package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.NewPasswordSetUpFragment
import com.android.kubota.viewmodel.ftue.NewPasswordViewModel
import com.android.kubota.viewmodel.ftue.NewPasswordViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import com.kubota.repository.service.Result
import kotlinx.coroutines.launch

class NewPasswordFragment: NewPasswordSetUpFragment<NewPasswordController>() {

    private lateinit var viewModel: NewPasswordViewModel
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var currentPassword: EditText
    private lateinit var code: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        code = requireArguments().getString(VERIFY_CODE, "")
        requireArguments().getString(ACCESS_TOKEN)?.let {
            viewModel = NewPasswordViewModelFactory(it)
                .create(NewPasswordViewModel::class.java)
        } ?: activity?.onBackPressed()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (!::viewModel.isInitialized) return super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_new_password, null)
        initializeUI(view)

        return view
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        actionButton.hideKeyboard()
        controller.showProgressBar()

        viewLifecycleOwner.lifecycleScope.launch {
            val request = controller.createRequest(
                newPassword.text.toString(),
                currentPassword.text.toString()
            )
            onResult(viewModel.changePassword(request))
        }
    }

    override fun areFieldsValid(): Boolean {
        return when (controller.getMode() == AccountSetUpController.NEW_PASSWORD_FLOW) {
            true -> currentPassword.text.isNotEmpty() && super.areFieldsValid()
            else -> super.areFieldsValid()
        }
    }

    private fun initializeUI(view: View) {
        actionButton = view.findViewById(R.id.nextButton)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        currentPasswordLayout = view.findViewById(R.id.passwordInputLayout)
        currentPassword = view.findViewById(R.id.passwordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)

        if (controller.getMode() == AccountSetUpController.NEW_PASSWORD_FLOW) {
            activity?.title = getString(R.string.new_password)
        } else {
            currentPasswordLayout.visibility = View.GONE
            val layoutParams = newPasswordLayout.layoutParams as LinearLayout.LayoutParams
            layoutParams.topMargin = resources.getDimension(R.dimen.current_password_top_margin).toInt()
            newPasswordLayout.layoutParams = layoutParams
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

    private fun onResult(result: Result) {
        controller.hideProgressBar()
        when (result) {
            is Result.Success -> {
                controller.onSuccess()
            }
            is Result.InvalidToken -> {
                controller.makeSnackbar().setText(R.string.session_expired).show()
            }
            is Result.InvalidPassword -> {
                newPasswordLayout.error = getString(R.string.password_rule_generic_invalid_password)
            }
            is Result.NetworkError -> {
                controller.makeSnackbar().setText(R.string.connectivity_error_message).show()
            }
            else -> {
                controller.makeSnackbar().setText(R.string.server_error_message).show()
            }
        }
    }
}