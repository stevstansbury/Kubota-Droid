package com.android.kubota.utility

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.inmotionsoftware.promisekt.*

fun Fragment.showMessage(@StringRes titleId: Int, @StringRes messageId: Int): Guarantee<Int> {
    val pending = Guarantee.pending<Int>()
    MessageDialogFragment.showMessage(parentFragmentManager, titleId, messageId) {
        pending.second.invoke(it)
        Promise.value(Unit)
    }
    return pending.first
}

fun Fragment.showMessage(title: String, message: String): Guarantee<Int> {
    val pending = Guarantee.pending<Int>()
    MessageDialogFragment.showMessage(parentFragmentManager, title, message) {
        pending.second.invoke(it)
        Promise.value(Unit)
    }
    return pending.first
}

private const val TITLE_RES_KEY = "titleId"
private const val MESSAGE_RES_KEY = "messageId"
private const val TITLE_CHAR_KEY = "title"
private const val MESSAGE_CHAR_KEY = "message"
private const val IS_SIMPLE_KEY = "isSimple"

class MessageDialogFragment(
): DialogFragment() {

    private var pending: Pair<Promise<Unit>, Resolver<Unit>> = Promise.pending()
    private var onButtonAction: ((button: Int) -> Promise<Unit>)? = null

    companion object {
        private const val TAG = "MessageDialogFragment"

        fun showMessage(
            manager: FragmentManager,
            @StringRes titleId: Int,
            @StringRes messageId: Int,
            onButtonAction: ((button: Int) -> Promise<Unit>)? = null
        ): Promise<Unit> {
            val fragment = MessageDialogFragment().apply {
                arguments = Bundle(2).apply {
                    putInt(TITLE_RES_KEY, titleId)
                    putInt(MESSAGE_RES_KEY, messageId)
                }
            }
            fragment.onButtonAction = onButtonAction

            fragment.show(manager, TAG)
            fragment.isCancelable = false
            return fragment.pending.first
        }

        fun showMessage(
            manager: FragmentManager,
            title: String,
            message: String,
            onButtonAction: ((button: Int) -> Promise<Unit>)? = null
        ): Promise<Unit> {
            val fragment = MessageDialogFragment().apply {
                arguments = Bundle(2).apply {
                    putString(TITLE_CHAR_KEY, title)
                    putString(MESSAGE_CHAR_KEY, message)
                }
            }
            fragment.onButtonAction = onButtonAction

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
            val fragment = MessageDialogFragment().apply {
                arguments = Bundle(3).apply {
                    putInt(TITLE_RES_KEY, titleId)
                    putInt(MESSAGE_RES_KEY, messageId)
                    putBoolean(IS_SIMPLE_KEY, true)
                }
            }
            fragment.onButtonAction = onButtonAction

            fragment.show(manager, TAG)
            fragment.isCancelable = false
            return fragment.pending.first
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val title = args.getString(TITLE_CHAR_KEY) ?: getString(args.getInt(TITLE_RES_KEY))
        val message = args.getString(MESSAGE_CHAR_KEY) ?: getString(args.getInt(MESSAGE_RES_KEY))
        val isSimple = args.getBoolean(IS_SIMPLE_KEY, false)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, button ->
                val p = onButtonAction?.let{ it(button) } ?: Promise.value(Unit)
                p.ensure {
                    this.pending.second.fulfill(Unit)
                    dismiss()
                }
            }

        return when (isSimple) {
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
