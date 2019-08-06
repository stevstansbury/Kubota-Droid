package com.android.kubota.viewmodel

import android.app.Activity
import androidx.lifecycle.*
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.ui.*
import com.android.kubota.utility.Utils
import com.crashlytics.android.Crashlytics
import com.google.android.libraries.places.compat.AutocompleteFilter
import com.google.android.libraries.places.compat.AutocompletePrediction
import com.google.android.libraries.places.compat.Places
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.service.SearchDealer as ServiceDealer

abstract class SearchViewModel: ViewModel() {

    protected val isLoading = MutableLiveData<Boolean>()

    protected val serverError = MutableLiveData<Boolean>()

    abstract fun search(activity: AppCompatActivity, recyclerView: RecyclerView, query: String)
}

class SearchEquipmentViewModel(private val categoryService: CategoryModelService): SearchViewModel() {

    val categories: MutableLiveData<Map<String, List<String>>> by lazy {
        loadCategories()
        MutableLiveData<Map<String, List<String>>>()
    }

    override fun search(activity: AppCompatActivity, recyclerView: RecyclerView, query: String) {
            val liveData = Transformations.map(categories) {map ->
                map?.entries?.flatMap { (key, entry) ->
                    val categoryResId = when(key) {
                        "Construction" -> R.string.equipment_construction_category
                        "Mowers" -> R.string.equipment_mowers_category
                        "Tractors" -> R.string.equipment_tractors_category
                        else -> R.string.equipment_utv_category
                    }

                    entry.mapNotNull { model ->
                        if (model.contains(query, ignoreCase = true)) {
                            EquipmentUIModel(id = 1, name = model, categoryResId = categoryResId, imageResId = Utils.getModelImage(key, model))
                        } else {
                            null
                        }
                    }
            }
        }

        liveData.observe(activity, Observer {
            recyclerView.adapter = EquipmentSearchHintListAdapter(it ?: emptyList()) {
                val intent = Intent()
                intent.putExtra(ChooseEquipmentFragment.KEY_SEARCH_RESULT, it)
                activity.setResult(Activity.RESULT_OK, intent)
                activity.finish()
            }

        })
    }

    private fun loadCategories() {
        Utils.backgroundTask {
            isLoading.postValue(true)
            serverError.postValue(false)
            when (val result = categoryService.getCategories()) {
                is CategorySyncResults.Success -> {
                    // TODO: Map to use EquipmentUIModel
                    categories.postValue(result.results)
                }
                is CategorySyncResults.ServerError, //We should log this in Crashlytics
                is CategorySyncResults.IOException -> serverError.postValue(true)
            }
            isLoading.postValue(false)
        }
    }
}

private const val USA_COUNTRY_FILTER = "US"
class SearchDealersViewModel(): SearchViewModel() {

    override fun search(activity: AppCompatActivity, recyclerView: RecyclerView, query: String) {
        val geoClient = Places.getGeoDataClient(activity)
        val task = geoClient.getAutocompletePredictions(query, null, AutocompleteFilter.Builder()
            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_REGIONS.and(AutocompleteFilter.TYPE_FILTER_CITIES))
            .setCountry(USA_COUNTRY_FILTER)
            .build())

        task.addOnSuccessListener {
            val list = ArrayList<AutocompletePrediction>(it.count)
            it.forEach { list.add(it) }
            recyclerView.adapter = DealersSearchHintListAdapter(list) {
                val intent = Intent()
                geoClient.getPlaceById(it.placeId)
                    .addOnSuccessListener {
                        intent.putExtra(ChooseEquipmentFragment.KEY_SEARCH_RESULT, it.get(0)?.latLng)
                        activity.setResult(Activity.RESULT_OK, intent)
                        activity.finish()
                    }
                    .addOnFailureListener {
                        Crashlytics.logException(it)
                        activity.setResult(AutocompleteActivity.RESULT_ERROR, intent)
                        activity.finish()
                    }
            }
        }.addOnFailureListener {
            Crashlytics.logException(it)
        }
    }

}