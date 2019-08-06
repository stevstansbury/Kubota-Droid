package com.android.kubota.utility

import androidx.appcompat.view.ActionMode
import android.view.Menu

abstract class MultiSelectorActionCallback : ActionMode.Callback {

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        // Called each time the action mode is shown
        return false
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        // Called when the action mode is finished
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {}
}
