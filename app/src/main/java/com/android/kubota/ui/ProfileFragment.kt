package com.android.kubota.ui

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.createNotificationDialog
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.promisekt.Promise

class ProfileFragment : BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_profile

    private lateinit var changePasswordButton: View
    private lateinit var settings: View
    private lateinit var mfa: View
    private lateinit var signOut: View
    private lateinit var guestLayout: View
    private lateinit var loggedInLayout: View
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun initUi(view: View) {
        changePasswordButton = view.findViewById<LinearLayout>(R.id.changePasswordListItem)
        settings = view.findViewById<LinearLayout>(R.id.settingsListItem)
        mfa = view.findViewById<LinearLayout>(R.id.mutlFactorAuthListItem)
        signOut = view.findViewById<LinearLayout>(R.id.signOutListItem)
        guestLayout = view.findViewById<LinearLayout>(R.id.guestLinearLayout)
        loggedInLayout = view.findViewById<LinearLayout>(R.id.loggedInLinearLayout)
        userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)

        view.findViewById<Button>(R.id.createAccountButton).setOnClickListener {
            (activity as? AccountController)?.createAccount()
        }

        changePasswordButton.setOnClickListener {
            (activity as? AccountController)?.changePassword()
        }

        settings.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ProfileSettingsFragment())
        }

        mfa.setOnClickListener {
            // TODO: Show MFA fragment
        }

        view.findViewById<LinearLayout>(R.id.aboutListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(AboutFragment())
        }

        view.findViewById<LinearLayout>(R.id.legalTermsListItem).setOnClickListener {
            flowActivity?.addFragmentToBackStack(LegalTermsFragment())
        }

        view.findViewById<LinearLayout>(R.id.kubotaUSAListItem).setOnClickListener {
            MessageDialogFragment.showMessage(
                manager = this.parentFragmentManager,
                titleId = R.string.leave_app_dialog_title,
                messageId = R.string.leave_app_kubota_usa_website_msg
            ) { button ->
                if (button == AlertDialog.BUTTON_POSITIVE) {
                    this.context?.let {
                        CustomTabsIntent.Builder()
                            .build()
                            .launchUrl(it, Uri.parse("https://www.kubotausa.com"))
                    }
                }
                Promise.value(Unit)
            }
        }

        signOut.setOnClickListener {
            MessageDialogFragment.showMessage(
                manager = this.parentFragmentManager,
                titleId = R.string.sign_out_dialog_title,
                messageId = R.string.sign_out_dialog_message
            ) { button ->
                if (button == AlertDialog.BUTTON_POSITIVE) {
                    AppProxy.proxy.accountManager.logout()
                } else {
                    Promise.value(Unit)
                }
            }
        }

        activity?.setTitle(R.string.profile_title)
    }

    override fun loadData() {
        this.flowActivity?.hideProgressBar()
        AppProxy.proxy.accountManager.isAuthenticated.observe(
            viewLifecycleOwner,
            Observer { isUserLoggedIn ->
                activity?.invalidateOptionsMenu()
                changePasswordButton.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
                settings.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
                mfa.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
                signOut.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
                loggedInLayout.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
                guestLayout.visibility = if (isUserLoggedIn) View.GONE else View.VISIBLE

                val username = AppProxy.proxy.accountManager.account?.username
                if (username?.isNotBlank() == true) {
                    userNameTextView.text = username
                    userNameTextView.visibility = View.VISIBLE
                } else {
                    userNameTextView.visibility = View.GONE
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.profile_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val isUserLoggedIn = AppProxy.proxy.accountManager.isAuthenticated.value ?: false
        menu.findItem(R.id.sign_in)?.isVisible = isUserLoggedIn.not()
        menu.findItem(R.id.notifications)?.isVisible = isUserLoggedIn
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_in -> {
                (activity as? AccountController)?.signIn()
                return true
            }
            R.id.notifications -> {
                createNotificationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
