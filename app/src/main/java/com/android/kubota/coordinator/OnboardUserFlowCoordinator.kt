package com.android.kubota.coordinator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.kubota.coordinator.flow.FLOWKIT_BUNDLE_CONTEXT
import com.android.kubota.coordinator.flow.FLOWKIT_BUNDLE_STATE
import com.android.kubota.coordinator.state.*
import com.android.kubota.coordinator.state.OnboardUserState.FromBegin
import com.android.kubota.coordinator.state.OnboardUserState.FromIntro
import com.android.kubota.coordinator.state.OnboardUserState.FromSignIn
import com.android.kubota.coordinator.state.OnboardUserState.FromCreateAccount
import com.android.kubota.ui.flow.account.OnboardUserFlowFragment
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover

class OnboardUserFlowCoordinator: StateMachineActivity<OnboardUserState, OnboardUserType, Boolean>(), OnboardUserStateMachine {

    companion object {
        fun favoriteDealerIntent(context: Context): Intent {
            val intent = Intent(context, OnboardUserFlowCoordinator::class.java)
            intent.putExtra(
                FLOWKIT_BUNDLE_CONTEXT,
                Bundle().put(FLOWKIT_BUNDLE_STATE, OnboardUserState.Begin(context = OnboardUserType.FAVORITE_DEALER))
            )
            return intent
        }
    }

    private var onboardUserType: OnboardUserType? = null

    override fun onBegin(state: OnboardUserState, context: OnboardUserType): Promise<FromBegin> {
        this.onboardUserType = context
        return Promise.value(FromBegin.Intro(context = context))
    }

    override fun onIntro(state: OnboardUserState, context: OnboardUserType): Promise<FromIntro> {
        return this.subflow2(fragment = OnboardUserFlowFragment::class.java, context = context)
                    .map {
                        when (it) {
                            OnboardUserFlowFragment.Result.CREATE_ACCOUNT ->
                                FromIntro.CreateAccount(Unit)
                            OnboardUserFlowFragment.Result.SIGN_IN ->
                                FromIntro.SignIn(Unit)
                            OnboardUserFlowFragment.Result.SKIP ->
                                FromIntro.End(false)
                        }
                    }
    }

    override fun onSignIn(state: OnboardUserState, context: Unit): Promise<FromSignIn> {
        return this.subflow(stateMachine = SignInFlowCoordinator::class.java, state = SignInState.Begin(context = Unit))
                    .map {
                        FromSignIn.End(context = it) as FromSignIn
                    }
                    .recover {
                        this.onboardUserType?.let { type ->
                            Promise.value(FromSignIn.Intro(context = type) as FromSignIn)
                        } ?: throw it
                    }
    }

    override fun onCreateAccount(state: OnboardUserState, context: Unit): Promise<FromCreateAccount> {
        return this.subflow(stateMachine = CreateAccountFlowCoordinator::class.java, state = CreateAccountState.Begin(context=Unit))
                    .map {
                        FromCreateAccount.End(context = it) as FromCreateAccount
                    }
                    .recover {
                        this.onboardUserType?.let { type ->
                            Promise.value(FromCreateAccount.Intro(context = type) as FromCreateAccount)
                        } ?: throw it
                    }
    }

}
