package com.android.kubota.utility

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.util.PatternsCompat

class EmailTextWatcher(
    private val editText: EditText,
    private var listener: ((isValidEmail: Boolean) -> Unit)? = null
) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        val textEntered = s.toString()

        if (textEntered.isNotEmpty() && textEntered.contains(" ")) {
            editText.setText(editText.text.toString().replace(" ", ""))
            editText.setSelection(editText.text?.length ?: 0)
        }

        val validEmail = editText.text?.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) ?: false
        listener?.invoke(validEmail)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}