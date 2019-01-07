package com.android.kubota.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.android.kubota.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupButtons()
    }

    private fun setupButtons() {
        homeButton.setOnClickListener {
            //Launch home screen here
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
}
