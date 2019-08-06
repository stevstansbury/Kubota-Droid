package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.android.kubota.extensions.toUIDealer
import com.android.kubota.ui.action.UndoAction
import com.kubota.repository.data.Account
import com.kubota.repository.data.Dealer
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyDealersViewModel(private val userRepo: UserRepo, private val dealersPrefRepo: DealerPreferencesRepo) : ViewModel()  {
    private var dealerList = emptyList<Dealer>()

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
        val repoDealer = dealerList.find { d -> dealer.id == d.id}
        return object : UndoAction {

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