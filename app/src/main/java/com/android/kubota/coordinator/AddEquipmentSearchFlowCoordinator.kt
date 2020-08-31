package com.android.kubota.coordinator

import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.flow.hideActivityIndicator
import com.android.kubota.coordinator.flow.showActivityIndicator
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.AddEquipmentSearchState.FromBegin
import com.android.kubota.coordinator.state.AddEquipmentSearchState.FromSearchView
import com.android.kubota.coordinator.state.AddEquipmentSearchState.FromSearch
import com.android.kubota.coordinator.state.AddEquipmentSearchState.FromAddEquipmentUnit
import com.android.kubota.ui.flow.equipment.EquipmentSearchFlowFragment
import com.android.kubota.utility.MessageDialogFragment
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.SearchModelType

class AddEquipmentSearchFlowCoordinator
    : AddEquipmentFlowCoordinator<AddEquipmentSearchState, Unit, AddEquipmentResult>()
    , AddEquipmentSearchStateMachine {

    override fun onBegin(
        state: AddEquipmentSearchState,
        context: Unit
    ): Promise<FromBegin> {
        this.animated = true
        return Promise.value(FromBegin.SearchView(
            context = EquipmentSearchInput(result = emptyList(), error = null)
        ))
    }

    override fun onSearchView(
        state: AddEquipmentSearchState,
        context: EquipmentSearchInput
    ): Promise<FromSearchView> {
        return this.subflow2(EquipmentSearchFlowFragment::class.java, context=context)
            .map {
                when (it) {
                    is EquipmentSearchFlowFragment.Result.Search ->
                        FromSearchView.Search(
                            context= SearchParamsContext(serial=it.serial, modelName=it.modelName)
                        )
                    is EquipmentSearchFlowFragment.Result.Select ->
                        FromSearchView.AddEquipmentUnit(
                            context = AddEquipmentModelContext(serial=it.serial, model = it.model)
                        )
                }
            }
    }

    override fun onSearch(
        state: AddEquipmentSearchState,
        context: SearchParamsContext
    ): Promise<FromSearch> {
        return AppProxy.proxy.serviceManager.equipmentService
            .searchModels(
                SearchModelType.PartialModelAndSerial(partialModel=context.modelName, serial=context.serial)
            )
            .map { models ->
                FromSearch.SearchView(context=EquipmentSearchInput(result=models, error=null))
                        as FromSearch
            }
            .recover {
                Promise.value(FromSearch.SearchView(
                    context = EquipmentSearchInput(result=emptyList(), error=it)))
            }
    }

    override fun onAddEquipmentUnit(
        state: AddEquipmentSearchState,
        context: AddEquipmentModelContext
    ): Promise<FromAddEquipmentUnit> {
        this.animated = true
        this.showActivityIndicator()
        return this.isAuthenticated()
                    .thenMap { authenticated ->
                        if (authenticated) {
                            val request = AddEquipmentUnitType.Serial(
                                serial=context.serial,
                                modelName=context.model.searchModel ?: context.model.model
                            )
                            this.addEquipmentUnitRequest(request)
                                .map {
                                    this.animated = false
                                    FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentUnit(unit = it)) as FromAddEquipmentUnit
                                }
                        } else {
                            Promise.value(
                                FromAddEquipmentUnit.End(AddEquipmentResult.ViewEquipmentModel(model = context.model)) as FromAddEquipmentUnit
                            )
                        }
                    }
                    .recover { error ->
                        MessageDialogFragment
                            .showSimpleMessage(
                                this.supportFragmentManager,
                                titleId = R.string.title_error,
                                messageId = R.string.failed_to_add_equipment
                            )
                            .map {
                                FromAddEquipmentUnit.SearchView(
                                    context = EquipmentSearchInput(result=emptyList(), error=error))
                                        as FromAddEquipmentUnit
                            }
                    }
                    .ensure {
                        this.hideActivityIndicator()
                    }
    }

}
