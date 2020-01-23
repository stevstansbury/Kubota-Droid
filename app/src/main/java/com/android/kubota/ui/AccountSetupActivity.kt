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
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.AccountSetUpContext.Companion.CREATE_ACCOUNT_FLOW
import com.android.kubota.ui.AccountSetUpContext.Companion.NEW_PASSWORD_FLOW
import com.android.kubota.ui.AccountSetUpContext.Companion.SIGN_IN_FLOW
import com.google.android.material.snackbar.Snackbar

private const val MODE_ARGUMENT = "flow_mode"

class AccountSetupActivity: AppCompatActivity(), AccountSetUpContext {

    companion object {
        fun startActivityForCreateAccount(context: Context) {
            context.startActivity(Intent(context, AccountSetupActivity::class.java).apply {
                putExtra(MODE_ARGUMENT, CREATE_ACCOUNT_FLOW)
            })
        }

        fun startActivityForChangePassword(context: Context) {
            context.startActivity(Intent(context, AccountSetupActivity::class.java).apply {
                putExtra(MODE_ARGUMENT, NEW_PASSWORD_FLOW)
            })
        }
    }

    private var currentMode = SIGN_IN_FLOW
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_setup)

        findViewById<View>(R.id.toolbarWithLogo).visibility = View.GONE
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            currentMode = intent.getIntExtra(MODE_ARGUMENT, SIGN_IN_FLOW)

            val fragment = when (currentMode) {
                CREATE_ACCOUNT_FLOW -> CreateAccountFragment()
                NEW_PASSWORD_FLOW -> NewPasswordFragment()
                else -> SignInFragment()

            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentPane, fragment)
                .commitAllowingStateLoss()
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
        supportFragmentManager.popBackStackImmediate(null, 0)
    }

    override fun makeSnackbar(): Snackbar? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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