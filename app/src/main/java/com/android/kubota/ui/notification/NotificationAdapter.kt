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
import com.kubota.service.domain.InboxMessage

class NotificationAdapter(
    private val data: MutableList<InboxMessage>,
    private val onClickListener: ((notification: InboxMessage) -> Unit)
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

    fun getItem(position: Int) = data[position]

    fun replace(inboxMessageList: List<InboxMessage>) {
        removeAll()
        addAll(inboxMessageList)
    }

    private fun addAll(inboxMessageList: List<InboxMessage>) {
        data.addAll(inboxMessageList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun removeAll() {
        data.clear()
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}