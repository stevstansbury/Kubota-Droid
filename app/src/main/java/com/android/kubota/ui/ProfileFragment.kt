package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.Fragment
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.createAccount
import com.android.kubota.extensions.getPublicClientApplication
import com.android.kubota.extensions.hideProgressBar
import com.android.kubota.extensions.showProgressBar
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ProfileViewModel
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.exception.MsalException

class ProfileFragment(): Fragment() {

    private lateinit var viewModel: ProfileViewModel

    private val callback = object : AuthenticationCallback {

        override fun onSuccess(authenticationResult: AuthenticationResult?) {
            authenticationResult?.let {
                viewModel.addUser(it)
            }
        }

        override fun onCancel() {
            activity?.hideProgressBar()
        }

        override fun onError(exception: MsalException?) {
            activity?.hideProgressBar()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideProfileViewModelFactory(context!!)
        viewModel = ViewModelProviders.of(this, factory).get(ProfileViewModel::class.java)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, null)

        val changePasswordButton = view.findViewById<LinearLayout>(R.id.changePasswordListItem)
        val guestLinearLayout = view.findViewById<LinearLayout>(R.id.guestLinearLayout)
        val loggedInLinearLayout = view.findViewById<LinearLayout>(R.id.loggedInLinearLayout)
        viewModel.isUserLoggedIn.observe(this, Observer {
            if (it != null && it) {
                changePasswordButton.visibility = View.VISIBLE
                guestLinearLayout.visibility = View.GONE
                loggedInLinearLayout.visibility = View.VISIBLE
            } else {
                changePasswordButton.visibility = View.GONE
                guestLinearLayout.visibility = View.VISIBLE
                loggedInLinearLayout.visibility = View.GONE
            }
            activity?.invalidateOptionsMenu()
            activity?.hideProgressBar()
        })
        val userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        viewModel.userName.observe(this, Observer { userNameTextView.text = it })

        view.findViewById<LinearLayout>(R.id.myEquipmentListItem).setOnClickListener {  }
        view.findViewById<LinearLayout>(R.id.myDealersListItem).setOnClickListener {  }
        view.findViewById<LinearLayout>(R.id.aboutListItem).setOnClickListener {  }
        view.findViewById<LinearLayout>(R.id.legalTermsListItem).setOnClickListener {  }
        view.findViewById<LinearLayout>(R.id.kubotaUSAListItem).setOnClickListener {
            context?.let {
                CustomTabsIntent.Builder()
                    .build()
                    .launchUrl(it, Uri.parse("https://www.kubotausa.com"))
            }
        }

        view.findViewById<Button>(R.id.createAccountButton).setOnClickListener {
            activity?.let {
                it.getPublicClientApplication().createAccount(it, callback)
            }
        }


        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.profile_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)

        menu?.findItem(R.id.sign_out)?.isVisible = viewModel.isUserLoggedIn.value ?: true
        menu?.findItem(R.id.sign_in)?.isVisible = viewModel.isUserLoggedIn.value?.not() ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.sign_in -> {
                activity?.let {
                    activity?.showProgressBar()
                    it.getPublicClientApplication().createAccount(it, callback)
                }
                return true
            }
            R.id.sign_out -> {
                activity?.showProgressBar()
                viewModel.logout()
                return true
            }

        }

        return super.onOptionsItemSelected(item)
    }
}