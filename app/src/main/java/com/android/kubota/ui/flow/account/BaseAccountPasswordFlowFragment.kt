package com.android.kubota.ui.flow.account

import android.app.Application
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.ui.ftue.PasswordRulesLayout
import com.android.kubota.utility.PasswordUtils
import com.android.kubota.viewmodel.equipment.getString
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.flowkit.android.FlowFragment

fun View.onLoseFocus( listener: () -> Unit ) {
    this.setOnFocusChangeListener { view, willFocus ->
        if (!willFocus) {
            listener()
        }
    }
}

class PasswordViewModel(application: Application): AndroidViewModel(application) {
    val password1 = MutableLiveData<String>("")

    val password2 = MutableLiveData<String>("")
    val error2 = MutableLiveData<String?>(null)

    fun validatePasswords() {
        val pass1 = password1.value ?: ""

        error2.value = null
        val pass2 = password2.value ?: ""

        if (!TextUtils.equals(pass1, pass2)) {
            error2.value = getString(R.string.matching_password_error)
        }
    }

    fun checkPassword(): Boolean {
        val pass1 = password1.value ?: ""
        val pass2 = password2.value ?: ""
        return PasswordUtils.isValidPassword(pass1) && TextUtils.equals(pass1, pass2)
    }
}

abstract class BaseAccountPasswordFlowFragment<Input, Output>
    : FlowFragment<Input, Output>() {

    protected lateinit var actionButton: Button
    protected lateinit var newPassword: EditText
    protected lateinit var newPasswordLayout: TextInputLayout
    protected lateinit var confirmPasswordLayout: TextInputLayout
    protected lateinit var confirmPassword: EditText
    protected lateinit var passwordRulesLayout: PasswordRulesLayout

    protected val viewModel by lazy {
        ViewModelProvider(this.requireActivity()).get(PasswordViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actionButton.setOnClickListener { onActionButtonClicked() }

        newPassword.addTextChangedListener(afterTextChanged={
            viewModel.password1.value = it?.toString() ?: ""
//            newPasswordLayout.error = null
            updateActionButton()
        })

        confirmPassword.addTextChangedListener(afterTextChanged={
            viewModel.password2.value = it?.toString() ?: ""
//            confirmPassword.error = null
            updateActionButton()
        })

        viewModel.error2.observe(viewLifecycleOwner, Observer {
            confirmPasswordLayout.error = it
        })

        confirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                viewModel.validatePasswords()
                if (actionButton.isEnabled) {
                    onActionButtonClicked()
                }
            }
            false
        }

        newPassword.onLoseFocus {
            viewModel.validatePasswords()
            passwordRulesLayout.showDialogIfInvalidPassword(
                password = newPassword.text?.toString() ?: "",
                fragmentManager = parentFragmentManager
            )
        }

        confirmPassword.onLoseFocus {
            viewModel.validatePasswords()
        }
    }

    abstract fun onActionButtonClicked()
    protected open fun areFieldsValid(): Boolean = this.viewModel.checkPassword()

    protected open fun updateActionButton() {
        passwordRulesLayout.verifyNewPassword(viewModel.password1.value ?: "")
        actionButton.isEnabled = areFieldsValid()
    }
}
