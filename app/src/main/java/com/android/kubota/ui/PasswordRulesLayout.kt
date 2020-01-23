package com.android.kubota.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.kubota.R
import com.android.kubota.utility.PasswordUtils

class PasswordRulesLayout: FrameLayout {
    private lateinit var lengthRuleTextView: TextView
    private lateinit var upperCaseRuleTextView: TextView
    private lateinit var lowerCaseRuleTextView: TextView
    private lateinit var specialCharacterRuleTextView: TextView

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.view_password_rules, this)

        lengthRuleTextView = view.findViewById(R.id.characterLimitRule)
        upperCaseRuleTextView = view.findViewById(R.id.upperCaseRule)
        lowerCaseRuleTextView = view.findViewById(R.id.lowerCaseRule)
        specialCharacterRuleTextView = view.findViewById(R.id.specialCharacterRule)
    }

    fun verifyNewPassword(password: String) {
        updateDrawable(lengthRuleTextView, PasswordUtils.hasAtLeast8Characters(password))
        updateDrawable(upperCaseRuleTextView, PasswordUtils.hasUpperCaseLetter(password))
        updateDrawable(lowerCaseRuleTextView, PasswordUtils.hasLowerCaseLetter(password))
        updateDrawable(specialCharacterRuleTextView, PasswordUtils.hasNumberOrSpecialCharacter(password))
    }

    private fun updateDrawable(textView: TextView, isRuleMet: Boolean) {
        val drawbleResId = if (isRuleMet) R.drawable.password_rule_met else R.drawable.password_rule_not_met
        textView.setCompoundDrawablesWithIntrinsicBounds(drawbleResId, 0, 0, 0)
    }
}