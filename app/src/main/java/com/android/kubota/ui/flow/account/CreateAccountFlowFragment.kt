package com.android.kubota.ui.flow.account

import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.PatternsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.account.AccountError
import com.android.kubota.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputLayout
import com.kubota.service.api.KubotaServiceError


enum class Country(@StringRes val countryNameResId: Int, @DrawableRes val flagResId: Int)  {
    US(R.string.usa_country_code, R.drawable.ic_usa_flag),
    JAPAN(R.string.jp_country_code, R.drawable.ic_jp_flag)
}

class CreateAccountFlowFragment
    : BaseAccountPasswordFlowFragment<Throwable?, CreateAccountFlowFragment.Result>() {

    companion object {
        const val EMAIL_ARGUMENT = "account_email"
        private val PASSWORD_ARGUMENT = "password"
        private val CONFIRM_PASSWORD_ARGUMENT = "confirm_password"
    }

    sealed class Result {
        class CreateAccount(val email: String, val password: String): Result()
        object TermsAndConditions: Result()
    }

    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailField: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var countrySpinner: View

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
        newPasswordLayout = view.findViewById(R.id.newPasswordInputLayout)

        val countryImage: ImageView = view.findViewById(R.id.countryImage)
        val cardView = view.findViewById<View>(R.id.cardView)
        view.findViewById<RecyclerView>(R.id.countriesList).apply {
            this.addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
            this.adapter = CountryAdapter(arrayListOf(Country.US, Country.JAPAN)) {
                countryImage.setImageResource(it.flagResId)
                cardView.visibility = View.GONE
            }
        }

        countrySpinner = view.findViewById(R.id.countrySpinner)
        countrySpinner.setOnClickListener {
            cardView.visibility = View.VISIBLE
        }

        emailField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validEmail = when(s) {
                    null -> false
                    else -> s.matches(PatternsCompat.EMAIL_ADDRESS.toRegex())
                }
                emailInputLayout.error?.let {
                    emailInputLayout.error = null
                }
                updateActionButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        phoneNumber.addTextChangedListener {
            updateActionButton()
        }

        view.findViewById<TextView>(R.id.termsAndConditionsLink).apply {
            val linkPortion = getString(R.string.kubota_terms_and_conditions_link)
            val fullText = getString(R.string.create_account_terms_conditions_link, linkPortion)
            val startIdx = fullText.indexOf(linkPortion)
            val endIdx = startIdx + linkPortion.length

            val spannableString = SpannableString(fullText)
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    actionButton.hideKeyboard()
                    resolve(Result.TermsAndConditions)
                }
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

    private fun updateView(input: Throwable?) {
        when (input) {
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
            else ->
                newPasswordLayout.error = getString(R.string.server_error_message)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(EMAIL_ARGUMENT, emailField.text)
        outState.putCharSequence(PASSWORD_ARGUMENT, newPassword.text)
        outState.putCharSequence(CONFIRM_PASSWORD_ARGUMENT, confirmPassword.text)
    }

    override fun onActionButtonClicked() {
        actionButton.hideKeyboard()
        actionButton.isEnabled = false
        resolve(Result.CreateAccount(email = emailField.text.toString(), password = newPassword.text.toString()))
    }

    override fun areFieldsValid(): Boolean {
        if (!super.areFieldsValid()) return false
        return  validEmail && phoneNumber.text.isNotEmpty()
    }

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
