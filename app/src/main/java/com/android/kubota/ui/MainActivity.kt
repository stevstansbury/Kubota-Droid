package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.android.kubota.R
import com.android.kubota.extensions.getPublicClientApplication
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val LOG_IN_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideUserViewModelFactory(this)
        val viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        if (savedInstanceState == null) {
            if (viewModel.user.value == null) {
                startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
            } else {
                val isGuest = viewModel.user.value?.isGuest() ?: true
                if (isGuest) {
                    startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
                }
            }
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentPane, MockNavigationFragment())
                .commitAllowingStateLoss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LOG_IN_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }
            return
        }

        if (requestCode ==  AuthenticationConstants.UIRequest.BROWSER_FLOW) {
            getPublicClientApplication().handleInteractiveRequestRedirect(requestCode, resultCode, data)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
