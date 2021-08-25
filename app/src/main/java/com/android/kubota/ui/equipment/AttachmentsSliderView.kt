package com.android.kubota.ui.equipment

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.dpToPx
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.EquipmentModelTree
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator
import java.net.URL
import kotlin.math.roundToInt

class AttachmentsSliderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var attachmentSlider: RecyclerView
    private lateinit var btnMinimizeSlider: TextView
    private lateinit var circleIndicator: ScrollingPagerIndicator

    private var sliderOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val position =
                (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

            if (position != -1) {
                (recyclerView.adapter as? AttachmentListAdapter)?.updateSelectedItem(position)
            }
        }
    }

    private var attachmentClickListener: OnAttachmentClicked? = null

    override fun onFinishInflate() {
        super.onFinishInflate()

        LayoutInflater.from(context).inflate(R.layout.view_attachments_slider, this)

        attachmentSlider = findViewById(R.id.recycler)
        btnMinimizeSlider = findViewById(R.id.tv_minimize)
        circleIndicator = findViewById(R.id.circle_indicator)
    }

    fun displayCompatibleAttachments(categories: List<EquipmentModelTree>) {
        val attachments = categories
            .first()
            .let { it as EquipmentModelTree.Category }
            .items
            .mapNotNull { it as? EquipmentModelTree.Category }

        val adapter = AttachmentListAdapter(
            list = listOf(
                AttachmentCategoryItemState(
                    categoryName = "",
                    categorySize = 0,
                    categoryIcon = null
                ) //static See All Item
            ) + attachments.map { categoryWrapper ->
                AttachmentCategoryItemState(
                    categoryName = categoryWrapper.category.category,
                    categorySize = categoryWrapper.getTopCategoryAttachmentsSize(),
                    categoryIcon = categoryWrapper.category.imageResources?.fullUrl
                        ?: categoryWrapper.category.imageResources?.iconUrl
                )
            },
            attachmentAdapterDelegate = object : AttachmentAdapterDelegate {
                override fun onItemClicked(attachmentItem: AttachmentCategoryItemState) {
                    attachmentClickListener?.onItemClicked(attachmentItem)
                }

                override fun onSeeAllItemClicked(attachmentItem: AttachmentCategoryItemState) {
                    attachmentClickListener?.onSeeAllItemClicked(attachmentItem)
                }

                override fun onStateChanged(expanded: Boolean) {
                    btnMinimizeSlider.isVisible = expanded
                    circleIndicator.isVisible = expanded

                    if (expanded) {
                        attachmentSlider.addOnScrollListener(sliderOnScrollListener)
                    } else {
                        attachmentSlider.removeOnScrollListener(sliderOnScrollListener)
                    }
                }

                override fun onSelectedItemExpanding(position: Int) {
                    if (position != -1) {
                        attachmentSlider.scrollToPosition(position)
                    }
                }
            }
        )

        attachmentSlider.adapter = adapter
        circleIndicator.attachToRecyclerView(attachmentSlider, context.dpToPx(16))

        btnMinimizeSlider.setOnClickListener {
            adapter.minimize()
        }
    }

    fun setOnAttachmentClickedListener(listener: OnAttachmentClicked) {
        attachmentClickListener = listener
    }

    interface OnAttachmentClicked {
        fun onItemClicked(attachmentItem: AttachmentCategoryItemState)
        fun onSeeAllItemClicked(attachmentItem: AttachmentCategoryItemState)
    }

    data class AttachmentCategoryItemState(
        val categoryName: String,
        val categorySize: Int,
        val categoryIcon: URL?
    ) {
        var selected = false
    }

    interface AttachmentAdapterDelegate {
        fun onItemClicked(attachmentItem: AttachmentCategoryItemState)
        fun onSeeAllItemClicked(attachmentItem: AttachmentCategoryItemState)
        fun onStateChanged(expanded: Boolean)
        fun onSelectedItemExpanding(position: Int)
    }

    interface AttachmentItemViewBehaviour {
        fun updateItemSize(progress: Int, onItemExpanding: (itemPosition: Int) -> Unit)

        fun showItemLabels(itemSelected: Boolean)
        fun hideItemLabels()
        fun selectItem(itemSelected: Boolean)
    }

    inner class AttachmentItemViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), AttachmentItemViewBehaviour {

        var item: AttachmentCategoryItemState? = null

        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_attachment)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvViewAll: TextView = itemView.findViewById(R.id.tv_view_all)

        val container: CardView = itemView.findViewById(R.id.container_attachment)

        private val minItemSizeDp = 48
        private val maxItemSizeDp = 155

        fun bind(
            attachmentItem: AttachmentCategoryItemState,
            expanded: Boolean,
            onItemClicked: (attachmentItem: AttachmentCategoryItemState) -> Unit
        ) {
            item = attachmentItem

            tvTitle.isVisible = expanded
            tvViewAll.isVisible = expanded

            tvTitle.text = attachmentItem.categoryName
            tvViewAll.text = tvViewAll.context.getString(
                R.string.attachment_slider_item_view_all,
                attachmentItem.categorySize
            )

            ivIcon.setImageResource(R.drawable.ic_construction_category_thumbnail)

            if (attachmentItem.categoryIcon != null) {
                AppProxy.proxy.serviceManager.contentService
                    .getBitmap(url = attachmentItem.categoryIcon)
                    .done { bitmap ->
                        when (bitmap) {
                            null -> ivIcon.setImageResource(R.drawable.ic_construction_category_thumbnail)
                            else -> ivIcon.setImageBitmap(bitmap)
                        }
                    }
            }

            ivIcon.updateWidthAndHeight(if (expanded) maxItemSizeDp else minItemSizeDp)

            val cardBackgroundColor = if (expanded) {
                if (attachmentItem.selected) {
                    itemView.context.getColor(R.color.attachments_list_item_selected)
                } else {
                    itemView.context.getColor(R.color.attachments_list_item_unselected)
                }
            } else {
                Color.WHITE
            }

            container.setCardBackgroundColor(cardBackgroundColor)

            itemView.setOnClickListener {
                onItemClicked(attachmentItem)
            }
        }

        override fun updateItemSize(progress: Int, onItemExpanding: (itemPosition: Int) -> Unit) {
            val size = minItemSizeDp + progress / 100F * (maxItemSizeDp - minItemSizeDp)
            ivIcon.updateWidthAndHeight(size.roundToInt())

            onItemExpanding(adapterPosition)
        }

        override fun showItemLabels(itemSelected: Boolean) {
            tvTitle.isVisible = true
            tvViewAll.isVisible = true

            if (itemSelected) {
                container.setCardBackgroundColor(itemView.context.getColor(R.color.attachments_list_item_selected))
            } else {
                container.setCardBackgroundColor(itemView.context.getColor(R.color.attachments_list_item_unselected))
            }
        }

        override fun hideItemLabels() {
            tvTitle.isVisible = false
            tvViewAll.isVisible = false

            container.setCardBackgroundColor(Color.WHITE)
        }

        override fun selectItem(itemSelected: Boolean) {
            if (itemSelected) {
                container.setCardBackgroundColor(itemView.context.getColor(R.color.attachments_list_item_selected))
            } else {
                container.setCardBackgroundColor(itemView.context.getColor(R.color.attachments_list_item_unselected))
            }
        }
    }

    inner class AttachmentSeeAllItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        AttachmentItemViewBehaviour {

        val container: CardView = itemView.findViewById(R.id.container_see_all)
        private val tvSeeAll: TextView = itemView.findViewById(R.id.tv_see_all)

        private val minItemSizeDp = 48
        private val maxItemSizeDp = 200

        val minFontSize = 12F
        val maxFontSize = 21F

        fun bind(
            attachmentItem: AttachmentCategoryItemState,
            expanded: Boolean,
            onItemClicked: (attachmentItem: AttachmentCategoryItemState) -> Unit
        ) {
            tvSeeAll.updateHeight(if (expanded) maxItemSizeDp else minItemSizeDp)
            tvSeeAll.textSize = if (expanded) maxFontSize else minFontSize

            itemView.setOnClickListener {
                onItemClicked(attachmentItem)
            }
        }

        override fun updateItemSize(progress: Int, onItemExpanding: (itemPosition: Int) -> Unit) {
            val containerSize = minItemSizeDp + progress / 100F * (maxItemSizeDp - minItemSizeDp)

            tvSeeAll.updateHeight(containerSize.roundToInt())
            container.updateHeight(containerSize.roundToInt())

            val textSize = minFontSize + progress / 100F * (maxFontSize - minFontSize)
            tvSeeAll.textSize = textSize.roundToInt().toFloat()
        }

        override fun showItemLabels(itemSelected: Boolean) = Unit

        override fun hideItemLabels() = Unit

        override fun selectItem(itemSelected: Boolean) = Unit
    }

    inner class AttachmentListAdapter(
        private val list: List<AttachmentCategoryItemState>,
        private val attachmentAdapterDelegate: AttachmentAdapterDelegate
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var listExpanded = false
        private val attachmentBehaviourListeners: MutableSet<AttachmentItemViewBehaviour> =
            mutableSetOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_equipment_attachment_see_all_item, parent, false)

                val viewHolder = AttachmentSeeAllItemViewHolder(view)
                attachmentBehaviourListeners.add(viewHolder)

                viewHolder
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_equipment_attachment_item, parent, false)

                val viewHolder = AttachmentItemViewHolder(view)
                attachmentBehaviourListeners.add(viewHolder)

                viewHolder
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AttachmentItemViewHolder) {
                holder.bind(list[position], listExpanded) {
                    if (listExpanded) {
                        attachmentAdapterDelegate.onItemClicked(list[position])
                    } else {
                        listExpanded = true

                        val animator = ValueAnimator.ofInt(0, 100)

                        animator.addUpdateListener {
                            val progress = it.animatedValue as Int
                            expandItems(progress, position)
                        }

                        animator.doOnEnd {
                            showItemLabels(position)
                            attachmentAdapterDelegate.onStateChanged(listExpanded)
                        }

                        animator.start()
                    }
                }
            } else {
                (holder as AttachmentSeeAllItemViewHolder).bind(
                    list[position],
                    listExpanded
                ) {
                    attachmentAdapterDelegate.onSeeAllItemClicked(list[position])
                }
            }
        }

        override fun getItemCount() = list.size

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) 0 else 1
        }

        fun minimize() {
            listExpanded = false

            val animator = ValueAnimator.ofInt(100, 0)

            animator.addUpdateListener {
                val progress = it.animatedValue as Int

                attachmentBehaviourListeners.forEach { listener ->
                    listener.updateItemSize(progress) {}
                }
            }

            animator.doOnStart {
                attachmentBehaviourListeners.forEach { listener ->
                    listener.hideItemLabels()
                }
            }

            animator.doOnEnd {
                attachmentAdapterDelegate.onStateChanged(listExpanded)
            }

            animator.start()
        }

        fun updateSelectedItem(position: Int) {
            if (!listExpanded) return

            list.forEachIndexed { index, item ->
                item.selected = index == position
            }

            attachmentBehaviourListeners.forEach {
                if (it is AttachmentItemViewHolder) {
                    it.selectItem(it.item == list[position])
                } else {
                    (it as AttachmentSeeAllItemViewHolder).selectItem(false)
                }
            }
        }

        private fun expandItems(progress: Int, selectedItemPosition: Int) {
            attachmentBehaviourListeners.forEach { listener ->
                listener.updateItemSize(progress) { itemPosition ->
                    if (listener is AttachmentItemViewHolder) {
                        if (listener.item == list[selectedItemPosition]) {
                            attachmentAdapterDelegate.onSelectedItemExpanding(
                                itemPosition
                            )
                        }
                    }
                }
            }
        }

        private fun showItemLabels(selectedItemPosition: Int) {
            attachmentBehaviourListeners.forEach { listener ->
                if (listener is AttachmentItemViewHolder) {
                    listener.showItemLabels(listener.item == list[selectedItemPosition])
                } else {
                    (listener as AttachmentSeeAllItemViewHolder).showItemLabels(false)
                }
            }
        }
    }
}

fun View.updateWidthAndHeight(sizeDp: Int) {
    val layoutParams = this.layoutParams
    layoutParams.width = this.context.dpToPx(sizeDp)
    layoutParams.height = this.context.dpToPx(sizeDp)
    this.layoutParams = layoutParams
}

fun View.updateHeight(sizeDp: Int) {
    val layoutParams = this.layoutParams
    layoutParams.height = this.context.dpToPx(sizeDp)
    this.layoutParams = layoutParams
}

fun EquipmentModelTree.getTopCategoryAttachmentsSize(): Int {
    return when (this) {
        is EquipmentModelTree.Model -> 1
        is EquipmentModelTree.Category -> {
            this.items
                .map { it.getTopCategoryAttachmentsSize() }
                .fold(0) { acc, next -> acc + next }
        }
    }
}

