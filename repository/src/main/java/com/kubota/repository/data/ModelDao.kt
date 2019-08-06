package com.kubota.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(model: Model)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(models: List<Model>)

    @Query("SELECT * FROM models")
    fun getLiveDataModels(): LiveData<List<Model>?>

    @Query("SELECT * FROM models WHERE _id = :modelId")
    fun getLiveDataModel(modelId: Int): LiveData<Model?>

    @Query("SELECT * FROM models WHERE _id = :modelId")
    fun getModel(modelId: Int): Model?

    @Query("SELECT * FROM models")
    fun getModels(): List<Model>?

    @Update
    fun update(model: Model)

    @Update
    fun updateAll(models: List<Model>)

    @Delete
    fun delete(model: Model)
}