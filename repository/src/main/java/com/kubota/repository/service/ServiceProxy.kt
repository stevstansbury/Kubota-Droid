package com.kubota.repository.service

import com.kubota.repository.data.Dealer
import com.kubota.repository.data.Equipment

interface ServiceProxy {
    fun accountSync()
    fun deleteEquipment(equipment: Equipment)
    fun updateEquipment(equipment: Equipment)
    fun addEquipment(equipment: Equipment)
    fun deleteDealer(dealer: Dealer)
    fun addDealer(dealer: Dealer)
}