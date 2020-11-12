package com.android.kubota.ui.equipment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.ui.*
import com.android.kubota.viewmodel.equipment.AddEquipmentViewModel
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.then
import com.kubota.service.domain.EquipmentUnit
import java.lang.ref.WeakReference

interface AddEquipmentFlow {
    fun addEquipment(unit: EquipmentUnit)
    fun goToManualEntry()
    fun goToScanner()
}

interface AddEquipmentFragment {
    fun showProgressBar()
    fun hideProgressBar()
    fun showError(throwable: Throwable)
}

const val CAMERA_PERMISSION = 0

class AddEquipmentActivity: BaseActivity(), AddEquipmentFlow {

    private val viewModel: AddEquipmentViewModel by lazy {
        AddEquipmentViewModel.instance(owner = this)
    }

    override fun getLayOutResId(): Int {
        return 0
    }

    override fun getFragmentContainerId(): Int {
        return 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_equipment)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.let {
            it.setHomeAsUpIndicator(R.drawable.ic_arrow_back_wht_24dp)
            it.setDisplayHomeAsUpEnabled(true)
        }

        // check permissions
        this.requestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION)
            .done {
                goToScanner()
            }.catch {
                goToManualEntry()
            }

        viewModel.isLoading.observe(this, Observer {isLoading ->
            if (isLoading)
                getCurrentFragment()?.showProgressBar()
            else
                getCurrentFragment()?.hideProgressBar()
        })

        viewModel.error.observe(this, Observer {
            it?.let { getCurrentFragment()?.showError(it) }
        })

        viewModel.newEquipmentId.observe(this, Observer {uuid ->
            setResult(Activity.RESULT_OK, Intent().putExtra(NEW_EQUIPMENT_UUID, uuid.toString()))
            finish()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun addEquipment(unit: EquipmentUnit) {
        viewModel.addEquipmentUnit(this, unit = unit)
    }

    override fun goToManualEntry() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentPane, EquipmentSearchFragment.newInstance())
            .commit()
    }

    override fun goToScanner() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentPane, ScannerFragment())
            .commit()
    }

    private fun getCurrentFragment(): AddEquipmentFragment? {
        return supportFragmentManager.findFragmentById(R.id.fragmentPane) as? AddEquipmentFragment
    }

    override fun addFragmentToBackStack(fragment: Fragment) {
        TODO("Not yet implemented")
    }

    override fun clearBackStack() {
        TODO("Not yet implemented")
    }

    companion object {
        const val NEW_EQUIPMENT_UUID = "equipment_uuid"
    }
}

fun IntArray.permissionGranted(): Boolean {
    return this.isNotEmpty() && this[0] == PackageManager.PERMISSION_GRANTED
}