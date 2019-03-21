package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import com.android.kubota.R
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModel(private val userRepo: UserRepo, private val modelPrefsRepo: ModelPreferencesRepo) : ViewModel() {

    val isUserLoggedIn: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.isGuest()?.not() ?: true
    }

    val preferenceModelList = Transformations.map(modelPrefsRepo.getSavedModels()) {modelList ->
        val results = mutableListOf<UIModel>()

        modelList?.let {
            for (model in it) {

                results.add(when (model.category) {
                    "Construction" -> UIModel(model.model, model.serialNumber, R.string.equipment_construction_category, R.drawable.ic_construction_category_thumbnail)
                    "Mowers" -> UIModel(model.model, model.serialNumber, R.string.equipment_mowers_category, R.drawable.ic_mower_category_thumbnail)
                    "Tractors" -> UIModel(model.model, model.serialNumber, R.string.equipment_tractors_category, R.drawable.ic_tractor_category_thumbnail)
                    "Utility Vehicles" -> UIModel(model.model, model.serialNumber, R.string.equipment_utv_category, R.drawable.ic_utv_category_thumbnail)
                    else -> UIModel(model.model, model.serialNumber, 0, 0)
                })
            }
        }

        return@map results
    }
}

data class UIModel(val modelName: String, val serialNumber: String?, @StringRes val categoryResId: Int, @DrawableRes val imageResId: Int)