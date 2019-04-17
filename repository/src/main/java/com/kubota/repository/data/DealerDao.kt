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
    fun getUIDealers(): LiveData<List<Dealer>?>

    @Query("SELECT * FROM dealers WHERE _id = :dealerId")
    fun getDealer(dealerId: String): LiveData<Dealer?>

    @Query("SELECT * FROM dealers WHERE _id IN (:dealerIds)")
    fun getDealersID(dealerIds: Array<String>): LiveData<List<Dealer>?>

    @Update
    fun update(dealer: Dealer)

    @Update
    fun updateAll(dealers: List<Dealer>)

    @Delete
    fun delete(dealer: Dealer)
}