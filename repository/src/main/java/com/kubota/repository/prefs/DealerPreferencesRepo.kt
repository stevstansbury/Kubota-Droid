package com.kubota.repository.prefs

import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Dealer
import com.kubota.repository.data.DealerDao

class DealerPreferencesRepo(private val dealerDao: DealerDao) {

    companion object {
        internal const val EXTRA_DEALER = "dealer"
    }

    fun insertDealer(dealer: Dealer) = BaseApplication.serviceProxy.addDealer(dealer)

    fun getSavedDealers() = dealerDao.getUIDealers()

    fun deleteDealer(dealer: Dealer) = BaseApplication.serviceProxy.deleteDealer(dealer)
}