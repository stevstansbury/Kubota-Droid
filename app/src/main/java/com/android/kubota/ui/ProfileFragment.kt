package com.android.kubota.ui

import android.app.Dialog
import androidx.lifecycle.Observer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AlertDialog
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.ensure

class ProfileFragment : BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_profile

    private lateinit var changePasswordButton: View
    private lateinit var guestLayout: View
    private lateinit var loggedInLayout: View
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun initUi(view: View) {
        changePasswordButton = view.findViewById<LinearLayout>(R.id.changePasswordListItem)
        guestLayout = view.findViewById<LinearLayout>(R.id.guestLinearLayout)
        loggedInLayout = view.findViewById<LinearLayout>(R.id.loggedInLinearLayout)
        userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)

        changePasswordButton.setOnClickListener {
            (activity as? AccountController)?.changePassword()
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
            (activity as? AccountController)?.createAccount()
        }

        activity?.setTitle(R.string.profile_title)
    }

    override fun loadData() {
        AppProxy.proxy.accountManager.isAuthenticated.observe(viewLifecycleOwner, Observer{ isUserLoggedIn ->
            activity?.invalidateOptionsMenu()
            changePasswordButton.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
            loggedInLayout.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
            guestLayout.visibility = if (isUserLoggedIn) View.GONE else View.VISIBLE
            flowActivity?.hideProgressBar()

            val username = AppProxy.proxy.accountManager.account?.username
            if (username?.matches(Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")) == true) {
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
        menu.findItem(R.id.sign_out)?.isVisible = isUserLoggedIn
        menu.findItem(R.id.sign_in)?.isVisible = isUserLoggedIn.not()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.sign_in -> {
                (activity as? AccountController)?.signIn()
                return true
            }
            R.id.sign_out -> {
                SignOutDialogFragment.show(parentFragmentManager)
                return true
            }

        }

        return super.onOptionsItemSelected(item)
    }
}

class SignOutDialogFragment: DialogFragment() {

    companion object {
        private const val TAG = "SignOutDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            val fragment = SignOutDialogFragment()
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        return AlertDialog.Builder(requireContext())
            .setMessage(R.string.sign_out_dialog_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AppProxy.proxy.accountManager.logout().ensure { dismiss() }.catch {
                    Log.e("ProfileFragment", it.message ?: "")
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss()}
            .create()
    }
}