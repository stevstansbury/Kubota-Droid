package com.android.kubota.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.android.kubota.R

class MockNavigationFragment(): Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mock, null)
        view.findViewById<Button>(R.id.homeButton).setOnClickListener {
            //Launch home screen here
            fragmentManager?.beginTransaction()?.replace(R.id.fragmentPane, HomeFragment())?.addToBackStack(null)?.commit()
        }
        view.findViewById<Button>(R.id.manualsButton).setOnClickListener{
            //Launch manual screen here
        }
        view.findViewById<Button>(R.id.guidesButton).setOnClickListener{
            //Launch guide screen here
        }
        view.findViewById<Button>(R.id.dealerLocatorButton).setOnClickListener{
            //Launch Dealer Locator screen here
        }
        view.findViewById<Button>(R.id.profileButton).setOnClickListener{
            //Launch Profile screen here
            fragmentManager?.beginTransaction()?.replace(R.id.fragmentPane, ProfileFragment())?.addToBackStack(null)?.commit()
        }
        return view
    }
}