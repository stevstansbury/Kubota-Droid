package com.android.kubota.ui.flow.account

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.coordinator.state.OnboardUserType
import com.inmotionsoftware.flowkit.android.FlowFragment

class OnboardUserFlowFragment: FlowFragment<OnboardUserType, OnboardUserFlowFragment.Result>() {

    enum class Result {
        SIGN_IN,
        CREATE_ACCOUNT,
        SKIP
    }

    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var skipButton: Button

    private var input: MutableLiveData<OnboardUserType> = MutableLiveData()

    override fun onInputAttached(input: OnboardUserType) {
        this.input.postValue(input)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboard_user, container, false)
        titleTextView = view.findViewById(R.id.onboard_title_text_view)
        descriptionTextView = view.findViewById(R.id.onboard_description_text_view)
        imageView = view.findViewById(R.id.onboard_image_view)
        skipButton = view.findViewById(R.id.onboard_skip_btn)
        skipButton.setOnClickListener {
            resolve(Result.SKIP)
        }
        view.findViewById<ImageView>(R.id.btn_dismiss_dialog).setOnClickListener {
            this.resolve(Result.SKIP)
        }
        view.findViewById<Button>(R.id.onboard_create_account_btn).setOnClickListener {
            this.resolve(Result.CREATE_ACCOUNT)
        }

        this.input.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })
        return view
    }

    private fun updateView(input: OnboardUserType) {
        titleTextView.setText(input.title )
        descriptionTextView.apply {
            val s1 = getString(input.description)
            val s2 = SpannableString(getString(R.string.onboard_sign_in))
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    resolve(Result.SIGN_IN)
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }
            s2.setSpan(clickableSpan, 0, s2.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            this.text = TextUtils.concat(s1, " ", s2)
            this.movementMethod = LinkMovementMethod.getInstance()
            this.highlightColor = Color.TRANSPARENT
        }
        imageView.setImageDrawable(this.requireContext().getDrawable(input.image))
        skipButton.apply {
            when (input) {
                OnboardUserType.ADD_EQUIPMENT -> this.setText(R.string.skip)
                OnboardUserType.FAVORITE_DEALER -> this.setText(R.string.cancel)
            }
        }
    }

}

val OnboardUserType.title: Int
    get() {
        return when (this) {
            OnboardUserType.ADD_EQUIPMENT -> R.string.onboard_sign_in_title
            OnboardUserType.FAVORITE_DEALER -> R.string.onboard_favorite_dealer_title
        }
    }

val OnboardUserType.description: Int
    get() {
        return when (this) {
            OnboardUserType.ADD_EQUIPMENT -> R.string.onboard_add_equipment_description
            OnboardUserType.FAVORITE_DEALER -> R.string.onboard_favorite_dealer_description
        }
    }

val OnboardUserType.image: Int
    get() {
        return when (this) {
            OnboardUserType.ADD_EQUIPMENT -> R.drawable.onboard_sign_in
            OnboardUserType.FAVORITE_DEALER -> R.drawable.onboard_dealer
        }
    }
