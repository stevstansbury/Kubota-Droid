package com.android.kubota.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.kubota.R

class MyDealersListFragment() : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.my_dealer_list_title)

        return inflater.inflate(R.layout.fragment_my_dealers_list, null)
    }

}