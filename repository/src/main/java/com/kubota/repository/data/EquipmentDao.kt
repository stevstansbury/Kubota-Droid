package com.kubota.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(equipment: Equipment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(equipment: List<Equipment>)

    @Query("SELECT * FROM equipments")
    fun getLiveDataEquipments(): LiveData<List<Equipment>?>

    @Query("SELECT * FROM equipments WHERE _id = :equipmentId")
    fun getLiveDataEquipment(equipmentId: Int): LiveData<Equipment?>

    @Query("SELECT * FROM equipments WHERE _id = :equipmentId")
    fun getEquipment(equipmentId: Int): Equipment?

    @Query("SELECT * FROM equipments")
    fun getEquipments(): List<Equipment>?

    @Update
    fun update(equipment: Equipment)

    @Update
    fun updateAll(equipment: List<Equipment>)

    @Delete
    fun delete(equipment: Equipment)
}