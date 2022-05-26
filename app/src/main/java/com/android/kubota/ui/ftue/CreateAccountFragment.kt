package com.android.kubota.ui.ftue

import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.PatternsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.account.AccountError
import com.android.kubota.utility.EmailTextWatcher
import com.android.kubota.viewmodel.ftue.CreateAccountViewModel
import com.google.android.material.textfield.TextInputLayout
import com.kubota.service.api.KubotaServiceError

private const val PASSWORD_ARGUMENT = "password"
private const val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"

class CreateAccountFragment: NewPasswordSetUpFragment<CreateAccountController>() {

    private lateinit var emailField: EditText
    private lateinit var phoneNumber: EditText

    private var validEmail = false
    private val viewModel: CreateAccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.title = getString(R.string.create_account)
        val view = inflater.inflate(R.layout.fragment_create_account, null)

        val emailInputLayout = view.findViewById<TextInputLayout>(R.id.emailInputLayout)
        emailField = view.findViewById(R.id.emailEditText)
        phoneNumber = view.findViewById(R.id.phoneNumberEditText)
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)
        newPassword = view.findViewById(R.id.newPasswordEditText)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        confirmPassword = view.findViewById(R.id.confirmPasswordEditText)
        passwordRulesLayout = view.findViewById(R.id.passwordRulesLayout)
        actionButton = view.findViewById(R.id.createAccountButton)

        emailField.addTextChangedListener(
            EmailTextWatcher(emailField) { isValidEmail ->
                validEmail = isValidEmail
                emailInputLayout.error?.let {
                    emailInputLayout.error = null
                }
                updateActionButton()
            }
        )

        phoneNumber.addTextChangedListener {
            updateActionButton()
        }

        val termsAndConditionsLink = view.findViewById<TextView>(R.id.termsAndConditionsLink)
        val linkPortion = getString(R.string.kubota_terms_and_conditions_link)
        val fullText = getString(R.string.create_account_terms_conditions_link, linkPortion)
        val startIdx = fullText.indexOf(linkPortion)
        val endIdx = startIdx + linkPortion.length
        val spannableString = SpannableString(fullText)
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                controller.onNavigateToLegalTerms()
            }
        }, startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        termsAndConditionsLink.text = spannableString
        termsAndConditionsLink.movementMethod = LinkMovementMethod.getInstance()

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            when (isLoading) {
                true -> controller.showProgressBar()
                else -> controller.hideProgressBar()
            }
        })

        viewModel.accountCreated.observe(viewLifecycleOwner, Observer { isCreated ->
            if (isCreated) {
                controller.hideProgressBar()
                controller.onSuccess()
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            controller.hideProgressBar()
            when (error) {
                null -> {
                    emailInputLayout.error = ""
                    newPasswordLayout.error = ""
                }
                is AccountError.AccountExists ->
                    emailInputLayout.error = getString(R.string.duplicate_account_error)
                is AccountError.BlacklistedPassword ->
                    newPasswordLayout.error = getString(R.string.password_rule_blacklisted)
                is AccountError.InvalidPassword ->
                    newPasswordLayout.error =
                        getString(R.string.password_rule_generic_invalid_password)
                is KubotaServiceError.NotConnectedToInternet,
                is KubotaServiceError.NetworkConnectionLost ->
                    newPasswordLayout.error = getString(R.string.connectivity_error_message)
                is KubotaServiceError.ServerMaintenance ->
                    newPasswordLayout.error = getString(R.string.server_maintenance)
                else ->
                    newPasswordLayout.error = getString(R.string.server_error_message)
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun onActionButtonClicked() {
        actionButton.isEnabled = false
        controller.showProgressBar()
        viewModel.createAccount(emailField.text.toString(), newPassword.text.toString(), phoneNumber.text.toString())
    }

    override fun areFieldsValid(): Boolean {
        return super.areFieldsValid() && validEmail && phoneNumber.text.isNotEmpty()
    }
}

enum class Country(@StringRes val countryNameResId: Int, @DrawableRes val flagResId: Int)  {
    US(R.string.usa_country_code, R.drawable.ic_usa_flag),
    JAPAN(R.string.jp_country_code, R.drawable.ic_jp_flag)
}

class CountryAdapter(val data: List<Country>, val clickListener: (country: Country)-> Unit): RecyclerView.Adapter<CountryAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.view_country_adapter_item,
                parent,
                false
            )
        return Holder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val country = data[position]

        holder.textView.let {
            it.setText(country.countryNameResId)
            val d = ResourcesCompat.getDrawable(it.context.resources, country.flagResId, null)
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ResourcesCompat.getDrawable(it.context.resources, country.flagResId, null),
                null,
                null,
                null
            )
        }

        holder.item.setOnClickListener {
            clickListener.invoke(country)
        }
    }

    data class Holder(val item: View) : RecyclerView.ViewHolder(item) {
        val textView: TextView = itemView.findViewById(R.id.countryName)
    }
}