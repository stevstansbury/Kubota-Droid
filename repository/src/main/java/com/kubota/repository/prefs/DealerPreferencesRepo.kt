package com.kubota.repository.prefs

import com.kubota.repository.data.Dealer
import com.kubota.repository.data.DealerDao

class DealerPreferencesRepo(private val dealerDao: DealerDao) {

    companion object {
        internal const val EXTRA_DEALER = "dealer"
    }

    fun saveDealer(dealer: Dealer) = dealerDao.insert(dealer)

    fun getSavedDealers() = dealerDao.getDealers()

    fun deleteDealer(dealer: Dealer) = dealerDao.delete(dealer)
}