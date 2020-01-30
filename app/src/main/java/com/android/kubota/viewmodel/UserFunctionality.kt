package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.kubota.repository.user.UserRepo

interface LoggedIn {
    val userRepo: UserRepo

    val isUserLoggedIn: LiveData<Boolean>
}

class LoggedInDelegate(override val userRepo: UserRepo) : LoggedIn {
    override val isUserLoggedIn: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.isGuest()?.not() ?: false
    }
}

interface AddPreference: LoggedIn {

    val numberOfSavedPreferences: LiveData<Int>

    val canAddPreference: LiveData<Boolean>
        get() {
            val result = MediatorLiveData<Boolean>()

            val func = object : Function2<Boolean?, Int?, Boolean> {
                override fun apply(input1: Boolean?, input2: Int?): Boolean {
                    if (input1 == true) return true

                    return (input2 ?: 0) == 0
                }

            }

            result.addSource(isUserLoggedIn) { result.value = func.apply(isUserLoggedIn.value, numberOfSavedPreferences.value) }
            result.addSource(numberOfSavedPreferences) { result.value = func.apply(isUserLoggedIn.value, numberOfSavedPreferences.value) }

            return result
        }
}