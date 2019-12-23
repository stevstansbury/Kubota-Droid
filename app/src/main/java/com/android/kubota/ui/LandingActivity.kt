package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel

class LandingActivity: AppCompatActivity() {

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)

        viewModel.user.observe(this, Observer {
            if (it?.isGuest()?.not() == true) {
                onGuestContinueClicked()
            }
        })

        setContentView(R.layout.activity_landing)

        findViewById<Button>(R.id.createAccountButton).setOnClickListener { onCreateAccountClicked() }
        findViewById<Button>(R.id.signInButton).setOnClickListener { onSignInClicked() }
        findViewById<TextView>(R.id.continueTextView).setOnClickListener { onGuestContinueClicked() }
    }

    private fun onCreateAccountClicked() {
        AccountSetupActivity.startActivityForCreateAccount(this)
    }

    private fun onSignInClicked() {
        startActivity(Intent(this, AccountSetupActivity::class.java))
    }

    private fun onGuestContinueClicked() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}