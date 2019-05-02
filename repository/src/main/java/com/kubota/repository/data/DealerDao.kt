package com.kubota.repository.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface DealerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dealer: Dealer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(dealers: List<Dealer>)

    @Query("SELECT * FROM dealers")
    fun getDealers(): List<Dealer>?

    @Query("SELECT * FROM dealers")
    fun getLiveDataDealers(): LiveData<List<Dealer>?>

    @Query("SELECT * FROM dealers WHERE number = :dealerNumber")
    fun getLiveDataDealerByNumber(dealerNumber: String): LiveData<Dealer?>

    @Query("SELECT * FROM dealers WHERE number = :dealerNumber")
    fun getDealerByNumber(dealerNumber: String): Dealer?

    @Query("SELECT * FROM dealers WHERE _id IN (:dealerIds)")
    fun getLiveDataDealersID(dealerIds: Array<String>): LiveData<List<Dealer>?>

    @Update
    fun update(dealer: Dealer)

    @Update
    fun updateAll(dealers: List<Dealer>)

    @Delete
    fun delete(dealer: Dealer)
}