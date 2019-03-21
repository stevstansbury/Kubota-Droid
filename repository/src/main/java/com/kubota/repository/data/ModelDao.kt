package com.kubota.repository.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface ModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(model: Model)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(models: List<Model>)

    @Query("SELECT * FROM models")
    fun getUIModels(): LiveData<List<Model>?>

    @Query("SELECT * FROM models")
    fun getModels(): List<Model>?

    @Update
    fun update(model: Model)

    @Update
    fun updateAll(models: List<Model>)

    @Delete
    fun delete(model: Model)
}