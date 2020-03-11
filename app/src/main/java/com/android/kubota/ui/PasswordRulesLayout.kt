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
    private val lengthRuleTextView: TextView
    private val alphaCharacterRuleTextView: TextView
    private val numericCharacterRuleTextView: TextView
    private val symbolCharacterRuleTextView: TextView

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.view_password_rules, this)

        lengthRuleTextView = view.findViewById(R.id.characterLimitRule)
        alphaCharacterRuleTextView = view.findViewById(R.id.alphaCharacterRule)
        numericCharacterRuleTextView = view.findViewById(R.id.numericCaseRule)
        symbolCharacterRuleTextView = view.findViewById(R.id.symbolCharacterRule)
    }

    fun verifyNewPassword(password: String) {
        updateDrawable(lengthRuleTextView, PasswordUtils.hasAtLeast8Characters(password))
        updateDrawable(alphaCharacterRuleTextView, PasswordUtils.containsAlphaCharacter(password))
        updateDrawable(numericCharacterRuleTextView, PasswordUtils.containsNumericCharacter(password))
        updateDrawable(symbolCharacterRuleTextView, PasswordUtils.containsASymbol(password))
    }

    private fun updateDrawable(textView: TextView, isRuleMet: Boolean) {
        val drawableResId = if (isRuleMet) R.drawable.password_rule_met else R.drawable.password_rule_not_met
        textView.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0)
    }
}