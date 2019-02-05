package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.android.kubota.R

class SplashActivity(): AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()

        Handler().postDelayed({
            if (this@SplashActivity.isFinishing || this@SplashActivity.isDestroyed) {
                return@postDelayed
            }
            this@SplashActivity.startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()

        }, 1500L)
    }
}