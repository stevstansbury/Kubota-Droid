package com.android.kubota.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.android.kubota.R

private val KEY_MODEL_NAME = "model_name"
private val KEY_GUIDE_ITEM = "guide_item"

class MaintenanceGuideActivity: AppCompatActivity() {

    companion object {
        fun launchMaintenanceGuideActivity(context: Context, model: String, guideItem: String) {
            val intent = Intent(context, MaintenanceGuideActivity::class.java)
            intent.putExtra(KEY_MODEL_NAME, model)
            intent.putExtra(KEY_GUIDE_ITEM, guideItem)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maintenance_guide)
    }
}