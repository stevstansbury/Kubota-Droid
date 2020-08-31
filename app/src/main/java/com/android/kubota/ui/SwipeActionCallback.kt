package com.android.kubota.ui

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.graphics.*


abstract class SwipeActionCallback(private val leftSwipeAction: SwipeAction, private val rightSwipeAction: SwipeAction): ItemTouchHelper.Callback() {

    private val background = ColorDrawable()
    private val clearPaint = Paint()

    init {
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun getMovementFlags(p0: RecyclerView, p1: RecyclerView.ViewHolder): Int = makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

    override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean = false

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, clearPaint)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val itemHeight = itemView.height

        val isCancelled = dX == 0f && !isCurrentlyActive

        if (isCancelled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val swipeAction = if (dX > 0f) leftSwipeAction else rightSwipeAction

        background.color = swipeAction.backgroundColor
        if (dX > 0f) {
            background.setBounds(0, itemView.top, dX.toInt(), itemView.bottom)
        } else {
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }
        background.draw(c)

        val deleteIconTop = itemView.top + (itemHeight - swipeAction.intrinsicHeight) / 2
        val deleteIconMargin = (itemHeight - swipeAction.intrinsicHeight) / 2
        val deleteIconLeft = if (dX > 0f) deleteIconMargin - swipeAction.intrinsicWidth else itemView.right - deleteIconMargin - swipeAction.intrinsicWidth
        val deleteIconRight = if (dX > 0f) deleteIconMargin else itemView.right - deleteIconMargin
        val deleteIconBottom = deleteIconTop + swipeAction.intrinsicHeight

        swipeAction.actionDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        swipeAction.actionDrawable.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

}

data class SwipeAction(val actionDrawable: Drawable, val backgroundColor: Int) {
    val intrinsicWidth: Int = actionDrawable.intrinsicWidth
    val intrinsicHeight: Int = actionDrawable.intrinsicHeight

}