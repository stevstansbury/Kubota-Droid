package com.kubota.repository.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface  AccountDao {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insert(account: Account)

    @Query("SELECT * FROM account WHERE id = 1")
    fun getUIAccount(): LiveData<Account>

    @Query("SELECT * FROM account WHERE id = 1")
    fun getAccount(): Account?

    @Update
    fun update(account: Account)

    @Delete
    fun deleteAccount()
}