package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.android.kubota.extensions.toUIDealer
import com.android.kubota.ui.action.UndoAction
import com.kubota.repository.data.Account
import com.kubota.repository.data.Dealer
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyDealersViewModel(private val userRepo: UserRepo, private val dealersPrefRepo: DealerPreferencesRepo) : ViewModel()  {
    private var dealerList = emptyList<Dealer>()

    val isUserLoggedIn: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.isGuest()?.not() ?: true
    }

    val isLoading: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.flags == Account.FLAGS_SYNCING
    }

    val preferenceDealersList = Transformations.map(dealersPrefRepo.getSavedDealers()) {dealerList ->
        val results = mutableListOf<UIDealer>()
        this.dealerList = dealerList ?: emptyList()
        dealerList?.forEach {
            results.add(it.toUIDealer())
        }

        return@map results
    }

    fun createDeleteAction(dealer: UIDealer): UndoAction {
        val temp = dealerList.find { d -> dealer.id == d.id}
        return object : UndoAction {
            private val repoDealer= temp

            override fun commit() {
                repoDealer?.let {
                    dealersPrefRepo.deleteDealer(repoDealer)
                }
            }

            override fun undo() {
                repoDealer?.let {
                    dealersPrefRepo.insertDealer(repoDealer)
                }
            }
        }
    }

    fun getUpdatedDealersList(){
        userRepo.syncAccount()
    }
}

data class UIDealer(val id: Int, val name: String, val address: String, val city: String, val state: String, val postalCode: String, val phone: String, val website: String, val dealerNumber: String)