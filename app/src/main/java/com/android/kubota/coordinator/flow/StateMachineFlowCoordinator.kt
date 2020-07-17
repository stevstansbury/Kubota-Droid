package com.android.kubota.coordinator.flow

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.coordinator.SignInFlowCoordinator
import com.android.kubota.coordinator.flow.util.BlockingActivityIndicator
import com.android.kubota.coordinator.state.SignInState
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.PermissionRequestManager
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.flowkit.FlowState
import com.inmotionsoftware.flowkit.android.FlowViewModel
import com.inmotionsoftware.flowkit.android.StateMachineActivity
import com.inmotionsoftware.promisekt.*

const val FLOWKIT_BUNDLE_CONTEXT = "com.inmotionsoftware.flowkit.Context"
const val FLOWKIT_BUNDLE_STATE = "state"

abstract class StateMachineFlowCoordinator<S: FlowState,I,O>: StateMachineActivity<S, I, O>() {

    var animated: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_flow_coordinator)
        findViewById<View>(R.id.toolbarWithLogo).visibility = View.GONE

        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun requestPermission(permission: String, message: Int) =
        PermissionRequestManager.requestPermission(activity=this, permission=permission, message=message)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun createFragmentContainerView(): Int = R.id.fragmentPane

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        super.finish()
        if (!animated) {
            this.overridePendingTransition(0, 0)
        }
    }
}

//
// AuthStateMachineFlowCoordinator
//

abstract class AuthStateMachineFlowCoordinator<S: FlowState, I, O>: StateMachineFlowCoordinator<S, I, O>(), AuthDelegate {

    private var authSessionPending: Pair<Promise<Boolean>, Resolver<Boolean>>? = null

    override fun onDestroy() {
        if (this.authSessionPending?.first?.isPending == true) {
            this.authSessionPending?.second?.fulfill(false)
        }
        super.onDestroy()
    }

    //
    // AuthDelegate
    //

    override fun authenticateOnSessionExpired(): Promise<Boolean> {
        if (this.authSessionPending?.first?.isPending == true) {
            return this.authSessionPending?.first!!
        }

        this.authSessionPending = Promise.pending()

        this.showSessionExpiredMessage()
            .done { button ->
                if (button == AlertDialog.BUTTON_NEGATIVE) {
                    this.authSessionPending?.second?.fulfill(false)
                } else {
                    this.subflow(stateMachine = SignInFlowCoordinator::class.java, state = SignInState.Begin(Unit))
                        .recover {
                            Promise.value(false)
                        }
                        .done {
                            this.authSessionPending?.second?.fulfill(it)
                        }
                }
            }

        return this.authSessionPending?.first!!
    }

    // TODO: Consolidate as an extension on Activity
    private fun showSessionExpiredMessage(): Guarantee<Int> =
        showMessageDialog(
            title = R.string.session_expired_dialog_title,
            message = R.string.session_expired_dialog_message,
            cancelable = false
        )

    fun showMessageDialog(@StringRes title: Int, @StringRes message: Int): Guarantee<Unit> =
        showMessageDialog(
            title = title,
            message = message,
            cancelable = false
        ).guaranteeDone { Unit }

    fun showMessageDialog(@StringRes title: Int, @StringRes message: Int, cancelable: Boolean): Guarantee<Int> {
        val guarantee = Guarantee.pending<Int>()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, button ->
                dialog.dismiss()
                guarantee.second.invoke(button)
            }
            .setCancelable(cancelable) { dialog, button ->
                dialog.dismiss()
                guarantee.second.invoke(button)
            }.show()
        return guarantee.first
    }
}

//
// Extension
//


private fun AlertDialog.Builder.setCancelable(cancelable: Boolean, listener: (DialogInterface, Int) -> Unit ): AlertDialog.Builder {
    this.setCancelable(cancelable)
    if (cancelable) {
        this.setNegativeButton(R.string.cancel, listener)
    }
    return this
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.showToolbar() {
    findViewById<Toolbar>(R.id.toolbar)?.visibility = View.VISIBLE
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.hideToolbar() {
    findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.showBlockingActivityIndicator() {
    this.hideBlockingActivityIndicator()
    BlockingActivityIndicator().show(this.supportFragmentManager, BlockingActivityIndicator.TAG)
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.hideBlockingActivityIndicator() {
    val fragment = this.supportFragmentManager.findFragmentByTag(BlockingActivityIndicator.TAG)
    (fragment as? BlockingActivityIndicator)?.dismiss()
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.showActivityIndicator() {
    findViewById<ProgressBar>(R.id.flowCoordinatorProgressBar)?.visibility = View.VISIBLE
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.hideActivityIndicator() {
    findViewById<ProgressBar>(R.id.flowCoordinatorProgressBar)?.visibility = View.GONE
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.showToast(@StringRes resId: Int) {
    this.showToast(message = getString(resId))
}

fun <S: FlowState,I,O> StateMachineActivity<S,I,O>.showToast(message: String) {
    val rootView: View = findViewById(R.id.flowCoordinatorView)
    val snackBar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT).apply {
        val lp = view.layoutParams as CoordinatorLayout.LayoutParams
        val xMargin = resources.getDimension(R.dimen.snack_bar_horizontal_margin).toInt()
        val yMargin = resources.getDimension(R.dimen.snack_bar_vertical_margin).toInt()

        lp.setMargins(
            lp.leftMargin + xMargin,
            lp.topMargin,
            lp.rightMargin + xMargin,
            lp.bottomMargin + yMargin
        )
        view.layoutParams = lp
        view.setBackgroundResource(R.drawable.snack_bar_background)
    }
    snackBar.setText(message)
    snackBar.show()
}

fun <S: FlowState, I, O, I2, O2, F: FlowDialogFragment<I2, O2>> StateMachineActivity<S, I, O>.modalSubflow(fragment: F, context: I2): Promise<O2> {
    if (this.isDestroyed) {
        return Promise(error= IllegalStateException("Trying to add fragment to destroyed Activity"))
    }

    val pending = Promise.pending<O2>()
    this.runOnUiThread {
        fragment.show(this.supportFragmentManager, fragment.javaClass.simpleName)
        @Suppress("UNCHECKED_CAST")
        val viewModel = ViewModelProvider(this).get(FlowViewModel::class.java) as FlowViewModel<I2, O2>
        viewModel.input.value = context
        viewModel.resolver = pending.second
    }
    return pending.first
}
