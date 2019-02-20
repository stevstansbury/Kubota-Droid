package com.android.kubota.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R

class LegalTermsFragment(): Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.legal_terms_fragment_title)
        val view = inflater.inflate(R.layout.fragment_legal_terms, null)
        setOnClickListeners(view = view)
        return view
    }

    private fun setOnClickListeners(view: View) {
        view.findViewById<View>(R.id.termsOfUseListItem).setOnClickListener {  }
        view.findViewById<View>(R.id.privacyPolicyListItem).setOnClickListener {  }
        view.findViewById<View>(R.id.disclaimerListItem).setOnClickListener {  }
    }
}