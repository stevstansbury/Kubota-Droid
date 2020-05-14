package com.android.kubota.utility

import android.content.Context
import com.android.kubota.app.AppProxy
import com.android.kubota.viewmodel.*
import com.android.kubota.viewmodel.ftue.CreateAccountViewModelFactory
import com.android.kubota.viewmodel.ftue.ForgotPasswordViewModelFactory
import com.android.kubota.viewmodel.ftue.NewPasswordViewModelFactory
import com.android.kubota.viewmodel.ftue.SignInViewModelFactory
import com.android.kubota.viewmodel.resources.*
import com.kubota.repository.data.AppDatabase
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.user.ModelSuggestionRepo
import com.kubota.repository.user.UserRepo
import java.util.*

object InjectorUtils {

    fun provideUserViewModelFactory(context: Context): UserViewModelFactory {
        return UserViewModelFactory(
            context.createUserRepo()
        )
    }

    fun provideProfileViewModelFactory(context: Context): ProfileViewModelFactory {
        return ProfileViewModelFactory()
    }

    fun provideMyEquipmentViewModelFactory(context: Context): MyEquipmentViewModelFactory {
        return MyEquipmentViewModelFactory()
    }

    fun provideEquipmentDetailViewModel(context: Context, equipmentId: UUID): EquipmentDetailViewModelFactory {
        return EquipmentDetailViewModelFactory(
            equipmentId
        )
    }

    fun provideEngineHoursViewModel(context: Context, equipmentId: UUID): EngineHoursViewModelFactory {
        return EngineHoursViewModelFactory(
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

    fun provideEquipmentCategoriesViewModel(context: Context): EquipmentCategoriesViewModelFactory {
        AppDatabase.getInstance(context.applicationContext).apply {
            return EquipmentCategoriesViewModelFactory(
                CategoryModelService(),
                context.createModelSuggestionsRepo()
            )
        }
    }

    fun provideEquipmentSubCategoriesViewModel(): EquipmentSubCategoriesViewModelFactory {
        return EquipmentSubCategoriesViewModelFactory(
            CategoryModelService()
        )
    }

    fun provideModelDetailViewModel(context: Context): ModelDetailViewModelFactory {
        return ModelDetailViewModelFactory(context.createModelSuggestionsRepo())
    }

    fun provideDealerLocatorViewModel(context: Context): DealerLocatorViewModelFactory {
        return DealerLocatorViewModelFactory(
            context.createUserRepo(),
            context.createDealerPreferencesRepo()
        )
    }

    fun provideSignInViewModel(context: Context): SignInViewModelFactory {
        return SignInViewModelFactory()
    }

    fun provideForgotPasswordViewModelFactory() = ForgotPasswordViewModelFactory()

    fun provideNewPasswordViewModelFactory(accessToken: String) = NewPasswordViewModelFactory(accessToken)

    fun provideHoursToService(context: Context, equipmentId: Int): HoursToServiceViewModelFactory {
        return HoursToServiceViewModelFactory(
            context.createEquipmentPreferencesRepo(),
            equipmentId
        )
    }

    fun provideCreateAccountViewModel() =
        CreateAccountViewModelFactory()
}

private fun Context.getKubotaApplication() = this.applicationContext as AppProxy

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

private fun Context.createModelSuggestionsRepo(): ModelSuggestionRepo {
    return ModelSuggestionRepo(
        AppDatabase.getInstance(this.applicationContext).modelSuggestionsDao()
    )
}