package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.android.kubota.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            if (this@SplashActivity.isFinishing || this@SplashActivity.isDestroyed) {
                return@postDelayed
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtras(this@SplashActivity.intent)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            this@SplashActivity.startActivity(intent)
            finish()

        }, 1500L)
    }
}