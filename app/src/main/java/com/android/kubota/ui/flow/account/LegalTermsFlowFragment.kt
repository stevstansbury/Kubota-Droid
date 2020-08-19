package com.android.kubota.ui.flow.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.ui.DisclaimerFragment
import com.android.kubota.ui.LegalMode
import com.android.kubota.ui.WebViewFragment
import com.inmotionsoftware.flowkit.android.FlowFragment

class LegalTermsFlowFragment: FlowFragment<Unit, Unit>() {

    override fun onInputAttached(input: Unit) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().setTitle(R.string.legal_terms_fragment_title)

        val view = inflater.inflate(R.layout.fragment_legal_terms, container, false)
        view.findViewById<View>(R.id.termsOfUseListItem).setOnClickListener {
            pushFragment(WebViewFragment.createInstance(LegalMode.TERMS_OF_USE_MODE))
        }
        view.findViewById<View>(R.id.privacyPolicyListItem).setOnClickListener {
            pushFragment(WebViewFragment.createInstance(LegalMode.PRIVACY_POLICY_MODE))
        }
        view.findViewById<View>(R.id.disclaimerListItem).setOnClickListener {
            pushFragment(DisclaimerFragment())
        }
        return view
    }

    // TODO: Make this an Activity extension for reusable
    private fun pushFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentPane, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

}
