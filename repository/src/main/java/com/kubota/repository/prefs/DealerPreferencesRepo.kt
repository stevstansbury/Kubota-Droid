package com.kubota.repository.prefs

import com.kubota.repository.data.Dealer
import com.kubota.repository.data.DealerDao

class DealerPreferencesRepo(private val dealerDao: DealerDao) {

    fun saveDealer(dealer: Dealer) = dealerDao.insert(dealer)

    fun getSavedDealers() = dealerDao.getDealers()

    fun getSelectedDealer() = dealerDao.getSelectedDealer()

    fun deleteDealer(dealer: Dealer) = dealerDao.delete(dealer)
}