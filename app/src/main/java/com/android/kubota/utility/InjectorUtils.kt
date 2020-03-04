package com.android.kubota.utility

import android.content.Context
import com.android.kubota.MyKubotaApplication
import com.android.kubota.viewmodel.*
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.user.UserRepo

object InjectorUtils {

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return UserViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao())
        )
    }

    fun provideProfileViewModelFactory(context: Context): ProfileViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return ProfileViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao())
        )
    }

    fun provideMyEquipmentViewModelFactory(context: Context): MyEquipmentViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return MyEquipmentViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao()),
            EquipmentPreferencesRepo(AppDatabase.getInstance(kubotaApp).equipmentDao())
        )
    }

    fun provideEquipmentDetailViewModel(context: Context): EquipmentDetailViewModelFactory {
        return EquipmentDetailViewModelFactory(
            EquipmentPreferencesRepo(
                AppDatabase.getInstance(context.getKubotaApplication()).equipmentDao()
            )
        )
    }

    fun provideEngineHoursViewModel(context: Context, equipmentId: Int): EngineHoursViewModelFactory {
        return EngineHoursViewModelFactory(
            EquipmentPreferencesRepo(
                AppDatabase.getInstance(context.getKubotaApplication()).equipmentDao()
            ),
            equipmentId
        )
    }

    fun provideModelManualViewModel(context: Context): ModelManualViewModelFactory {
        return ModelManualViewModelFactory(
            EquipmentPreferencesRepo(
                AppDatabase.getInstance(context.getKubotaApplication()).equipmentDao()
            )
        )
    }

    fun provideMyDealersViewModelFactory(context: Context): MyDealersViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return MyDealersViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao()),
            DealerPreferencesRepo(AppDatabase.getInstance(kubotaApp).dealerDao())
        )
    }

    fun provideDealerDetailsViewModelFactory(context: Context): DealerDetailViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return DealerDetailViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao()),
            DealerPreferencesRepo(AppDatabase.getInstance(kubotaApp).dealerDao())
        )
    }

    fun provideChooseEquipmentViewModel(): ChooseEquipmentViewModelFactory {
        // TODO: are we gonna save categories in the app?
        return ChooseEquipmentViewModelFactory(CategoryModelService())
    }

    fun provideAddEquipmentViewModel(context: Context): AddEquipmentViewModelFactory {
        return AddEquipmentViewModelFactory(
            EquipmentPreferencesRepo(
                AppDatabase.getInstance(context.getKubotaApplication()).equipmentDao()
            )
        )
    }

    fun provideDealerLocatorViewModel(context: Context): DealerLocatorViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return DealerLocatorViewModelFactory(
            UserRepo(AppDatabase.getInstance(kubotaApp).accountDao()),
            DealerPreferencesRepo(AppDatabase.getInstance(kubotaApp).dealerDao())
        )
    }

    fun provideSearchEquipmentViewModel(): SearchEquipmentViewModelFactory {
        return SearchEquipmentViewModelFactory(CategoryModelService())
    }

    fun provideSearchDealerViewModel(): SearchDealersViewFactory {
        return SearchDealersViewFactory()
    }

    fun provideSignInViewModel(context: Context): SignInViewModelFactory {
        val kubotaApp = context.getKubotaApplication()
        return SignInViewModelFactory(
          kubotaApp,
          UserRepo(AppDatabase.getInstance(kubotaApp).accountDao())
        )
    }
}

private fun Context.getKubotaApplication() = this.applicationContext as MyKubotaApplication
