package com.kubota.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FaultCodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(faultCode: FaultCode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(faultCodes: List<FaultCode>)

    @Query("SELECT * FROM fault_code WHERE equipmentId = :equipmentId")
    fun getLiveDataFaultCodes(equipmentId: Int): LiveData<List<FaultCode>?>

    @Query("SELECT * FROM fault_code WHERE equipmentId = :equipmentId")
    fun getFaultCodes(equipmentId: Int): List<FaultCode>?

    @Update
    fun update(faultCode: FaultCode)

    @Delete
    fun delete(faultCode: FaultCode)
}