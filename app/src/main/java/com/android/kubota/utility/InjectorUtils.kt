package com.android.kubota.utility

import android.content.Context
import com.android.kubota.MyKubotaApplication
import com.android.kubota.viewmodel.*
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.prefs.DealerPreferencesRepo
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

    fun provideEquipmentDetailViewModel(context: Context): EquipmentDetailViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return EquipmentDetailViewModelFactory(ModelPreferencesRepo(AppDatabase.getInstance(kubotaApp).modelDao()))
    }

    fun provideModelManualViewModel(context: Context): ModelManualViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return ModelManualViewModelFactory(ModelPreferencesRepo(AppDatabase.getInstance(kubotaApp).modelDao()))
    }

    fun provideMyDealersViewModelFactory(context: Context): MyDealersViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return MyDealersViewModelFactory(UserRepo(kubotaApp.pca, AppDatabase.getInstance(kubotaApp).accountDao()), DealerPreferencesRepo(AppDatabase.getInstance(kubotaApp).dealerDao()))
    }

    fun provideDealerDetailsViewModelFactory(context: Context): DealerDetailViewModelFactory {
        val kubotaApp = context.applicationContext as MyKubotaApplication
        return DealerDetailViewModelFactory(DealerPreferencesRepo(AppDatabase.getInstance(kubotaApp).dealerDao()))
    }
}