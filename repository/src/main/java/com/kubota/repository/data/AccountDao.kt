package com.kubota.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface  AccountDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insert(account: Account)

    @Query("SELECT * FROM account WHERE id = 1")
    fun getLiveDataAccount(): LiveData<Account?>

    @Query("SELECT * FROM account WHERE id = 1")
    fun getAccount(): Account?

    @Update
    fun update(account: Account)

    @Delete
    fun deleteAccount(account: Account)
}