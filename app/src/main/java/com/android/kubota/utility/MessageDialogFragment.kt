package com.android.kubota.utility

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.Resolver
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.fulfill

class MessageDialogFragment(
    @StringRes private val titleId: Int,
    @StringRes private val messageId: Int,
    private val isSimple: Boolean = false,
    private val onButtonAction: ((button: Int) -> Promise<Unit>)? = null
): DialogFragment() {

    private var pending: Pair<Promise<Unit>, Resolver<Unit>> = Promise.pending()

    companion object {
        private const val TAG = "MessageDialogFragment"

        fun showMessage(
            manager: FragmentManager,
            @StringRes titleId: Int,
            @StringRes messageId: Int,
            onButtonAction: ((button: Int) -> Promise<Unit>)? = null
        ): Promise<Unit> {
            val fragment = MessageDialogFragment(titleId, messageId, false, onButtonAction)
            fragment.show(manager, TAG)
            fragment.isCancelable = false
            return fragment.pending.first
        }

        fun showSimpleMessage(
            manager: FragmentManager,
            @StringRes titleId: Int,
            @StringRes messageId: Int,
            onButtonAction: ((button: Int) -> Promise<Unit>)? = null
        ): Promise<Unit> {
            val fragment = MessageDialogFragment(titleId, messageId, true, onButtonAction)
            fragment.show(manager, TAG)
            fragment.isCancelable = false
            return fragment.pending.first
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(this.titleId)
            .setMessage(this.messageId)
            .setPositiveButton(android.R.string.ok) { _, button ->
                val p = onButtonAction?.let{ it(button) } ?: Promise.value(Unit)
                p.ensure {
                    this.pending.second.fulfill(Unit)
                    dismiss()
                }
            }

        return when (this.isSimple) {
            true -> dialog.create()
            else -> {
                dialog.setNegativeButton(android.R.string.cancel) { _, button ->
                    val p = onButtonAction?.let { it(button) } ?: Promise.value(Unit)
                    p.ensure {
                        this.pending.second.fulfill(Unit)
                        dismiss()
                    }
                }.create()
            }
        }
    }

}
