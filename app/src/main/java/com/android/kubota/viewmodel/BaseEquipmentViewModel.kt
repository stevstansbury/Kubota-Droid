package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

abstract class BaseEquipmentViewModel: ViewModel() {

    abstract val equipmentImage: LiveData<Int>

    abstract val equipmentModel: LiveData<String>
}