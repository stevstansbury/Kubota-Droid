package com.android.kubota.ui

import android.app.Dialog
import androidx.lifecycle.Observer
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AlertDialog
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ProfileViewModel

class ProfileFragment : BaseFragment() {
    override val layoutResId: Int = R.layout.fragment_profile

    private lateinit var viewModel: ProfileViewModel

    private lateinit var changePasswordButton: View
    private lateinit var guestLayout: View
    private lateinit var loggedInLayout: View
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideProfileViewModelFactory(context!!)
        viewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)
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
        viewModel.isUserLoggedIn.observe(viewLifecycleOwner, Observer {isUserLoggedIn ->
            activity?.invalidateOptionsMenu()
            changePasswordButton.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
            loggedInLayout.visibility = if (isUserLoggedIn) View.VISIBLE else View.GONE
            guestLayout.visibility = if (isUserLoggedIn) View.GONE else View.VISIBLE
            flowActivity?.hideProgressBar()
        })

        viewModel.userName.observe(viewLifecycleOwner, Observer {
            if (it?.matches(Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")) == true) {
                userNameTextView.text = it
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

        menu.findItem(R.id.sign_out)?.isVisible = viewModel.isUserLoggedIn.value ?: false
        menu.findItem(R.id.sign_in)?.isVisible = viewModel.isUserLoggedIn.value?.not() ?: true
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
                (activity as? AccountController)?.logout()
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss()}
            .create()
    }
}