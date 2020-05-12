package com.kubota.repository.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ModelSuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(suggestion: ModelSuggestion)

    @Query("SELECT * FROM model_suggestion WHERE :modelName = name")
    fun getModelSuggestion(modelName: String): ModelSuggestion?

    @Query("SELECT * FROM model_suggestion")
    fun getModelSuggestion(): List<ModelSuggestion>?

    @Query("SELECT * FROM model_suggestion ORDER BY searchDate DESC LIMIT 5")
    fun getLiveDataModelSuggestion(): LiveData<List<ModelSuggestion>?>

    @Update
    fun update(suggestion: ModelSuggestion)

    @Delete
    fun delete(suggestion: ModelSuggestion)
}