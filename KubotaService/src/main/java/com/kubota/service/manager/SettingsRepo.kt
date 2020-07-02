package com.kubota.service.manager

import android.content.Context
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.internal.UserSettingsRepo

interface SettingsRepo {

    fun getCurrentUnitsOfMeasurement(): MeasurementUnitType

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)

    interface Observer {
        fun onChange()
    }
}

object SettingsRepoFactory {
    private var repo: UserSettingsRepo? = null

    fun getSettingsRepo(context: Context): SettingsRepo {
        if (repo == null) repo = UserSettingsRepo(context = context.applicationContext)
        return repo!!
    }

    internal fun getUserSettingsRepo(): UserSettingsRepo {
        return repo!!
    }
}