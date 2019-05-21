package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Transformations
import com.kubota.repository.user.UserRepo

interface LoggedIn {
    val userRepo: UserRepo

    val isUserLoggedIn: LiveData<Boolean>
        get() = Transformations.map(userRepo.getAccount()) {
            return@map it?.isGuest()?.not() ?: true
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

            result.addSource(isUserLoggedIn) { _ -> result.value = func.apply(isUserLoggedIn.value, numberOfSavedPreferences.value) }
            result.addSource(numberOfSavedPreferences) { _ -> result.value = func.apply(isUserLoggedIn.value, numberOfSavedPreferences.value) }

            return result
        }
}