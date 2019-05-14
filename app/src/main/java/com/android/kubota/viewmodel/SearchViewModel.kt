package com.android.kubota.viewmodel

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Intent
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.extensions.backgroundTask
import com.android.kubota.extensions.toDealer
import com.android.kubota.extensions.toUIDealer
import com.android.kubota.ui.*
import com.google.android.gms.maps.model.LatLng
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.service.SearchDealer as ServiceDealer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class SearchViewModel: ViewModel() {
    private val viewModelJob = Job()
    protected val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

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
                    val imageResId = when(key) {
                        "Construction" -> R.drawable.ic_construction_category_thumbnail
                        "Mowers" -> R.drawable.ic_mower_category_thumbnail
                        "Tractors" -> R.drawable.ic_tractor_category_thumbnail
                        else -> R.drawable.ic_utv_category_thumbnail
                    }

                    entry.mapNotNull { model ->
                        if (model.contains(query, ignoreCase = true)) {
                            EquipmentUIModel(id = 1, name = model, categoryResId = categoryResId, imageResId = imageResId)
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
        backgroundScope.backgroundTask {
            isLoading.postValue(true)
            serverError.postValue(false)
            when (val result = categoryService.getCategories()) {
                is CategorySyncResults.Success -> {
                    // TODO: Map to use EquipmentUIModel
                    categories.postValue(result.results)
                }
                is CategorySyncResults.ServerError,
                is CategorySyncResults.IOException -> serverError.postValue(true)
            }
            isLoading.postValue(false)
        }
    }
}

class SearchDealersViewModel(private val geocoder: Geocoder, private val dealerPreferencesRepo: DealerPreferencesRepo): SearchViewModel() {

    private val searchLiveData = MediatorLiveData<List<SearchDealer>>()

    override fun search(activity: AppCompatActivity, recyclerView: RecyclerView, query: String) {

        val func = object : Function2<List<ServiceDealer>?, List<UIDealer>?, List<SearchDealer>> {
            override fun apply(input1: List<ServiceDealer>?, input2: List<UIDealer>?): List<SearchDealer> {
                if (input1.isNullOrEmpty()) {
                    return emptyList()
                } else {
                    val dealerNumbersList = input2?.map { it.dealerNumber }
                    return input1.map { it.toDealer(dealerNumbersList?.contains(it.dealerNumber) ?: false) }
                }

            }

        }

        backgroundScope.backgroundTask {
            val addressList = geocoder.getFromLocationName(query, 1)
            if (addressList.isNullOrEmpty().not()) {
                val address = addressList.first()

                val source1 = SearchDealersLiveData(LatLng(address.latitude, address.longitude))
                val source2 = Transformations.map(dealerPreferencesRepo.getSavedDealers()) {
                    return@map it?.map { it.toUIDealer() }
                }

                searchLiveData.addSource(source1) { _ -> searchLiveData.value = func.apply(source1.value, source2.value) }
                searchLiveData.addSource(source2) { _ -> searchLiveData.value = func.apply(source1.value, source2.value) }
            } else {
                searchLiveData.postValue(emptyList())
            }
        }

        searchLiveData.observe(activity, Observer {
            recyclerView.adapter = DealersSearchHintListAdapter(it ?: emptyList()) {
                val intent = Intent()
                intent.putExtra(ChooseEquipmentFragment.KEY_SEARCH_RESULT, it)
                activity.setResult(Activity.RESULT_OK, intent)
                activity.finish()
            }
        })
    }

}