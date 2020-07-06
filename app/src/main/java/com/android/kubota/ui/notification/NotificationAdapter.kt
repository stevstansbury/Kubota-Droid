package com.android.kubota.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.R
import com.android.kubota.databinding.ViewNotificationItemBinding
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.Notification

class NotificationAdapter(
    private val data: List<Notification>,
    private val onClickListener: ((notification: Notification) -> Unit)
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ViewNotificationItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.view_notification_item,
            parent,
            false
        )

        binding.root.tag = binding
        return BindingHolder(binding.root)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding: ViewDataBinding =
            holder.itemView.tag as ViewNotificationItemBinding
        binding.setVariable(BR.notification, data[position])
        holder.itemView.setOnClickListener {
            onClickListener.invoke(data[position])
        }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}