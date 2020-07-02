package com.android.kubota.viewmodel.dealers

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.*
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.combineAndCompute
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.google.android.gms.maps.model.LatLng
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.Dealer
import com.kubota.service.domain.preference.MeasurementUnitType
import com.kubota.service.domain.preference.UserPreference
import com.kubota.service.manager.SettingsRepo
import com.kubota.service.manager.SettingsRepoFactory

class DealerViewModelFactory(
    private val application: Application
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DealerViewModel(application) as T
    }

}

class DealerViewModel(
    application: Application
): AndroidViewModel(application), SettingsRepo.Observer {

    companion object {
        fun instance(
            owner: ViewModelStoreOwner,
            application: Application
        ): DealerViewModel {
            return ViewModelProvider(owner, DealerViewModelFactory(application))
                        .get(DealerViewModel::class.java)
        }
    }

    private val mIsAuthenticated = Transformations.map(AppProxy.proxy.accountManager.isAuthenticated) {
        updateData(delegate = null)
        it
    }

    private val mSettingsRepo = SettingsRepoFactory.getSettingsRepo(application)
    private val mMeasurementUnits = MutableLiveData<MeasurementUnitType>(mSettingsRepo.getCurrentUnitsOfMeasurement())
    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mNearestDealers = MutableLiveData<List<Dealer>>(emptyList())
    private val mFavoriteDealers = MutableLiveData<List<Dealer>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val nearestDealers: LiveData<List<SearchDealer>> = mNearestDealers
        .combineAndCompute(mMeasurementUnits) { dealerList, measurementUnit ->
            val searchDealerList = mutableListOf<SearchDealer>()
            dealerList.forEach {
                searchDealerList.add(
                    SearchDealer(
                        dealer = it,
                        measurementUnitType = measurementUnit,
                        resources = application.resources
                    )
                )
            }

            searchDealerList
        }
    val favoriteDealers: LiveData<List<Dealer>> = mFavoriteDealers
    val canAddToFavorite = mIsAuthenticated

    init {
        updateData(delegate = null)
        mSettingsRepo.addObserver(this)
    }

    override fun onChange() {
        mMeasurementUnits.postValue(mSettingsRepo.getCurrentUnitsOfMeasurement())
    }

    fun updateData(delegate: AuthDelegate?) {
        when(AppProxy.proxy.accountManager.isAuthenticated.value) {
            true -> execute(delegate) { AppProxy.proxy.serviceManager.userPreferenceService.getUserPreference() }
            else -> mFavoriteDealers.value = emptyList()
        }
    }

    fun searchNearestDealers(coordinate: LatLng): LiveData<List<SearchDealer>> {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.dealerService
            .getNearestDealers(latitude = coordinate.latitude, longitude = coordinate.longitude)
            .done { mNearestDealers.value = it }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
        return nearestDealers
    }

    fun isFavorited(dealer: Dealer): Boolean {
        return mFavoriteDealers.value?.let { dealers ->
            dealers.find { it.id == dealer.id }?.let { true } ?: false
        } ?: false
    }

    fun addToFavorite(delegate: AuthDelegate?, dealer: Dealer) {
        execute(delegate) { AppProxy.proxy.serviceManager.userPreferenceService.addDealer(id = dealer.id) }
    }

    fun removeFromFavorite(delegate: AuthDelegate?, dealer: Dealer) {
        execute(delegate) { AppProxy.proxy.serviceManager.userPreferenceService.removeDealer(id = dealer.id) }
    }

    fun createDeleteAction(delegate: AuthDelegate?, dealer: Dealer): UndoAction {
        return object : UndoAction {
            override fun commit() {
                removeFromFavorite(delegate, dealer)
            }
            override fun undo() {
                addToFavorite(delegate, dealer)
            }
        }
    }

    private fun execute(delegate: AuthDelegate?, f: () -> Promise<UserPreference> ) {
        when(AppProxy.proxy.accountManager.isAuthenticated.value) {
            true -> {
                mIsLoading.value = true
                AuthPromise(delegate)
                    .then { f() }
                    .done { mFavoriteDealers.value = it.dealers ?: emptyList() }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> return
        }
    }

}

class SearchDealer(dealer: Dealer, measurementUnitType: MeasurementUnitType, resources: Resources) {
    val id = dealer.id
    val address = dealer.address
    val dateCreated = dealer.dateCreated
    val dealerCertificationLevel = dealer.dealerCertificationLevel
    val dealerDivision = dealer.dealerDivision
    val dealerEmail = dealer.dealerEmail
    val dealerName = dealer.dealerName
    val dealerNumber = dealer.dealerNumber
    val expirationDate = dealer.expirationDate
    val extendedWarranty = dealer.extendedWarranty
    val fax= dealer.fax
    val lastModified = dealer.lastModified
    val location = dealer.location
    val phone = dealer.phone
    val productCodes = dealer.productCodes
    val publicationDate = dealer.publicationDate
    val salesQuoteEmail = dealer.salesQuoteEmail
    val serviceCertified = dealer.serviceCertified
    val tier2Participant = dealer.tier2Participant
    val urlName = dealer.urlName
    val rsmemail = dealer.rsmemail
    val rsmname = dealer.rsmname
    val rsmnumber = dealer.rsmnumber
    val measurementUnit = measurementUnitType
    private val distanceInMiles: Double = dealer.distance ?: 0.0
    val distance: String

    init {
        distance = if (measurementUnit == MeasurementUnitType.US) {
            resources.getString(R.string.distance_miles, distanceInMiles)
        } else {
            resources.getString(R.string.distance_kilometers, (distanceInMiles * 1.60934))
        }
    }

    fun toDealer(): Dealer {
        return Dealer(
            id,
            address,
            dateCreated,
            dealerCertificationLevel,
            dealerDivision,
            dealerEmail,
            dealerName,
            dealerNumber,
            distanceInMiles,
            expirationDate,
            extendedWarranty,
            fax,
            lastModified,
            location,
            phone,
            productCodes,
            publicationDate,
            salesQuoteEmail,
            serviceCertified,
            tier2Participant,
            urlName,
            rsmemail,
            rsmname,
            rsmnumber
        )
    }
}
