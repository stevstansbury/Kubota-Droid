package com.android.kubota.utilities

import android.content.Context
import com.android.kubota.MyKubotaApplication
import com.android.kubota.viewmodel.UserViewModelFactory
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.user.UserRepo

object InjectorUtils {

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return UserViewModelFactory(UserRepo(kubotaApp.pca, AppDatabase.getInstance(kubotaApp).accountDao()))
    }
}