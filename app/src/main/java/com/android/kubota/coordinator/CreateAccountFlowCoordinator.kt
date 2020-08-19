package com.android.kubota.coordinator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.app.AppProxy
import com.android.kubota.app.account.AccountError
import com.android.kubota.coordinator.flow.*
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.CreateAccountState.FromBegin
import com.android.kubota.coordinator.state.CreateAccountState.FromPrompt
import com.android.kubota.coordinator.state.CreateAccountState.FromTermsAndConditions
import com.android.kubota.coordinator.state.CreateAccountState.FromCreateAccount
import com.android.kubota.ui.LegalMode
import com.android.kubota.ui.WebViewFlowFragment
import com.android.kubota.ui.context
import com.android.kubota.ui.flow.account.CreateAccountFlowFragment
import com.android.kubota.ui.flow.account.LegalTermsFlowFragment
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.api.KubotaServiceError

class CreateAccountFlowCoordinator
    : StateMachineFlowCoordinator<CreateAccountState, Unit, Boolean>()
    , CreateAccountStateMachine {

    companion object {
        fun intent(context: Context): Intent {
            val intent = Intent(context, CreateAccountFlowCoordinator::class.java)
            intent.putExtra(
                FLOWKIT_BUNDLE_CONTEXT,
                Bundle().put(FLOWKIT_BUNDLE_STATE, CreateAccountState.Begin(context = Unit))
            )
            return intent
        }
    }

    override fun onBegin(
        state: CreateAccountState,
        context: Unit
    ): Promise<FromBegin> {
        return Promise.value(FromBegin.Prompt(context=null))
    }

    override fun onPrompt(
        state: CreateAccountState,
        context: Throwable?
    ): Promise<FromPrompt> {
        return this.subflow2(CreateAccountFlowFragment::class.java, context=context)
            .map {
                when (it) {
                    is CreateAccountFlowFragment.Result.CreateAccount ->
                        FromPrompt.CreateAccount(
                            context = CreateAccountContext(
                                email = it.email,
                                password = it.password,
                                phoneNumber = it.phoneNumber
                            )
                        )
                    is CreateAccountFlowFragment.Result.TermsAndConditions ->
                        FromPrompt.TermsAndConditions(context=Unit)
                }
            }
    }

    override fun onTermsAndConditions(
        state: CreateAccountState,
        context: Unit
    ): Promise<FromTermsAndConditions> {
        return this.subflow2(WebViewFlowFragment::class.java, context=LegalMode.TERMS_OF_USE_MODE.context())
                    .map {
                        FromTermsAndConditions.Prompt(context = null)
                            as FromTermsAndConditions
                    }
                    .recover {
                        Promise.value(FromTermsAndConditions.Prompt(context = null))
                    }
    }

    override fun onCreateAccount(
        state: CreateAccountState,
        context: CreateAccountContext
    ): Promise<FromCreateAccount> {
        this.showBlockingActivityIndicator()
        return AppProxy.proxy.accountManager.createAccount(email=context.email, password=context.password, phoneNumber = context.phoneNumber)
                        .map {
                            FromCreateAccount.End(context = true) as FromCreateAccount
                        }
                        .recover {
                            val error = it as? KubotaServiceError
                            if (error == null) { throw it }
                            when (error) {
                                is KubotaServiceError.BadRequest -> {
                                    when {
                                        error.message?.contains("Blacklisted password") == true -> throw AccountError.BlacklistedPassword(error.message ?: "")
                                        error.message?.contains("Not a valid phone number") == true -> throw AccountError.InvalidPhoneNumber(error.message ?: "")
                                        error.message?.contains("Not a mobile phone number") == true -> throw AccountError.NotMobilePhoneNumber(error.message ?: "")
                                        else -> throw AccountError.InvalidPassword(error.message ?: "")
                                    }
                                }
                                is KubotaServiceError.Conflict -> throw AccountError.AccountExists(error.message ?: "")
                                else -> throw error
                            }
                        }
                        .recover {
                            Promise.value(FromCreateAccount.Prompt(context = it))
                        }
                        .ensure {
                            this.hideBlockingActivityIndicator()
                        }
    }

}
