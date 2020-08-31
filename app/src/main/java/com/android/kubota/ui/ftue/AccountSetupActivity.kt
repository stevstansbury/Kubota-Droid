package com.android.kubota.ui.ftue

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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.android.kubota.R
import com.android.kubota.coordinator.flow.util.BlockingActivityIndicator
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.FlowActivity
import com.android.kubota.ui.LegalTermsFragment
import com.android.kubota.ui.ftue.AccountSetUpController.Companion.CREATE_ACCOUNT_FLOW
import com.android.kubota.ui.ftue.AccountSetUpController.Companion.NEW_PASSWORD_FLOW
import com.android.kubota.ui.ftue.AccountSetUpController.Companion.SIGN_IN_FLOW
import com.google.android.material.snackbar.Snackbar

private const val MODE_ARGUMENT = "flow_mode"
private const val VERIFICATION_CODE = "code"

class AccountSetupActivity: AppCompatActivity(), FlowActivity,
    ForgotPasswordController, VerifyCodeController, NewPasswordController,
    CreateAccountController, SignInController {

    companion object {
        fun startActivityForSignIn(context: Context) =
            startActivity(context = context, flow = SIGN_IN_FLOW)

        fun startActivityForCreateAccount(context: Context) =
            startActivity(context = context, flow = CREATE_ACCOUNT_FLOW)

        fun startActivityForChangePassword(context: Context, forgotPasswordToken: String? = null, verificationCode: String? = null) =
            startActivity(
                context = context,
                flow = NEW_PASSWORD_FLOW,
                forgotPasswordToken = forgotPasswordToken,
                verificationCode = verificationCode
            )

        private fun startActivity(context: Context, flow: Int, forgotPasswordToken: String? = null, verificationCode: String? = null) {
            context.startActivity(Intent(context, AccountSetupActivity::class.java).apply {
                putExtra(MODE_ARGUMENT, flow)
                forgotPasswordToken?.let { putExtra(FORGOT_PASSWORD_TOKEN, it) }
                verificationCode?.let { putExtra(VERIFICATION_CODE, it)}
            })
        }
    }

    private var currentMode = SIGN_IN_FLOW
    private lateinit var toolbar: Toolbar
    private lateinit var rootView: CoordinatorLayout
    private var forgotPasswordToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_setup)

        findViewById<View>(R.id.toolbarWithLogo).visibility = View.GONE
        toolbar = findViewById(R.id.toolbar)
        rootView = findViewById(R.id.rootView)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            currentMode = intent.getIntExtra(MODE_ARGUMENT, SIGN_IN_FLOW)
            if (currentMode == NEW_PASSWORD_FLOW) {
                forgotPasswordToken = intent.getStringExtra(FORGOT_PASSWORD_TOKEN)
            }
            onStartViewModeNavigation()
        } else {
            currentMode = savedInstanceState.getInt(MODE_ARGUMENT, SIGN_IN_FLOW)
            forgotPasswordToken = savedInstanceState.getString(FORGOT_PASSWORD_TOKEN)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MODE_ARGUMENT, currentMode)
        forgotPasswordToken?.let { outState.putString(FORGOT_PASSWORD_TOKEN, it) }
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

    // AccountSetUpController functions
    override fun makeSnackbar(): Snackbar {
        return Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT).apply {
            val lp = view.layoutParams as CoordinatorLayout.LayoutParams
            val xMargin = resources.getDimension(R.dimen.snack_bar_horizontal_margin).toInt()
            val yMargin = resources.getDimension(R.dimen.snack_bar_vertical_margin).toInt()

            lp.setMargins(
                lp.leftMargin + xMargin,
                lp.topMargin,
                lp.rightMargin + xMargin,
                lp.bottomMargin + yMargin
            )
            view.layoutParams = lp
            view.setBackgroundResource(R.drawable.snack_bar_background)
        }
    }

    override fun showProgressBar() {
        hideProgressBar()
        LoadingDialog().show(supportFragmentManager,
            LoadingDialog.TAG
        )
    }

    override fun hideProgressBar() {
        (supportFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog)?.dismiss()
    }

    override fun showBlockingActivityIndicator() {
        this.showProgressBar()
    }

    override fun hideBlockingActivityIndicator() {
        this.hideProgressBar()
    }

    // Belong to multiple Controller interfaces
    override fun onSuccess(str: String) {
        supportFragmentManager.findFragmentById(R.id.fragmentPane)?.let {
            when (it) {
                is ForgotPasswordFragment -> {
                    forgotPasswordToken = str
                    onSuccess()
                }
            }
        }
    }

    override fun onSuccess() {
        when (currentMode) {
            SIGN_IN_FLOW -> {
                supportFragmentManager.findFragmentById(R.id.fragmentPane)?.let {
                    when (it) {
                        is ForgotPasswordFragment -> {
                            // Go to NewPasswordFragment
                            val fragment = NewPasswordFragment().apply {
                                arguments = Bundle(1).apply {
                                    putString(FORGOT_PASSWORD_TOKEN, forgotPasswordToken)
                                }
                            }
                            addFragmentToBackStack(fragment)
                        }
                        is NewPasswordFragment -> {
                            // Go to SignIn
                            clearBackStack()
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentPane, SignInFragment())
                                .commitAllowingStateLoss()
                        }
                        else -> finish()
                    }
                }
            }
            CREATE_ACCOUNT_FLOW -> {
                // When an account is created, the AccountManager will log the user in as well
                finish()
            }
            NEW_PASSWORD_FLOW -> {
                finish()
            }
        }
    }

    // CreateAccountController Functions
    override fun onNavigateToLegalTerms() {
        addFragmentToBackStack(LegalTermsFragment())
    }

    // SignInController Functions
    override fun onForgotPassword() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, ForgotPasswordFragment())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    // NewPasswordController functions
    override fun getMode() = currentMode

//    override fun createRequest(newPassword: String, other: String): RequestType {
//        return if (currentMode == NEW_PASSWORD_FLOW) {
//            RequestType.ChangePassword(other, newPassword)
//        } else {
//            RequestType.ResetPassword(verificationCode ?: "", newPassword)
//        }
//    }

    // FlowActivity Functions
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

    private fun onStartViewModeNavigation() {
        val fragment = when (currentMode) {
            CREATE_ACCOUNT_FLOW -> CreateAccountFragment()
            NEW_PASSWORD_FLOW -> {
                NewPasswordFragment()
            }
            else -> SignInFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentPane, fragment)
            .commitAllowingStateLoss()
    }
}

interface AccountSetUpController {
    companion object {
        const val SIGN_IN_FLOW = 0
        const val CREATE_ACCOUNT_FLOW = 1
        const val NEW_PASSWORD_FLOW = 2
        const val RESET_PASSWORD_FLOW = 3
    }

    fun showProgressBar()
    fun hideProgressBar()
    fun makeSnackbar(): Snackbar
}

interface ForgotPasswordController: AccountSetUpController {
    fun onSuccess(resetPasswordToken: String)
}

interface VerifyCodeController: AccountSetUpController {
    fun onSuccess(code: String)
}

interface NewPasswordController: AccountSetUpController {
    fun getMode(): Int
    fun onSuccess()
//    fun createRequest(newPassword: String, other: String): RequestType
}

interface CreateAccountController: AccountSetUpController {
    fun onNavigateToLegalTerms()
    fun onSuccess()
}

interface SignInController: AccountSetUpController {
    fun onForgotPassword()
    fun onSuccess()
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