package com.android.kubota.ui

import android.graphics.Typeface
import android.os.Build
import android.text.format.Time
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import com.android.kubota.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DataBindingAdapters {

    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: TextView, @StringRes resId: Int) {
        if (resId != 0) {
            view.setText(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: TextView, date: Date) {
        val calendar = Calendar.getInstance()
        if (isToday(date.time, calendar.timeInMillis, calendar.timeZone.rawOffset.toLong())) {
            view.setText(R.string.today)
        } else {
            view.text = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        }
    }

    @JvmStatic
    @BindingAdapter("android:textStyle")
    fun setTextStyle(view: TextView, isRead: Boolean) {
        when (view.id) {
            R.id.date -> view.setTypeface(view.typeface, if (isRead) Typeface.BOLD_ITALIC else Typeface.BOLD_ITALIC)
            R.id.title -> view.setTypeface(view.typeface, if (isRead) Typeface.NORMAL else Typeface.BOLD)
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
    @BindingAdapter("android:drawableStart")
    fun setDrawableStart(view: TextView, @DrawableRes resId: Int) {
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(resId, 0, 0, 0)
    }

    @JvmStatic
    @BindingAdapter("android:src")
    fun setImageSrc(view: ImageView, @DrawableRes resId: Int) {
        if (resId != 0) {
            view.setImageResource(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("android:tint")
    fun setImageTint(view: ImageView, @ColorRes resId: Int) {
        if (resId != 0) {
            val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.resources.getColor(resId, view.context.theme)
            } else {
                view.resources.getColor(resId)
            }
            view.setColorFilter(color)
        }
    }

    @JvmStatic
    @BindingAdapter("android:background")
    fun setBackgroundSrc(view: View, @DrawableRes resId: Int) {
        if (resId != 0) {
            view.setBackgroundResource(resId)
        }
    }

    @JvmStatic
    @BindingAdapter("app:percent")
    fun setPercent(view: GaugeView, percent: Double) {
        view.setPercent(percent)
    }

    /**
     * Returns TODAY or TOMORROW if applicable.  Otherwise returns NONE.
     */
    private fun isToday(
        dayMillis: Long,
        currentMillis: Long,
        localGmtOffset: Long
    ): Boolean {
        val startDay: Int = Time.getJulianDay(dayMillis, localGmtOffset)
        val currentDay: Int = Time.getJulianDay(currentMillis, localGmtOffset)
        val days = startDay - currentDay
        return days == 0
    }
}