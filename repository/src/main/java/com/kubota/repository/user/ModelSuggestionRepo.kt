package com.kubota.repository.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kubota.repository.data.Constants
import com.kubota.repository.data.ModelSuggestion
import com.kubota.repository.data.ModelSuggestionDao
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.KubotaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelSuggestionRepo(
    private val dao: ModelSuggestionDao
) {
    private val service = CategoryModelService()

    suspend fun getModelSuggestions(): LiveData<List<KubotaModel>?> {
        val modelsList = getKubotaModels()

        return Transformations.map(dao.getLiveDataModelSuggestion()) {
            if (it.isNullOrEmpty() || modelsList == null) return@map null

            val modelMap = mutableMapOf<String, MutableMap<String, KubotaModel>>()

            for (model in modelsList) {
                val equipmentMap = modelMap[model.category] ?: mutableMapOf()
                equipmentMap[model.modelName] = model

                modelMap[model.category] = equipmentMap
            }

            return@map modelsList.let {models ->
                val mutableList = mutableListOf<KubotaModel>()

                for (suggestion in it) {
                    modelMap[suggestion.category]?.let {
                        it[suggestion.name]?.let {
                            mutableList.add(it)
                        }
                    }
                }

                mutableList
            }
        }
    }

    suspend fun getKubotaModels(): List<KubotaModel>? {
        return withContext(Dispatchers.IO) {
            when(val response = service.getModels()) {
                is CategorySyncResults.Success -> response.results
                else -> null
            }
        }
    }



    fun saveModelSuggestion(model: KubotaModel) {
        val searchSuggestion = dao.getModelSuggestion(model.modelName)
            ?.let {
                it.searchDate = System.currentTimeMillis()

                it
            }
            ?: model.toSearchSuggestion()

        if (searchSuggestion.id == Constants.DEFAULT_ID) {
            dao.insert(searchSuggestion)
        } else {
            dao.update(searchSuggestion)
        }
    }
}

private fun KubotaModel.toSearchSuggestion(): ModelSuggestion {
    return ModelSuggestion(
        searchDate = System.currentTimeMillis(),
        name = modelName,
        category = category
    )
}