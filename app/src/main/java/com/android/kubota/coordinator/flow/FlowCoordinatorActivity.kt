package com.android.kubota.coordinator.flow

import android.content.Intent
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.coordinator.*
import com.android.kubota.coordinator.state.AddEquipmentResult
import com.android.kubota.utility.AuthDelegate
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit


interface FlowCoordinatorDelegate {
    fun startSignInFlow(): Promise<Boolean>
    fun startCreateAccountFlow(): Promise<Boolean>
    fun startChangePasswordFlow(): Promise<Boolean>
    fun startAddEquipmentUnit()
    fun startAddEquipmentUnitFlow(): Promise<AddEquipmentResult?>
    fun startFavoriteDealerOnboardUserFlow(): Promise<Boolean>
}

abstract class FlowCoordinatorActivity
    : AppCompatActivity(), AuthDelegate, FlowCoordinatorDelegate {

    companion object {
        private const val SIGN_IN_FLOW_REQUEST_CODE = 1001
        private const val CREATE_ACCOUNT_FLOW_REQUEST_CODE = 1002
        private const val CHANGE_PASSWORD_FLOW_REQUEST_CODE = 1003
        private const val ADD_EQUIPMENT_FLOW_REQUEST_CODE = 1004
        private const val FAVORITE_DEALER_ONBOARD_USER_FLOW_REQUEST_CODE = 1005
    }

    private var signInFlowPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null
    private var createAccountFlowPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null
    private var changePasswordFlowPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null
    private var addEquipmentFlowPending: Pair<Promise<AddEquipmentResult?>, Resolver<AddEquipmentResult?>>? =
        null
    private var onboardUserFlowPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null
    private var expiredSessionFlowPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null

    private var expiredDialog: AlertDialog? = null

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val resultHandler: ((Any?) -> Unit)? = when (requestCode) {
            SIGN_IN_FLOW_REQUEST_CODE -> { result: Any? ->
                this.signInFlowPending?.second?.fulfill(result == true)
                this.signInFlowPending = null
            }
            CREATE_ACCOUNT_FLOW_REQUEST_CODE -> { result: Any? ->
                this.createAccountFlowPending?.second?.fulfill(result == true)
                this.createAccountFlowPending = null
            }
            CHANGE_PASSWORD_FLOW_REQUEST_CODE -> { result: Any? ->
                this.changePasswordFlowPending?.second?.fulfill(result == true)
                this.changePasswordFlowPending = null
            }
            ADD_EQUIPMENT_FLOW_REQUEST_CODE -> { result: Any? ->
                this.addEquipmentFlowPending?.second?.fulfill(result as? AddEquipmentResult)
                this.addEquipmentFlowPending = null
            }
            FAVORITE_DEALER_ONBOARD_USER_FLOW_REQUEST_CODE -> { result: Any? ->
                this.onboardUserFlowPending?.second?.fulfill(result == true)
                this.onboardUserFlowPending = null
            }
            else -> null
        }

        if (resultHandler != null) {
            val result = data?.getBundleExtra("FLOWKIT_RESULT")?.get("result")
            resultHandler(result)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        if (this.signInFlowPending?.first?.isPending == true) {
            this.signInFlowPending?.second?.fulfill(false)
        }
        if (this.createAccountFlowPending?.first?.isPending == true) {
            this.createAccountFlowPending?.second?.fulfill(false)
        }
        if (this.changePasswordFlowPending?.first?.isPending == true) {
            this.changePasswordFlowPending?.second?.fulfill(false)
        }
        if (this.addEquipmentFlowPending?.first?.isPending == true) {
            this.addEquipmentFlowPending?.second?.fulfill(null)
        }
        if (this.onboardUserFlowPending?.first?.isPending == true) {
            this.onboardUserFlowPending?.second?.fulfill(false)
        }
        if (this.expiredSessionFlowPending?.first?.isPending == true) {
            this.expiredSessionFlowPending?.second?.fulfill(false)
        }
        super.onDestroy()
    }

    override fun startSignInFlow(): Promise<Boolean> {
        if (this.signInFlowPending?.first?.isPending == true)
            return this.signInFlowPending!!.first

        this.signInFlowPending = Promise.pending()
        this.startActivityForResult(
            SignInFlowCoordinator.intent(this),
            SIGN_IN_FLOW_REQUEST_CODE
        )
        return this.signInFlowPending!!.first
    }

    override fun startCreateAccountFlow(): Promise<Boolean> {
        if (this.createAccountFlowPending?.first?.isPending == true)
            return this.createAccountFlowPending!!.first

        this.createAccountFlowPending = Promise.pending()
        this.startActivityForResult(
            CreateAccountFlowCoordinator.intent(this),
            CREATE_ACCOUNT_FLOW_REQUEST_CODE
        )
        return this.createAccountFlowPending!!.first
    }

    override fun startChangePasswordFlow(): Promise<Boolean> {
        if (this.changePasswordFlowPending?.first?.isPending == true)
            return this.changePasswordFlowPending!!.first

        this.changePasswordFlowPending = Promise.pending()
        this.startActivityForResult(
            NewPasswordFlowCoordinator.changePasswordIntent(this),
            CHANGE_PASSWORD_FLOW_REQUEST_CODE
        )
        return this.changePasswordFlowPending!!.first
    }

    override fun startAddEquipmentUnit() {
        this.startAddEquipmentUnitFlow()
            .done { result ->
                when (result) {
                    is AddEquipmentResult.ViewEquipmentModel -> {
                        this.onViewEquipmentModel(result.model)
                    }
                    is AddEquipmentResult.ViewEquipmentUnit -> {
                        this.onEquipmentUnitAdded(result.unit)
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
    }

    override fun startAddEquipmentUnitFlow(): Promise<AddEquipmentResult?> {
        if (this.addEquipmentFlowPending?.first?.isPending == true)
            return this.addEquipmentFlowPending!!.first

        this.addEquipmentFlowPending = Promise.pending()
        this.startActivityForResult(
            AddEquipmentScanFlowCoordinator.intent(this),
            ADD_EQUIPMENT_FLOW_REQUEST_CODE
        )
        return this.addEquipmentFlowPending!!.first
    }

    override fun startFavoriteDealerOnboardUserFlow(): Promise<Boolean> {
        if (this.onboardUserFlowPending?.first?.isPending == true)
            return this.onboardUserFlowPending!!.first

        this.onboardUserFlowPending = Promise.pending()
        this.startActivityForResult(
            OnboardUserFlowCoordinator.favoriteDealerIntent(this),
            FAVORITE_DEALER_ONBOARD_USER_FLOW_REQUEST_CODE
        )
        return this.onboardUserFlowPending!!.first
    }

    override fun authenticateOnSessionExpired(): Promise<Boolean> {
        if (this.expiredSessionFlowPending?.first?.isPending == true)
            return this.expiredSessionFlowPending!!.first

        this.showSessionExpiredMessage()
            .done { button ->
                if (button == AlertDialog.BUTTON_NEGATIVE) {
                    this.signInFlowPending?.second?.fulfill(false)
                } else {
                    this.startSignInFlow()
                        .done {
                            this.expiredSessionFlowPending?.second?.fulfill(it)
                        }
                        .catch {
                            this.expiredSessionFlowPending?.second?.reject(it)
                        }
                }
            }

        return this.signInFlowPending?.first!!
    }

    abstract fun onEquipmentUnitAdded(unit: EquipmentUnit)

    abstract fun onViewEquipmentModel(model: EquipmentModel)

    //    // TODO: Consolidate as an extension on Activity
    private fun showSessionExpiredMessage(): Guarantee<Int> {
        val guarantee = Guarantee.pending<Int>()
        AppProxy.proxy.accountManager.logout()

        if (expiredDialog == null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.session_expired_dialog_title)
                .setMessage(R.string.session_expired_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, button ->
                    dialog.dismiss()
                    expiredDialog = null

                    guarantee.second.invoke(button)
                }
                .setNegativeButton(R.string.cancel) { dialog, button ->
                    dialog.dismiss()
                    expiredDialog = null

                    guarantee.second.invoke(button)
                }
                .show()
        } else {
            guarantee.second.invoke(-2)
        }
        return guarantee.first
    }
}
