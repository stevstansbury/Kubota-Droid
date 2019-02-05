package com.android.kubota.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.android.kubota.R
import com.android.kubota.utilities.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val LOG_IN_REQUEST_CODE = 1

    }

    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        viewModel.user.observe(this, Observer {
            it?.let {
                if (it.isGuest() && savedInstanceState == null) {
                    startActivityForResult(Intent(this@MainActivity, SignUpActivity::class.java), LOG_IN_REQUEST_CODE)
                }
            }
        })
        setContentView(R.layout.activity_main)
        setupButtons()
    }

    private fun setupButtons() {
        homeButton.setOnClickListener {
            //Launch home screen here
            viewModel.logout()
        }
        manualsButton.setOnClickListener{
            //Launch manual screen here
        }
        guidesButton.setOnClickListener{
            //Launch guide screen here
        }
        dealerLocatorButton.setOnClickListener{
            //Launch Dealer Locator screen here
        }
        profileButton.setOnClickListener{
            //Launch Profile screen here
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LOG_IN_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            finish()
        }
    }
}
