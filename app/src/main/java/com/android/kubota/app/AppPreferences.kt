package com.android.kubota.app

import android.content.Context
import android.content.SharedPreferences
import com.kubota.service.internal.MaintenanceHistoryUpdate
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class AppPreferences(context: Context) {
    companion object {
        private const val KEY_CRP_SLT = "CRP_SLT"
        private const val KEY_FIRST_TIME_USED = "FIRST_TIME_USED"
        private const val KEY_GUIDES_DISCLAIMER_ACCEPTED = "GUIDES_DISCLAIMER_ACCEPTED"
        private const val KEY_FIRST_TIME_SCAN = "FIRST_TIME_SCAN"
        private const val LANGUAGE_TAG = "LANGUAGE_TAG"
        private const val KEY_MAINTENANCE_UPDATE = "MAINTENANCE_UPDATE"
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences("KubotaPreferences", Context.MODE_PRIVATE)

    var firstTimeUsed: Boolean
        get() {
            return this.preferences.getBoolean(KEY_FIRST_TIME_USED, true)
        }
        set(value) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_FIRST_TIME_USED, value)
            editor.apply()
        }

    var guidesDisclaimerAccepted: Boolean
        get() {
            return this.preferences.getBoolean(KEY_GUIDES_DISCLAIMER_ACCEPTED, false)
        }
        set(value) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_GUIDES_DISCLAIMER_ACCEPTED, value)
            editor.apply()
        }

    var firstTimeScan: Boolean
        get() {
            return this.preferences.getBoolean(KEY_FIRST_TIME_SCAN, true)
        }
        set(value: Boolean) {
            val editor = this.preferences.edit()
            editor.putBoolean(KEY_FIRST_TIME_SCAN, value)
            editor.apply()
        }

    val languageTag: String?
        get() {
            return this.preferences.getString(LANGUAGE_TAG, null)
        }

    fun setLanguageTag(value: String) {
        val editor = this.preferences.edit()
        editor.putString(LANGUAGE_TAG, value)
        editor.apply()
    }

    fun crpSlt(key: String): String {
        return this.preferences.getString(KEY_CRP_SLT + key, "") ?: ""
    }

    fun setCrpSlt(key: String, value: String) {
        val editor = this.preferences.edit()
        editor.putString(KEY_CRP_SLT + key, value)
        editor.apply()
    }

    fun setCryptoDataFor(key: String, encryptedData: String, encryptedIv: String) {
        val editor = this.preferences.edit()
        editor.putString(key, encryptedData)
        editor.putString("${key}_iv", encryptedIv)
        editor.apply()
    }

    fun getCryptoDataFor(key: String): Pair<String, String> {
        val encryptedString = this.preferences.getString(key, null) ?: ""
        val ivString = this.preferences.getString("${key}_iv", null) ?: ""

        return Pair(encryptedString, ivString)
    }

    fun addMaintenancePendingUpdate(unitId: String, update: MaintenanceHistoryUpdate) {
        val moshi = Moshi.Builder().build()

        val listType =
            Types.newParameterizedType(List::class.java, MaintenanceHistoryUpdate::class.java)
        val mapType =
            Types.newParameterizedType(Map::class.java, String::class.java, listType)

        val updateAdapter: JsonAdapter<Map<String, List<MaintenanceHistoryUpdate>>> =
            moshi.adapter(mapType)

        val currentUpdates = getMaintenancePendingUpdates()
        val pendingUpdates = mutableMapOf<String, List<MaintenanceHistoryUpdate>>()

        if (currentUpdates.keys.contains(unitId)) {
            val existingUpdate = currentUpdates[unitId]?.firstOrNull { it.id == update.id }

            if (existingUpdate == null) {
                pendingUpdates[unitId] = listOf(update) + (currentUpdates[unitId] ?: emptyList())
            } else {
                pendingUpdates[unitId] = currentUpdates[unitId]?.map {
                    if (it.id == update.id) {
                        update
                    } else {
                        it
                    }
                } ?: emptyList()
            }
        } else {
            pendingUpdates[unitId] = listOf(update)
        }

        val pendingUpdateJson = updateAdapter.toJson(pendingUpdates)
        this.preferences.edit().putString(KEY_MAINTENANCE_UPDATE, pendingUpdateJson).apply()
    }

    fun getMaintenancePendingUpdates(): Map<String, List<MaintenanceHistoryUpdate>> {
        val moshi = Moshi.Builder().build()

        val listType =
            Types.newParameterizedType(List::class.java, MaintenanceHistoryUpdate::class.java)
        val mapType =
            Types.newParameterizedType(Map::class.java, String::class.java, listType)

        val pendingUpdateJson =
            this.preferences.getString(KEY_MAINTENANCE_UPDATE, null) ?: return emptyMap()

        val updateAdapter: JsonAdapter<Map<String, List<MaintenanceHistoryUpdate>>> =
            moshi.adapter(mapType)

        return updateAdapter.fromJson(pendingUpdateJson) ?: return emptyMap()
    }

    fun clearMaintenancePendingUpdate(unitId: String, updateId: String?) {
        val pendingUpdates = getMaintenancePendingUpdates().toMutableMap()

        val remainingUpdates =
            pendingUpdates[unitId]?.toMutableList()?.filter { it.id != updateId } ?: emptyList()
        if (remainingUpdates.isEmpty()) {
            pendingUpdates.remove(unitId)
        } else {
            pendingUpdates[unitId] = remainingUpdates
        }

        if (pendingUpdates.isEmpty()) {
            clearKey(KEY_MAINTENANCE_UPDATE)
        } else {
            val moshi = Moshi.Builder().build()

            val listType =
                Types.newParameterizedType(List::class.java, MaintenanceHistoryUpdate::class.java)
            val mapType =
                Types.newParameterizedType(Map::class.java, String::class.java, listType)

            val updateAdapter: JsonAdapter<Map<String, List<MaintenanceHistoryUpdate>>> =
                moshi.adapter(mapType)

            val pendingUpdateJson = updateAdapter.toJson(pendingUpdates)
            this.preferences.edit().putString(KEY_MAINTENANCE_UPDATE, pendingUpdateJson).apply()
        }
    }

    fun clearAllMaintenancePendingUpdates() {
        clearKey(KEY_MAINTENANCE_UPDATE)
    }

    fun clearKey(key: String) {
        this.preferences.edit().remove(key).apply()
    }

}
