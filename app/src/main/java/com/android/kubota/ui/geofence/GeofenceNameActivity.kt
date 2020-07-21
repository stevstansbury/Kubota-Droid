package com.android.kubota.ui.geofence

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.inmotionsoftware.promisekt.PMKError
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Result
import kotlinx.android.synthetic.main.activity_geofence_name.*
import kotlinx.coroutines.CancellationException
import kotlin.random.Random
import kotlin.random.nextInt

class GeofenceNameActivity : AppCompatActivity() {
    companion object {
        private const val KEY_NAME = "name"
        
        fun handleResult(requestCode: Int, resultCode: Int, data: Intent?): Result<String> =
            when (resultCode) {
                Activity.RESULT_OK -> Result.fulfilled(data?.getStringExtra(KEY_NAME) ?: "")
                Activity.RESULT_CANCELED -> Result.rejected(PMKError.cancelled())
                else -> Result.rejected(IllegalArgumentException())
            }

        fun launch(fragment: Fragment, name: String): Int {
            val requestCode = Random.nextInt(range= IntRange(0, 65535))
            val intent = Intent(fragment.requireContext(), GeofenceNameActivity::class.java).putExtra(KEY_NAME, name)
            fragment.startActivityForResult(intent, requestCode)
            return requestCode
        }
    }

    class MyViewModel: ViewModel() {
        val name = MutableLiveData<String>()
    }

    private val viewModel: MyViewModel by lazy { ViewModelProvider(this).get(MyViewModel::class.java) }

    private lateinit var geofenceName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofence_name)
        geofenceName = findViewById(R.id.geofenceName)
    }

    override fun onStart() {
        super.onStart()

        this.geofenceName.doOnTextChanged { text, start, count, after ->
            this.viewModel.name.value = text.toString()
        }

        this.viewModel.name.observe(this, Observer {
            this.saveButton.isEnabled = it.isNotBlank()
        })

        this.saveButton.setOnClickListener {
            val name = this.viewModel.name.value
            setResult(Activity.RESULT_OK, Intent().putExtra(KEY_NAME, name))
            finish()
        }

        this.close.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val name = this.intent.getStringExtra(KEY_NAME)
        if (!name.isNullOrBlank()) {
            this.geofenceName.text = SpannableStringBuilder(name)
            this.geofenceName.selectAll()
        }
        this.viewModel.name.value = name
    }

    override fun onResume() {
        super.onResume()
        this.geofenceName.requestFocus()
    }
}