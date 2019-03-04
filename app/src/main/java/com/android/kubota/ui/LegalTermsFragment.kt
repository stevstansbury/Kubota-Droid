package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R

class LegalTermsFragment(): BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.legal_terms_fragment_title)
        val view = inflater.inflate(R.layout.fragment_legal_terms, null)
        setOnClickListeners(view = view)

        return view
    }

    private fun setOnClickListeners(view: View) {
        view.findViewById<View>(R.id.termsOfUseListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(WebViewFragment.TERMS_OF_USE_MODE))
        }
        view.findViewById<View>(R.id.privacyPolicyListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(WebViewFragment.PRIVACY_POLICY_MODE))
        }
        view.findViewById<View>(R.id.disclaimerListItem).setOnClickListener {  }
    }
}