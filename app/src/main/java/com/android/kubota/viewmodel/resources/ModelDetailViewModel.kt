package com.android.kubota.viewmodel.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.uimodel.KubotaModel
import com.kubota.repository.user.ModelSuggestionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelDetailViewModel(
    private val suggestionsRepo: ModelSuggestionRepo
): ViewModel() {

    suspend fun saveRecentlyViewed(model: KubotaModel) {
        withContext(Dispatchers.Default) {
            suggestionsRepo.saveModelSuggestion(model)
        }
    }

}

class ModelDetailViewModelFactory(
    private val suggestionsRepo: ModelSuggestionRepo
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ModelDetailViewModel(
            suggestionsRepo
        ) as T
    }
}