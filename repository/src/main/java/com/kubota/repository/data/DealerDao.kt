package com.kubota.repository.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface DealerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dealer: Dealer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(dealers: List<Dealer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(selectedDealer: SelectedDealer)

    @Query("SELECT * FROM dealers")
    fun getDealers(): LiveData<List<Dealer>>

    @Query("SELECT * FROM dealers WHERE id = :dealerId")
    fun getDealer(dealerId: String): LiveData<Dealer>

    @Query("SELECT * FROM dealers WHERE id IN (:dealerIds)")
    fun getDealersID(dealerIds: Array<String>): LiveData<List<Dealer>>

    //TODO: Fix up select statement from query
    @Query("SELECT * FROM dealers JOIN selected_dealers ON selected_dealers.dealerId = dealers.id")
    fun getSelectedDealer(): LiveData<Dealer>

    @Update
    fun update(dealer: Dealer)

    @Update
    fun updateAll(dealers: List<Dealer>)

    @Delete
    fun delete(dealer: Dealer)
}