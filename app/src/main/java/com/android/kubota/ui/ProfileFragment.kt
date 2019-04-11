package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.createAccount
import com.android.kubota.extensions.login
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ProfileViewModel
import com.kubota.repository.ext.getPublicClientApplication
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.exception.MsalException

class ProfileFragment(): BaseFragment() {

    private lateinit var viewModel: ProfileViewModel

    private val callback = object : AuthenticationCallback {

        override fun onSuccess(authenticationResult: AuthenticationResult?) {
            authenticationResult?.let {
                viewModel.addUser(it)
            }
        }

        override fun onCancel() {
            flowActivity?.hideProgressBar()
        }

        override fun onError(exception: MsalException?) {
            flowActivity?.hideProgressBar()
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
            flowActivity?.hideProgressBar()
        })
        val userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        viewModel.userName.observe(this, Observer {
            if (it?.matches(Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")) == true) {
                userNameTextView.text = it
                userNameTextView.visibility = View.VISIBLE
            } else {
                userNameTextView.visibility = View.GONE
            }
        })

        view.findViewById<LinearLayout>(R.id.myEquipmentListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(MyEquipmentsListFragment())
        }
        view.findViewById<LinearLayout>(R.id.myDealersListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(MyDealersListFragment())
        }
        view.findViewById<LinearLayout>(R.id.aboutListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(AboutFragment())
        }
        view.findViewById<LinearLayout>(R.id.legalTermsListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(LegalTermsFragment())
        }
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

        activity?.setTitle(R.string.profile_title)

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
                    flowActivity?.showProgressBar()
                    it.getPublicClientApplication().login(it, callback)
                }
                return true
            }
            R.id.sign_out -> {
                flowActivity?.showProgressBar()
                viewModel.logout()
                return true
            }

        }

        return super.onOptionsItemSelected(item)
    }
}