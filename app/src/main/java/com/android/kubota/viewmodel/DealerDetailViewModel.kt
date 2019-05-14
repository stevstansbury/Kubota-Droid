package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import com.kubota.repository.data.Dealer
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.user.UserRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class DealerDetailViewModel(private val userRepo: UserRepo, private val dealerPreferencesRepo: DealerPreferencesRepo): ViewModel() {

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
        val result = MediatorLiveData<Boolean>()

        val func = object : Function2<Boolean?, Int?, Boolean> {
            override fun apply(input1: Boolean?, input2: Int?): Boolean {
                if (input1 == true) return true

                return (input2 ?: 0) == 0
            }

        }

        result.addSource(isUserLoggedIn) { _ -> result.value = func.apply(isUserLoggedIn.value, numberOfSavedDealers.value) }
        result.addSource(numberOfSavedDealers) { _ -> result.value = func.apply(isUserLoggedIn.value, numberOfSavedDealers.value) }

        canAddDealer = result
    }

    fun isFavoritedDealer(dealerNumber: String): LiveData<Boolean> = Transformations.map(dealerPreferencesRepo.getSavedDealer(dealerNumber)) {
        return@map it != null
    }

    fun insertFavorite(dealer: UIDealerDetailModel) {
        val newDealer = Dealer(serverId = UUID.randomUUID().toString(), userId = 1, name = dealer.name, streetAddress = dealer.address, city = dealer.city, stateCode = dealer.state, postalCode = dealer.postalCode, countryCode = "", phone = dealer.phone, webAddress = dealer.website, number = dealer.dealerNumber)
        dealerPreferencesRepo.insertDealer(newDealer)
    }

    fun deleteFavoriteDealer(dealer: UIDealerDetailModel) {
        backgroundScope.launch {
            dealerPreferencesRepo.deleteDealer(dealer.dealerNumber)
        }
    }

}

data class UIDealerDetailModel(val dealerNumber: String, val name: String, val address: String, val city: String, val state: String, val postalCode: String, val phone: String, val website: String): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(dealerNumber)
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(city)
        parcel.writeString(state)
        parcel.writeString(postalCode)
        parcel.writeString(phone)
        parcel.writeString(website)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UIDealerDetailModel> {
        override fun createFromParcel(parcel: Parcel): UIDealerDetailModel {
            return UIDealerDetailModel(parcel)
        }

        override fun newArray(size: Int): Array<UIDealerDetailModel?> {
            return arrayOfNulls(size)
        }
    }

}