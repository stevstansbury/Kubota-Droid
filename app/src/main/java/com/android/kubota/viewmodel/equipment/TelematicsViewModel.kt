package com.android.kubota.viewmodel.equipment

import android.app.Application
import android.location.Geocoder
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.combineAndCompute
import com.android.kubota.ui.equipment.telematicsString
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.after
import com.kubota.service.domain.*
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.manager.SettingsRepo
import com.kubota.service.manager.SettingsRepoFactory
import java.util.*
import kotlin.math.floor
import kotlin.math.max

class TelematicsViewModelFactory(
    private val application: Application,
    private val equipmentUnitId: UUID
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TelematicsViewModel(
            application,
            equipmentUnitId
        ) as T
    }
}

class TelematicsViewModel(
    application: Application,
    private val equipmentUnitId: UUID
) : AndroidViewModel(application), SettingsRepo.Observer {

    private val settingsRepo = SettingsRepoFactory.getSettingsRepo(application)
    private val _mutableMeasurementUnits = MutableLiveData(settingsRepo.getCurrentUnitsOfMeasurement())
    private val _unitNickname = MutableLiveData<String>()
    private val _unitLocation = MutableLiveData<UnitLocation>()
    private val _isLoading = MutableLiveData(0)
    private val _telematics = MutableLiveData<Telematics>()
    private val _address = MutableLiveData<String>()
    private val _geoLocationIcon = MutableLiveData<Int>()
    private val _error = MutableLiveData<Throwable>()

    var polling: Boolean = false
        set(newValue) {
            if (newValue == field) return
            field = newValue
            if (newValue) {
                reloadEquipment(null)
            }
        }

    val error: LiveData<Throwable> = _error
    var interval = DEFAULT_REFRESH_INTERVAL
    val isLoading: LiveData<Boolean> = Transformations.map(_isLoading) { it > 0 }

    val unitLocation = _unitLocation
    val fuelLayoutVisibility: LiveData<Int> = Transformations.map(_telematics) {
        if (it.fuelRemainingPercent == null) View.GONE else View.VISIBLE
    }
    val defLayoutVisibility: LiveData<Int> = Transformations.map(_telematics) {
        if (it.defRemainingPercent == null) View.GONE else View.VISIBLE
    }
    val batteryLayoutVisibility: LiveData<Int> = Transformations.map(_telematics) {
        if (it.extPowerVolts == null) View.GONE else View.VISIBLE
    }
    val hydraulicTempLayoutVisibility: LiveData<Int> = Transformations.map(_telematics) {
        if (it.hydraulicTempCelsius == null) View.GONE else View.VISIBLE
    }
    val coolantLayoutVisibility: LiveData<Int> = Transformations.map(_telematics) {
        if (it.coolantTempCelsius == null) View.GONE else View.VISIBLE
    }

    val time: LiveData<String> = Transformations.map(_telematics) {
        it.locationTime?.telematicsString(application.resources)
    }

    val fuelPercent: LiveData<Double> = Transformations.map(_telematics) {
        it.fuelRemainingPercent.toPercent()
    }
    val defPercent: LiveData<Double> = Transformations.map(_telematics) {
        it.defRemainingPercent.toPercent()
    }
    val batteryVoltText: LiveData<String> = Transformations.map(_telematics) {
        val voltage = it.extPowerVolts ?: 0.0
        val roundedVoltage = floor(voltage * 10.0) / 10.0
        String.format(
            getString(R.string.voltage_fmt),
            roundedVoltage
        )
    }
    val batteryVoltColor: LiveData<Int> = Transformations.map(_telematics) {
        when (it.voltageStatus) {
            TelematicStatus.Normal -> R.color.battery_indicator_green
            TelematicStatus.Warning -> R.color.battery_indicator_brown
            TelematicStatus.Critical -> R.color.battery_indicator_red
            else -> R.color.battery_indicator_red
        }
    }
    val batteryVolt: LiveData<Int> = Transformations.map(_telematics) {
        when (it.voltageStatus) {
            TelematicStatus.Normal -> R.drawable.ic_battery_green
            TelematicStatus.Warning -> R.drawable.ic_battery_brown
            TelematicStatus.Critical -> R.drawable.ic_battery_red
            else -> R.drawable.ic_battery_red
        }
    }
    private val _hydraulicTemp = Transformations.map(_telematics) {
        it.hydraulicTempCelsius ?: 0
    }.combineAndCompute(_mutableMeasurementUnits) { temperature, measurementUnit ->
        if (measurementUnit == MeasurementUnitType.METRIC) {
            String.format(getString(R.string.temperature_celsius_fmt), temperature)
        } else {
            val adjustedTemperature = ((temperature * 1.8) + 32).toInt()
            String.format(getString(R.string.temperature_fahrenheit_fmt), adjustedTemperature)
        }
    }
    val hydraulicTemp: LiveData<String> = _hydraulicTemp
    val hydraulicTempColor: LiveData<Int> = Transformations.map(_telematics) {
        when (it.hydraulicTempStatus) {
            TelematicStatus.Normal -> R.color.thermometer_green
            TelematicStatus.Warning -> R.color.thermometer_yellow
            TelematicStatus.Critical -> R.color.thermometer_red
            else -> R.color.thermometer_red
        }
    }
    private val _coolantTemp = Transformations.map(_telematics) {
        it.coolantTempCelsius ?: 0
    }.combineAndCompute(_mutableMeasurementUnits) { temperature, measurementUnit ->
        if (measurementUnit == MeasurementUnitType.METRIC) {
            String.format(getString(R.string.temperature_celsius_fmt), temperature)
        } else {
            val adjustedTemperature = ((temperature * 1.8) + 32).toInt()
            String.format(getString(R.string.temperature_fahrenheit_fmt), adjustedTemperature)
        }
    }
    val coolantTemp: LiveData<String> = _coolantTemp
    val coolantTempColor: LiveData<Int> = Transformations.map(_telematics) {
        when (it.coolantTempStatus) {
            TelematicStatus.Normal -> R.color.thermometer_green
            TelematicStatus.Warning -> R.color.thermometer_yellow
            TelematicStatus.Critical -> R.color.thermometer_red
            else -> R.color.thermometer_red
        }
    }

    val unitNickname: LiveData<String> = _unitNickname
    val address: LiveData<String> = _address
    val geoLocationIcon: LiveData<Int> = _geoLocationIcon

    private val geocoder: Geocoder? by lazy { Geocoder(getApplication(), Locale.getDefault()) }

    init {
        settingsRepo.addObserver(this)
        loadEquipmentUnit()
    }

    override fun onChange() {
        _mutableMeasurementUnits.postValue(settingsRepo.getCurrentUnitsOfMeasurement())
    }

    override fun onCleared() {
        settingsRepo.removeObserver(this)
        super.onCleared()
    }

    fun setEquipment(unit: EquipmentUnit?) {
        this._telematics.value = unit?.telematics
        unit?.let {
            val title = String.format(
                getString(R.string.telemtatics_fragment_title),
                unit.nickName ?: it.model
            )
            _unitNickname.postValue(title)
        }
        unit?.telematics?.let {
            _telematics.postValue(it)
            loadAddress(it.location, it.outsideGeofence)
        }
    }

    private fun loadEquipmentUnit() {
        this.incrementLoading()
        AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = this.equipmentUnitId)
            .done { setEquipment(unit = it) }
            .ensure {
                this.decrementLoading()
                // after loading begin our polling
                this.polling = true
            }
            .catch { _error.postValue(it) }
    }

    private fun reloadEquipment(delegate: AuthDelegate?) {
        if (!polling) return

        incrementLoading()
        AuthPromise(delegate = delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.getEquipment(equipmentUnitId)
            }
            .ensure { this.decrementLoading() }
            .thenMap { equipmentUnit ->
                if (!polling) return@thenMap Promise.value(Unit)

                setEquipment(unit = equipmentUnit)

                after(seconds = this.interval)
            }
            .done {
                this.reloadEquipment(delegate = delegate)
            }
            .catch { _error.postValue(it) }
    }

    private fun incrementLoading() {
        _isLoading.value = _isLoading.value?.let { it + 1 } ?: 1
    }

    private fun decrementLoading() {
        _isLoading.value = _isLoading.value?.let { max(0, it - 1) } ?: 0
    }

    private fun loadAddress(location: GeoCoordinate?, outsideGeofence: Boolean) {
        val geofenceMarker =
            if (outsideGeofence) R.drawable.ic_outside_geofence else R.drawable.ic_inside_geofence
        _unitLocation.postValue(
            UnitLocation(
                location = location,
                mapMarkerResId = if (outsideGeofence) R.drawable.ic_outside_geofence else R.drawable.ic_inside_geofence
            )
        )

        Promise.value(location)
            .map(on = DispatchExecutor.global) { coordinate ->
                coordinate?.let {
                    this.geocoder?.getFromLocation(it.latitude, it.longitude, 1)?.firstOrNull()
                }
            }
            .done { geoAddress ->
                if (geoAddress != null) {
                    val postalCode = geoAddress.postalCode ?: ""
                    val locality = geoAddress.locality
                    val state = geoAddress.adminArea
                    val number = geoAddress.subThoroughfare ?: ""
                    val street = geoAddress.thoroughfare

                    val addressText = if (state != null && locality != null && street != null) {
                        String.format(
                            getString(R.string.address_two_line_fmt),
                            number,
                            street,
                            locality,
                            state,
                            postalCode
                        )
                    } else if (state != null && locality != null) {
                        String.format(
                            getString(R.string.city_state_postal_code_fmt),
                            locality,
                            state,
                            postalCode
                        )
                    } else {
                        getString(R.string.not_available)
                    }
                    _address.postValue(addressText)

                    // FIXME: Mock data
                    _geoLocationIcon.postValue(geofenceMarker)
                    _unitLocation.postValue(
                        UnitLocation(
                            location = location,
                            mapMarkerResId = geofenceMarker
                        )
                    )
                } else {
                    // FIXME: Need unknown geofence icon
                    _address.postValue(getString(R.string.location_unavailable))
                    _unitLocation.postValue(
                        UnitLocation(
                            location = location,
                            mapMarkerResId = R.drawable.ic_outside_geofence
                        )
                    )
                }
            }
            .catch {
                // FIXME: Need unknown geofence icon
                _address.postValue(getString(R.string.location_unavailable))
            }
    }
}

data class UnitLocation(
    val location: GeoCoordinate?,
    @DrawableRes val mapMarkerResId: Int
)

fun AndroidViewModel.getString(@StringRes resId: Int): String {
    return getApplication<Application>().getString(resId)
}

fun AndroidViewModel.getString(@StringRes resId: Int, str: String): String {
    return getApplication<Application>().getString(resId, str)
}

private fun Int?.toPercent(): Double {
    return this?.div(100.0) ?: 0.0
}