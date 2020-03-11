package com.android.kubota.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.AccountSetUpContext.Companion.CREATE_ACCOUNT_FLOW
import com.android.kubota.ui.AccountSetUpContext.Companion.NEW_PASSWORD_FLOW
import com.android.kubota.ui.AccountSetUpContext.Companion.SIGN_IN_FLOW
import com.google.android.material.snackbar.Snackbar

private const val MODE_ARGUMENT = "flow_mode"

class AccountSetupActivity: AppCompatActivity(), AccountSetUpContext {

    companion object {
        fun startActivityForSignIn(context: Context) = startActivity(context = context, flow = SIGN_IN_FLOW)

        fun startActivityForCreateAccount(context: Context) = startActivity(context = context, flow = CREATE_ACCOUNT_FLOW)

        fun startActivityForChangePassword(context: Context) = startActivity(context = context, flow = NEW_PASSWORD_FLOW)

        private fun startActivity(context: Context, flow: Int) {
            context.startActivity(Intent(context, AccountSetupActivity::class.java).apply {
                putExtra(MODE_ARGUMENT, flow)
            })
        }
    }

    private var currentMode = SIGN_IN_FLOW
    private lateinit var toolbar: Toolbar

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.getIntExtra(MODE_ARGUMENT, currentMode)?.let {
            if (it != currentMode) {
                currentMode = it

                supportFragmentManager.popBackStack(
                    R.id.fragmentPane,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )

                onStartViewModeNavigation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_setup)

        findViewById<View>(R.id.toolbarWithLogo).visibility = View.GONE
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            currentMode = intent.getIntExtra(MODE_ARGUMENT, SIGN_IN_FLOW)

            onStartViewModeNavigation()
        } else {
            currentMode = savedInstanceState.getInt(MODE_ARGUMENT, SIGN_IN_FLOW)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showProgressBar() {
        hideProgressBar()
        LoadingDialog().show(supportFragmentManager, LoadingDialog.TAG)
    }

    override fun hideProgressBar() {
        (supportFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog)?.dismiss()
    }

    override fun getMode() = currentMode

    override fun addFragmentToBackStack(fragment: Fragment) {
        toolbar.hideKeyboard()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun clearBackStack() {
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun makeSnackbar(): Snackbar? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun onStartViewModeNavigation() {
        val fragment = when (currentMode) {
            CREATE_ACCOUNT_FLOW -> CreateAccountFragment()
            NEW_PASSWORD_FLOW -> NewPasswordFragment()
            else -> SignInFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, fragment)
            .commitAllowingStateLoss()
    }
}

interface AccountSetUpContext: FlowActivity {
    companion object {
        const val SIGN_IN_FLOW = 0
        const val CREATE_ACCOUNT_FLOW = 1
        const val NEW_PASSWORD_FLOW = 2
    }

    override fun showProgressBar()
    override fun hideProgressBar()
    fun getMode(): Int
}

class LoadingDialog: DialogFragment() {

    companion object {
        const val TAG = "LoadingDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.AccountSetUpTheme_LoadingDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_fragment_loading, null)
    }

    override fun onCancel(dialog: DialogInterface) {
        activity?.onBackPressed()
    }
}