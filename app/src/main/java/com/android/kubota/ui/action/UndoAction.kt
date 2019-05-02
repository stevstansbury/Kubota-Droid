package com.android.kubota.ui.action

interface UndoAction {
    fun commit()

    fun undo()
}