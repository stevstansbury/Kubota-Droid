package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import com.android.kubota.extensions.toUIModel
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Account
import com.kubota.repository.data.Model
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModel(override val userRepo: UserRepo, private val modelPrefsRepo: ModelPreferencesRepo) : ViewModel(), LoggedIn  by LoggedInDelegate(userRepo) {
    private var modelList = emptyList<Model>()

    val isLoading: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.flags == Account.FLAGS_SYNCING
    }

    val preferenceModelList = Transformations.map(modelPrefsRepo.getSavedModels()) {modelList ->
        val results = mutableListOf<UIModel>()
        this.modelList = modelList ?: emptyList()
        modelList?.forEach {
            results.add(it.toUIModel())
        }

        return@map results
    }

    fun createDeleteAction(model: UIModel): UndoAction {
        val temp = modelList.find { m -> model.id == m.id}
        return object : UndoAction {
            private val repoModel= temp

            override fun commit() {
                repoModel?.let {
                    Utils.backgroundTask { modelPrefsRepo.deleteModel(repoModel) }
                }
            }

            override fun undo() {
                repoModel?.let {
                    Utils.backgroundTask { modelPrefsRepo.insertModel(repoModel) }
                }
            }
        }
    }

    fun createMultiDeleteAction(models: List<UIModel>): UndoAction {
        return object : UndoAction{
            private var deleteList = mutableListOf<Model>()
            override fun commit() {
                //for each UIModel, check if there is a matching Model
                models.forEach {model->
                    val item = modelList.find { m -> model.id == m.id}
                    item?.let {
                        deleteList.add(it)
                    }
                }
                Utils.backgroundTask {
                    deleteList.forEach{model->
                        modelPrefsRepo.deleteModel(model)
                    }
                }
            }

            override fun undo() {
                Utils.backgroundTask {
                    deleteList.forEach { model->
                        modelPrefsRepo.insertModel(model)
                    }
                }
            }
        }
    }

    fun getUpdatedEquipmentList(){
        userRepo.syncAccount()
    }
}

data class UIModel(val id: Int, val modelName: String, val serialNumber: String?, @StringRes val categoryResId: Int,
                   @DrawableRes val imageResId: Int, val hasManual: Boolean, val hasMaintenanceGuides: Boolean): Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        modelName = parcel.readString() as String,
        serialNumber = parcel.readString(),
        categoryResId = parcel.readInt(),
        imageResId = parcel.readInt(),
        hasManual = parcel.readInt() == 1,
        hasMaintenanceGuides =  parcel.readInt() == 1
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(modelName)
        parcel.writeString(serialNumber)
        parcel.writeInt(categoryResId)
        parcel.writeInt(imageResId)
        parcel.writeInt(if (hasManual) 1 else 0)
        parcel.writeInt(if (hasMaintenanceGuides) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UIModel> {
        override fun createFromParcel(parcel: Parcel): UIModel {
            return UIModel(parcel)
        }

        override fun newArray(size: Int): Array<UIModel?> {
            return arrayOfNulls(size)
        }
    }

}