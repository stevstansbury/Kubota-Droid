package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.android.kubota.R
import com.android.kubota.utilities.InjectorUtils
import com.android.kubota.viewmodel.UserViewModel
import com.kubota.repository.data.Account

class SplashActivity(): AppCompatActivity() {

    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val factory = InjectorUtils.provideUserViewModelFactory(this)
        val viewmodel = ViewModelProviders.of(this, factory).get(UserViewModel::class.java)
        viewmodel.user.observe(this, Observer { account = it })
    }

    override fun onResume() {
        super.onResume()

        Handler().postDelayed({
            if (this@SplashActivity.isFinishing || this@SplashActivity.isDestroyed) {
                return@postDelayed
            }
            var intent = Intent(this@SplashActivity, MainActivity::class.java)
            if (account?.isGuest() ?: true) {
                intent = Intent(this@SplashActivity, SignUpActivity::class.java)
            }
            finish()
            this@SplashActivity.startActivity(intent)

        }, 1500L)
    }
}