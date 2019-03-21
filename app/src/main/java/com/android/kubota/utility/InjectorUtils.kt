package com.android.kubota.utility

import android.content.Context
import com.android.kubota.MyKubotaApplication
import com.android.kubota.viewmodel.MyEquipmentViewModelFactory
import com.android.kubota.viewmodel.ProfileViewModelFactory
import com.android.kubota.viewmodel.UserViewModelFactory
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.UserRepo

object InjectorUtils {

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return UserViewModelFactory(UserRepo(kubotaApp.pca, AppDatabase.getInstance(kubotaApp).accountDao()))
    }

    fun provideProfileViewModelFactory(context: Context): ProfileViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return ProfileViewModelFactory(UserRepo(kubotaApp.pca, AppDatabase.getInstance(kubotaApp).accountDao()))
    }

    fun provideMyEquipmentViewModelFactory(context: Context): MyEquipmentViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return MyEquipmentViewModelFactory(UserRepo(kubotaApp.pca, AppDatabase.getInstance(kubotaApp).accountDao()), ModelPreferencesRepo(AppDatabase.getInstance(kubotaApp).modelDao()))
    }
}