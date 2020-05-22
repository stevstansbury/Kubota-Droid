package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.FaultCode

class FaultCodeViewModelFactory(
    private val model: String,
    private val faultCode: Int
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return FaultCodeViewModel(model, faultCode) as T
    }

}

class FaultCodeViewModel(
    private val model: String,
    private val faultCode: Int
): ViewModel() {

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mFaultCode = MutableLiveData<FaultCode>()
    private val mActionButtonEnable = MutableLiveData<Boolean>(false)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val actionButtonEnable: LiveData<Boolean> = mActionButtonEnable

    val faultCodeAction: LiveData<String> = Transformations.map(mFaultCode) {
        return@map it?.provisionalMeasure
    }

    val faultCodeDescription: LiveData<String> = Transformations.map(mFaultCode) {
        return@map it?.description
    }

    init {
        getFaultCode()
    }

    private fun getFaultCode() {
        mIsLoading.postValue(true)
        AppProxy.proxy.serviceManager.equipmentService.getFaultCodes(model = model, codes = listOf("$faultCode"))
            .done {
                mFaultCode.postValue(it.first())
                mActionButtonEnable.postValue(true)
            }
            .ensure { mIsLoading.postValue(false) }
            .catch { mError.postValue(it) }
    }
}