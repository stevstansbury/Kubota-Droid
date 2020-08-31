package com.kubota.repository

import android.app.Application
import android.content.Intent
import com.google.gson.Gson
import com.kubota.network.service.CacheUtils
import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Equipment
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.PreferenceSyncService
import com.kubota.repository.service.ServiceProxy

abstract class BaseApplication: Application(), ServiceProxy {

    companion object {
        lateinit var serviceProxy: ServiceProxy
    }

    override fun onCreate() {
        super.onCreate()

        serviceProxy = this
        val factory = object : CacheUtils.CacheUtilsFactory {}
        factory.initCache(this)

        val intent = Intent(this, PreferenceSyncService::class.java)
        intent.action = Intent.ACTION_SYNC
        startService(intent)
    }

    private fun startPreferenceService(action: String, extraKey: String?, data: Any?) {
        val intent = Intent(this, PreferenceSyncService::class.java)
        intent.action = action
        if (extraKey != null && data != null) {
            intent.putExtra(extraKey, Gson().toJson(data))
        }
        startService(intent)
    }

    override fun accountSync() {
        startPreferenceService(action = Intent.ACTION_SYNC, extraKey = null, data = null)
    }

    override fun deleteEquipment(equipment: Equipment) {
        startPreferenceService(action = Intent.ACTION_DELETE, extraKey = EquipmentPreferencesRepo.EXTRA_EQUIPMENT, data = equipment)
    }

    override fun updateEquipment(equipment: Equipment) {
        startPreferenceService(action = Intent.ACTION_EDIT, extraKey = EquipmentPreferencesRepo.EXTRA_EQUIPMENT, data = equipment)
    }

    override fun addEquipment(equipment: Equipment) {
        startPreferenceService(action = Intent.ACTION_INSERT, extraKey = EquipmentPreferencesRepo.EXTRA_EQUIPMENT, data = equipment)
    }

    override fun addDealer(dealer: Dealer) {
        startPreferenceService(action = Intent.ACTION_INSERT, extraKey = DealerPreferencesRepo.EXTRA_DEALER, data = dealer)
    }

    override fun deleteDealer(dealer: Dealer) {
        startPreferenceService(action = Intent.ACTION_DELETE, extraKey = DealerPreferencesRepo.EXTRA_DEALER, data = dealer)
    }
}