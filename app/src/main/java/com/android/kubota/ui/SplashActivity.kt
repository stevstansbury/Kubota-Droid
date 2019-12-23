package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel

class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: UserViewModel
    private var nextActivityIntent = Intent(this, LandingActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)

        viewModel.user.observe(this, Observer {
            if (it?.isGuest()?.not() == true) {
                nextActivityIntent = Intent(this, MainActivity::class.java)
            } else {
                nextActivityIntent = Intent(this, LandingActivity::class.java)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        Handler().postDelayed({
            if (this@SplashActivity.isFinishing || this@SplashActivity.isDestroyed) {
                return@postDelayed
            }
            this@SplashActivity.startActivity(nextActivityIntent)
            finish()

        }, 1500L)
    }
}