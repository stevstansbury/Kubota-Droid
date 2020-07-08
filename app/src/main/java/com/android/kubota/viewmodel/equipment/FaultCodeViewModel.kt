package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.kubota.service.domain.FaultCode

class FaultCodeViewModelFactory(
    private val faultCode: FaultCode
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return FaultCodeViewModel(faultCode) as T
    }

}

class FaultCodeViewModel(
    faultCode: FaultCode
): ViewModel() {

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mFaultCode = MutableLiveData(faultCode)
    private val mActionButtonEnable = MutableLiveData<Boolean>(true)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val actionButtonEnable: LiveData<Boolean> = mActionButtonEnable

    val faultCodeAction: LiveData<String> = Transformations.map(mFaultCode) {
        return@map it?.provisionalMeasure
    }

    val faultCodeDescription: LiveData<String> = Transformations.map(mFaultCode) {
        return@map it?.description
    }

}