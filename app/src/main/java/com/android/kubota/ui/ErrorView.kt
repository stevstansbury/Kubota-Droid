package com.android.kubota.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.android.kubota.R

class ErrorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    enum class Mode {
        WEB_FAIL,
        GENERIC_ERROR
    }

    private var errorTextView: TextView

    var mode: Mode = Mode.GENERIC_ERROR
        set(value) {
            field = value
            val textId = when (value) {
                Mode.WEB_FAIL -> R.string.web_page_error_message
                Mode.GENERIC_ERROR -> R.string.server_error_message
            }

            errorTextView.setText(textId)
        }

    init {
        View.inflate(context, R.layout.error_view, this)
        errorTextView = findViewById(R.id.errorTextView)
    }

}
