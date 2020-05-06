package com.android.kubota.ui

import android.view.View
import com.android.kubota.R

class LegalTermsFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_legal_terms

    override fun initUi(view: View) {
        activity?.setTitle(R.string.legal_terms_fragment_title)
        view.findViewById<View>(R.id.termsOfUseListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(WebViewFragment.TERMS_OF_USE_MODE))
        }
        view.findViewById<View>(R.id.privacyPolicyListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(WebViewFragment.PRIVACY_POLICY_MODE))
        }
        view.findViewById<View>(R.id.disclaimerListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(DisclaimerFragment())
        }
    }

    override fun loadData() {

    }
}