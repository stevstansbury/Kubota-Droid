package com.kubota.repository.user

import com.kubota.repository.data.ModelSuggestionDao

class ModelSuggestionRepo(
    private val dao: ModelSuggestionDao
) {

    fun getModelSuggestions() = dao.getLiveDataModelSuggestion()

}