package com.android.kubota.viewmodel.dealers

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.SignInHandler
import com.google.android.gms.maps.model.LatLng
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.Dealer
import com.kubota.service.domain.preference.UserPreference
import java.lang.ref.WeakReference

class DealerViewModelFactory(
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DealerViewModel(signInHandler) as T
    }

}

class DealerViewModel(
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner, signInHandler: WeakReference<SignInHandler>?): DealerViewModel {
            return ViewModelProvider(owner, DealerViewModelFactory(signInHandler))
                        .get(DealerViewModel::class.java)
        }
    }

    private val mIsAuthenticated = Transformations.map(AppProxy.proxy.accountManager.isAuthenticated) {
        updateData()
        it
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mNearestDealers = MutableLiveData<List<Dealer>>(emptyList())
    private val mFavoriteDealers = MutableLiveData<List<Dealer>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val nearestDealers: LiveData<List<Dealer>> = mNearestDealers
    val favoriteDealers: LiveData<List<Dealer>> = mFavoriteDealers
    val canAddToFavorite = mIsAuthenticated

    init {
        updateData()
    }

    fun updateData() {
        when(AppProxy.proxy.accountManager.isAuthenticated.value) {
            true -> execute { AppProxy.proxy.serviceManager.userPreferenceService.getUserPreference() }
            else -> mFavoriteDealers.value = emptyList()
        }
    }

    fun searchNearestDealers(coordinate: LatLng): LiveData<List<Dealer>> {
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

    fun addToFavorite(dealer: Dealer) {
        execute { AppProxy.proxy.serviceManager.userPreferenceService.addDealer(id = dealer.id) }
    }

    fun removeFromFavorite(dealer: Dealer) {
        execute { AppProxy.proxy.serviceManager.userPreferenceService.removeDealer(id = dealer.id) }
    }

    fun createDeleteAction(dealer: Dealer): UndoAction {
        return object : UndoAction {
            override fun commit() {
                removeFromFavorite(dealer)
            }
            override fun undo() {
                addToFavorite(dealer)
            }
        }
    }

    private fun signIn(): Promise<Unit> {
        return this.signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }

    private fun execute(f: () -> Promise<UserPreference> ) {
        when(AppProxy.proxy.accountManager.isAuthenticated.value) {
            true -> {
                mIsLoading.value = true
                AuthPromise()
                    .onSignIn { signIn() }
                    .then { f() }
                    .done { mFavoriteDealers.value = it.dealers ?: emptyList() }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> return
        }
    }

}
