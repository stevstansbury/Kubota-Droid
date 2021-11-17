package com.android.kubota.ui.notification

import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.BR
import com.android.kubota.R
import com.android.kubota.databinding.ViewNotificationItemBinding
import com.kubota.service.domain.InboxMessage
import kotlinx.android.synthetic.main.view_notification_item.view.*

class NotificationAdapter(
    private val data: MutableList<InboxMessage>,
    private val onClickListener: ((notification: InboxMessage) -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

        holder.itemView.body.text = data[position].body.parseNotifications { _, placeholder ->
            inSpans(object : ForegroundColorSpan(R.color.equipment_tree_filters_close) {}) {
                append(placeholder)
            }

        }
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

    private fun parseNotification(notification: String): SpannedString {
        var text = notification
        return buildSpannedString {
            while (text.contains("](http")) {
                val startOfPlaceholder = text.indexOf("[")
                val endOfPlaceholder = text.indexOf("]")

                val placeholder = text.substring(startOfPlaceholder + 1, endOfPlaceholder)

                val endOfLink = text.indexOf(")")
                append(text.substring(0, startOfPlaceholder))

                inSpans(object : ForegroundColorSpan(R.color.equipment_tree_filters_close) {}) {
                    append(placeholder)
                }

                text = text.substring(endOfLink + 1, text.length)
            }

            append(text.substring(0, text.length))
        }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}
