package com.kubota.service.internal

import android.content.Context
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.domain.preference.UserSettings
import com.kubota.service.manager.SettingsRepo

internal class UserSettingsRepo(context: Context): SettingsRepo {
    private val applicationContext = context.applicationContext
    private var userSettings: UserSettings = UserSettings(
        measurementUnit = context.getDefaultMeasurementUnit(),
        subscribedToNotifications = false,
        subscribedToMessages = false,
        subscribedToAlerts = false
    )
    private val observerList = mutableListOf<SettingsRepo.Observer>()

    override fun getCurrentUnitsOfMeasurement(): MeasurementUnitType {
        return userSettings.measurementUnit ?: applicationContext.getDefaultMeasurementUnit()
    }

    override fun addObserver(observer: SettingsRepo.Observer) {
        observerList.add(observer)
    }

    override fun removeObserver(observer: SettingsRepo.Observer) {
        observerList.remove(observer)
    }

    override fun localeChanged(coldUser: Boolean) {
        if (coldUser) {
            saveUserSettings()
        }
    }

    internal fun saveUserSettings(settings: UserSettings = getDefaultUserSettings()) {
        val newSettings = if (settings.measurementUnit == null)
            settings.copy(measurementUnit = applicationContext.getDefaultMeasurementUnit())
        else
            settings

        if (userSettings != newSettings) {
            this.userSettings = settings
            notifyChange()
        }
    }

    private fun notifyChange() {
        observerList.forEach { it.onChange() }
    }

    private fun getDefaultUserSettings(): UserSettings {
        return UserSettings(
            measurementUnit = applicationContext.getDefaultMeasurementUnit(),
            subscribedToNotifications = false,
            subscribedToMessages = false,
            subscribedToAlerts = false
        )
    }
}

fun Context.getDefaultMeasurementUnit(): MeasurementUnitType {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        //We can get selected measurement unit
        val locale = resources.configuration.locales.get(0)
        val measurementSystem = LocaleData
            .getMeasurementSystem(
                ULocale.forLocale(locale)
            )
        if (measurementSystem == LocaleData.MeasurementSystem.US)
            return MeasurementUnitType.US
    } else {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            resources.configuration.locales.get(0)
        else
            resources.configuration.locale

        if (locale.country.equals("US", true))
            return MeasurementUnitType.US

    }

    return MeasurementUnitType.METRIC
}