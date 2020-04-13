package com.android.kubota.utility

import android.content.Context
import com.android.kubota.MyKubotaApplication
import com.android.kubota.viewmodel.*
import com.android.kubota.viewmodel.ftue.CreateAccountViewModelFactory
import com.android.kubota.viewmodel.ftue.SignInViewModelFactory
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.user.UserRepo

object InjectorUtils {

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        return UserViewModelFactory(
            context.createUserRepo()
        )
    }

    fun provideProfileViewModelFactory(context: Context): ProfileViewModelFactory {
        return ProfileViewModelFactory(
            context.createUserRepo()
        )
    }

    fun provideMyEquipmentViewModelFactory(context: Context): MyEquipmentViewModelFactory {
        return MyEquipmentViewModelFactory(
            context.createUserRepo(),
            context.createEquipmentPreferencesRepo()
        )
    }

    fun provideEquipmentDetailViewModel(context: Context, equipmentId: Int): EquipmentDetailViewModelFactory {
        return EquipmentDetailViewModelFactory(
            context.createEquipmentPreferencesRepo(),
            equipmentId
        )
    }

    fun provideEngineHoursViewModel(context: Context, equipmentId: Int): EngineHoursViewModelFactory {
        return EngineHoursViewModelFactory(
            context.createEquipmentPreferencesRepo(),
            equipmentId
        )
    }

    fun provideFaultCodeInquiryViewModel(context: Context, equipmentId: Int): FaultCodeInquiryViewModelFactory {
        return FaultCodeInquiryViewModelFactory(
            context.createEquipmentPreferencesRepo(),
            equipmentId
        )
    }

    fun provideModelManualViewModel(context: Context): ModelManualViewModelFactory {
        return ModelManualViewModelFactory(
            context.createEquipmentPreferencesRepo()
        )
    }

    fun provideMyDealersViewModelFactory(context: Context): MyDealersViewModelFactory {
        return MyDealersViewModelFactory(
            context.createUserRepo(),
            context.createDealerPreferencesRepo()
        )
    }

    fun provideDealerDetailsViewModelFactory(context: Context): DealerDetailViewModelFactory {
        return DealerDetailViewModelFactory(
            context.createUserRepo(),
            context.createDealerPreferencesRepo()
        )
    }

    fun provideChooseEquipmentViewModel(): ChooseEquipmentViewModelFactory {
        return ChooseEquipmentViewModelFactory(CategoryModelService())
    }

    fun provideAddEquipmentViewModel(context: Context): AddEquipmentViewModelFactory {
        return AddEquipmentViewModelFactory(
            context.createEquipmentPreferencesRepo()
        )
    }

    fun provideDealerLocatorViewModel(context: Context): DealerLocatorViewModelFactory {
        return DealerLocatorViewModelFactory(
            context.createUserRepo(),
            context.createDealerPreferencesRepo()
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
            context.createUserRepo()
        )
    }

    fun provideHoursToService(context: Context, equipmentId: Int): HoursToServiceViewModelFactory {
        return HoursToServiceViewModelFactory(
            context.createEquipmentPreferencesRepo(),
            equipmentId
        )
    }

    fun provideCreateAccountViewModel() =
        CreateAccountViewModelFactory()
}

private fun Context.getKubotaApplication() = this.applicationContext as MyKubotaApplication

private fun Context.createEquipmentPreferencesRepo(): EquipmentPreferencesRepo {
    val appDatabase = AppDatabase.getInstance(this.applicationContext)
    return EquipmentPreferencesRepo(
        appDatabase.equipmentDao(),
        appDatabase.faultCodeDao()
    )
}

private fun Context.createUserRepo(): UserRepo {
    val appDatabase = AppDatabase.getInstance(this.applicationContext)
    return UserRepo(
        appDatabase.accountDao()
    )
}

private fun Context.createDealerPreferencesRepo(): DealerPreferencesRepo {
    val appDatabase = AppDatabase.getInstance(this.applicationContext)
    return DealerPreferencesRepo(
        appDatabase.dealerDao()
    )
}