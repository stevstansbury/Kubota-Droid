package com.android.kubota.ui

import android.view.View
import com.android.kubota.R
import com.android.kubota.app.AppProxy

class LegalTermsFragment : BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_legal_terms

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.setTitle(R.string.legal_terms_fragment_title)
        }
    }

    override fun initUi(view: View) {
        activity?.setTitle(R.string.legal_terms_fragment_title)
        view.findViewById<View>(R.id.termsOfUseListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(LegalMode.TERMS_OF_USE_MODE))
        }
        view.findViewById<View>(R.id.privacyPolicyListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(LegalMode.PRIVACY_POLICY_MODE))
        }
        view.findViewById<View>(R.id.disclaimerListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(DisclaimerFragment())
        }

        val californiaLegal = view.findViewById<View>(R.id.californiaListItem)
        when (AppProxy.proxy.currentLocale.country) {
            "US" -> californiaLegal.setOnClickListener {
                flowActivity?.addFragmentToBackStack(WebViewFragment.createInstance(LegalMode.CALIFORNIA_MODE))
            }
            else -> californiaLegal.visibility = View.GONE
        }
    }

    override fun loadData() {

    }
}