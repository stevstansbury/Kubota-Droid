package com.android.kubota.ui.ftue

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.fragment.app.FragmentManager
import com.android.kubota.R
import com.android.kubota.utility.MessageDialogFragment
import com.android.kubota.utility.PasswordUtils
import com.inmotionsoftware.promisekt.cauterize

class PasswordRulesLayout : FrameLayout {
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

    fun showDialogIfInvalidPassword(password: String, fragmentManager: FragmentManager) {
        val msgId = when {
            !PasswordUtils.hasAtLeast8Characters(password) -> R.string.password_rule_character_limit_msg
            !PasswordUtils.containsAlphaCharacter(password) -> R.string.password_rule_alpha_character_msg
            !PasswordUtils.containsNumericCharacter(password) -> R.string.password_rule_numeric_character_msg
            !PasswordUtils.containsASymbol(password) -> R.string.password_rule_symbol_character_msg
            else -> return
        }

        MessageDialogFragment.showSimpleMessage(
            manager = fragmentManager,
            title = context.getString(R.string.invalid_password),
            message = context.getString(msgId)
        ).cauterize()
    }

    private fun updateDrawable(textView: TextView, isRuleMet: Boolean) {
        val drawableResId = if (isRuleMet) R.drawable.password_rule_met else R.drawable.password_rule_not_met
        textView.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0)
    }
}