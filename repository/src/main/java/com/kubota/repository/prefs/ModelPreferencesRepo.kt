package com.kubota.repository.prefs

import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Model
import com.kubota.repository.data.ModelDao

class ModelPreferencesRepo(private val modelDao: ModelDao) {

    companion object {
        internal const val EXTRA_MODEL = "model"
    }

    fun getSavedModels() = modelDao.getLiveDataModels()

    fun insertModel(model: Model) {
        BaseApplication.serviceProxy.addModel(model)
    }

    fun deleteModel(model: Model) {
        BaseApplication.serviceProxy.deleteModel(model)
    }

    fun getModel(modelId: Int) = modelDao.getLiveDataModel(modelId)

    fun updateModel(id: Int, serialNumber: String?) {
        modelDao.getModel(id)?.let {
            val updatedModel = Model(it.id, it.serverId, it.userId, it.model, serialNumber, it.category, it.manualName,
                it.manualLocation, it.hasGuide)
            BaseApplication.serviceProxy.updateModel(updatedModel)
        }
    }
}