package com.kubota.repository.service

import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Model

interface ServiceProxy {
    fun accountSync()
    fun deleteModel(model: Model)
    fun updateModel(model: Model)
    fun addModel(model: Model)
    fun deleteDealer(dealer: Dealer)
    fun addDealer(dealer: Dealer)
}