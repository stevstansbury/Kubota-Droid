package com.android.kubota.ui

import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter

object DataBindingAdapters {

    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: TextView, @StringRes resId: Int) {
        if (resId != 0) {
            view.setText(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("android:textColor")
    fun setTextColor(view: TextView, @ColorRes resId: Int) {
        if (resId != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.setTextColor(view.resources.getColor(resId, view.context.theme))
            } else {
                view.setTextColor(view.resources.getColor(resId))
            }
        }
    }


    @JvmStatic
    @BindingAdapter("android:src")
    fun setImageSrc(view: ImageView, @DrawableRes resId: Int) {
        if (resId != 0) {
            view.setImageResource(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("android:background")
    fun setBackgroundSrc(view: View, @DrawableRes resId: Int) {
        if (resId != 0) {
            view.setBackgroundResource(resId)
        }
    }
}