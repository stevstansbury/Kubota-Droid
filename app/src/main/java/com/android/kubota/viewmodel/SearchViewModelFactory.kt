package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.location.Geocoder
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.service.CategoryModelService

class SearchEquipmentViewModelFactory(private val categoryService: CategoryModelService): ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SearchEquipmentViewModel(categoryService) as T
    }
}

class SearchDealersViewFactory(private val geocoder: Geocoder, private val dealerPreferencesRepo: DealerPreferencesRepo): ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SearchDealersViewModel(geocoder, dealerPreferencesRepo) as T
    }
}