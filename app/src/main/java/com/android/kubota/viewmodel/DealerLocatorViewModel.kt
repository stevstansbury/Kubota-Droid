package com.android.kubota.viewmodel

import android.arch.lifecycle.*
import android.os.Parcel
import android.os.Parcelable
import com.android.kubota.extensions.toDealer
import com.android.kubota.extensions.toUIDealer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.kubota.repository.data.Dealer
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.service.DealerLocatorService
import com.kubota.repository.user.UserRepo
import com.kubota.repository.service.SearchDealer as ServiceDealer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class DealerLocatorViewModel(private val userRepo: UserRepo, private val dealerPreferencesRepo: DealerPreferencesRepo): ViewModel() {

    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)

    private val isUserLoggedIn: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.isGuest()?.not() ?: true
    }

    private val numberOfSavedDealers = Transformations.map(dealerPreferencesRepo.getSavedDealers()) {
        return@map it?.size ?: 0
    }

    val canAddDealer: LiveData<Boolean>

    init {
        canAddDealer = MediatorLiveData<Boolean>()

        val func = object : Function2<Boolean?, Int?, Boolean> {
            override fun apply(input1: Boolean?, input2: Int?): Boolean {
                if (input1 == true) return true

                return (input2 ?: 0) == 0
            }

        }

        canAddDealer.addSource(isUserLoggedIn) { _ -> canAddDealer.value = func.apply(isUserLoggedIn.value, numberOfSavedDealers.value) }
        canAddDealer.addSource(numberOfSavedDealers) { _ -> canAddDealer.value = func.apply(isUserLoggedIn.value, numberOfSavedDealers.value) }
    }

    fun searchDealer(latLng: LatLng): LiveData<List<SearchDealer>> {
        val result = MediatorLiveData<List<SearchDealer>>()

        val func = object : Function2<List<ServiceDealer>?, List<UIDealer>?, List<SearchDealer>> {
            override fun apply(input1: List<ServiceDealer>?, input2: List<UIDealer>?): List<SearchDealer> {
                if (input1.isNullOrEmpty()) {
                    return emptyList()
                } else {
                    val dealerNumbersList = input2?.map { it.dealerNumber }
                    return input1.map { it.toDealer(dealerNumbersList?.contains(it.dealerNumber) ?: false) }
                }

            }

        }

        val source1 = SearchDealersLiveData(latLng)
        val source2 = Transformations.map(dealerPreferencesRepo.getSavedDealers()) {
            return@map it?.map { it.toUIDealer() }
        }

        result.addSource(source1) { _ -> result.value = func.apply(source1.value, source2.value) }
        result.addSource(source2) { _ -> result.value = func.apply(source1.value, source2.value) }

        return result
    }

    fun insertFavorite(dealer: SearchDealer) {
        val newDealer = Dealer(serverId = UUID.randomUUID().toString(), userId = 1, name = dealer.name, streetAddress = dealer.streetAddress, city = dealer.city, stateCode = dealer.stateCode, postalCode = dealer.postalCode, countryCode = "", phone = dealer.phone, webAddress = dealer.webAddress, number = dealer.dealerNumber)
        dealerPreferencesRepo.insertDealer(newDealer)
    }

    fun deleteFavoriteDealer(dealer: SearchDealer) {
        backgroundScope.launch {
            dealerPreferencesRepo.deleteDealer(dealer.dealerNumber)
        }
    }

}

class SearchDealersLiveData(): LiveData<List<ServiceDealer>?>() {
    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)

    private val service = DealerLocatorService()

    constructor(latLng: LatLng) : this() {
        searchCoordinate(latLng)
    }

    private fun searchCoordinate(latLng: LatLng) {
        backgroundScope.launch {
            val results = service.searchDealers(latLng.latitude, latLng.longitude)

            postValue(results)
        }
    }

}

interface Function2<I, J, O> {
    /**
     * Applies this function to the given inputs.
     *
     * @param input1 the first input
     * @param input2 the second input
     * @return the function result.
     */
    fun apply(input1: I, input2: J): O
}

class SearchDealer(val serverId : String, val name : String, val streetAddress: String, val city: String,
             val stateCode: String, val postalCode: String, val countryCode: String, val phone : String,
             val webAddress : String, val dealerNumber : String, val latitude: Double, val longitude: Double,
             val distance : String, val isFavorited: Boolean): ClusterItem, Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    override fun getTitle(): String? {
        return null
    }

    override fun getSnippet(): String? {
        return null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serverId)
        parcel.writeString(name)
        parcel.writeString(streetAddress)
        parcel.writeString(city)
        parcel.writeString(stateCode)
        parcel.writeString(postalCode)
        parcel.writeString(countryCode)
        parcel.writeString(phone)
        parcel.writeString(webAddress)
        parcel.writeString(dealerNumber)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(distance)
        parcel.writeByte(if (isFavorited) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SearchDealer> {
        override fun createFromParcel(parcel: Parcel): SearchDealer {
            return SearchDealer(parcel)
        }

        override fun newArray(size: Int): Array<SearchDealer?> {
            return arrayOfNulls(size)
        }
    }
}