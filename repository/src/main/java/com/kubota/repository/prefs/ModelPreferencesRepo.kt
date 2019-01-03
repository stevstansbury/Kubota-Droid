package com.kubota.repository.prefs

import com.kubota.repository.data.Model
import com.kubota.repository.data.ModelDao

class ModelPreferencesRepo(private val modelDao: ModelDao) {

    fun getSavedModels() = modelDao.getModels()

    fun getSelectedModel() = modelDao.getSelectedModel()

    fun deleteModel(model: Model) = modelDao.delete(model)
}