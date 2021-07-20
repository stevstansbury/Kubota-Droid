package com.android.kubota.ui.equipment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.*
import com.android.kubota.ui.*
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.showMessage
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.*
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator
import java.net.URL
import kotlin.math.roundToInt

class EquipmentDetailFragment : BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var machineCard: MachineCardView
    private lateinit var manualsButton: TextView
    private lateinit var guidesButton: TextView
    private lateinit var faultCodeButton: TextView
    private lateinit var inhibitRestartButton: TextView
    private lateinit var telematicsButton: TextView
    private lateinit var geofenceButton: TextView
    private lateinit var maintenanceScheduleButton: TextView
    private lateinit var warrantyInfoButton: TextView
    private lateinit var instructionalVideoButton: TextView
    private lateinit var kubotaNowHeader: TextView
    private lateinit var containerAttachmentSlider: View
    private lateinit var attachmentSlider: RecyclerView
    private lateinit var btnMinimizeSlider: TextView
    private lateinit var circleIndicator: ScrollingPagerIndicator

    private var shouldReload = false

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): EquipmentDetailFragment {
            return EquipmentDetailFragment().apply {
                arguments = getBundle(equipmentUnit)
            }
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (shouldReload) {
            viewModel.reload(authDelegate)
        } else {
            shouldReload = true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            viewModel.equipmentUnit.value?.displayName?.let { activity?.title = it }
        }
    }

    override fun initUi(view: View) {
        machineCard = view.findViewById(R.id.machineCardView)
        manualsButton = view.findViewById(R.id.manualsButton)
        guidesButton = view.findViewById(R.id.guidesButton)
        telematicsButton = view.findViewById(R.id.telematicsButton)
        geofenceButton = view.findViewById(R.id.geofenceButton)
        faultCodeButton = view.findViewById(R.id.faultCodeButton)
        faultCodeButton.setOnClickListener {
            this.equipmentUnit?.let {
                flowActivity?.addFragmentToBackStack(
                    FaultCodeFragment.createInstance(it)
                )
            }
        }
        maintenanceScheduleButton = view.findViewById(R.id.maintenanceSchedulesButton)
        inhibitRestartButton = view.findViewById(R.id.inhibitRestartButton)
        warrantyInfoButton = view.findViewById(R.id.warrantyInfoButton)
        instructionalVideoButton = view.findViewById(R.id.instructionalVideoButton)
        kubotaNowHeader = view.findViewById(R.id.kubota_new_header)
        containerAttachmentSlider = view.findViewById(R.id.container_attachments_slider)
        attachmentSlider = view.findViewById(R.id.recycler)
        btnMinimizeSlider = view.findViewById(R.id.tv_minimize)
        circleIndicator = view.findViewById(R.id.circle_indicator)

        machineCard.enterDetailMode()
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        this.hideProgressBar()

        this.viewModel.isLoading.observe(viewLifecycleOwner, { loading ->
            when (loading) {
                true -> this.showBlockingActivityIndicator()
                else -> this.hideBlockingActivityIndicator()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, { error ->
            error?.let { this.showError(it) }
        })

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, { unit ->
            unit?.let {
                onBindData(it)
                machineCard.setModel(it)
            }
        })

        this.viewModel.compatibleAttachments.observe(viewLifecycleOwner, { list ->
            if (list.isNotEmpty()) {
                displayCompatibleAttachments(list)
            }
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        activity?.title = display.nickname

        machineCard.setOnLocationViewClicked(object : MachineCardView.OnLocationViewClicked {
            override fun onClick() {
                geofenceButton.callOnClick()
            }
        })

        val geofenceDrawable = if (unit.telematics?.outsideGeofence == true) {
            requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
        } else {
            requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        }

        geofenceButton.setCompoundDrawablesWithIntrinsicBounds(null, null, geofenceDrawable, null)
        geofenceButton.visibility = if (unit.hasTelematics) View.VISIBLE else View.GONE
        geofenceButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(GeofenceFragment.createInstance(unit.telematics?.location))
        }

        machineCard.setOnEditViewClicked(object : MachineCardView.OnEditViewClicked {
            override fun onClick() {
                flowActivity?.addFragmentToBackStack(EditEquipmentFragment.createInstance(unit))
            }
        })

        telematicsButton.visibility = if (unit.hasTelematics) View.VISIBLE else View.GONE
        telematicsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(TelematicsFragment.createInstance(unit.id))
        }

        inhibitRestartButton.visibility = if (unit.canModifyRestart()) View.VISIBLE else View.GONE
        inhibitRestartButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(InhibitStarterFragment.createInstance(unit))
        }

        var telematicsStatus = requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        unit.telematics?.let {
            val status = TelematicStatus.Critical
            if (it.voltageStatus == status || it.defRemainingStatus == status || it.fuelRemainingStatus == status || it.hydraulicTempStatus == status || it.coolantTempStatus == status) {
                telematicsStatus = requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
            }
        }

        telematicsButton.setCompoundDrawablesWithIntrinsicBounds(null, null, telematicsStatus, null)

        kubotaNowHeader.isVisible = unit.hasTelematics || unit.canModifyRestart()

        faultCodeButton.visibility = if (unit.hasFaultCodes) View.VISIBLE else View.GONE

        val faultCodeDrawable = if (unit.telematics?.faultCodes?.isNotEmpty() == true) {
            requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
        } else {
            requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        }

        faultCodeButton.setCompoundDrawablesWithIntrinsicBounds(null, null, faultCodeDrawable, null)

        manualsButton.visibility = if (unit.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            when (unit.manualInfo.count() == 1) {
                true -> this.flowActivity?.let {
                    ManualsListFragment.pushManualToStack(it, unit.manualInfo.first())
                }
                false -> this.flowActivity?.addFragmentToBackStack(
                    ManualsListFragment.createInstance(
                        modelName = unit.model,
                        manualInfo = unit.manualInfo
                    )
                )
            }
        }

        guidesButton.visibility = if (unit.guideUrl != null) View.VISIBLE else View.GONE
        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(
                    GuidesListFragment.createInstance(unit.model)
                )
            } else {
                val fragment = DisclaimerFragment.createInstance(
                    DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED
                )
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        activity?.popCurrentTabStack()
                        flowActivity?.addFragmentToBackStack(
                            GuidesListFragment.createInstance(unit.model)
                        )
                    }

                    override fun onDisclaimerDeclined() {
                        activity?.popCurrentTabStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        maintenanceScheduleButton.visibility =
            if (unit.hasMaintenanceSchedules) View.VISIBLE else View.GONE
        maintenanceScheduleButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(MaintenanceIntervalFragment.createInstance(unit.model))
        }

        warrantyInfoButton.visibility = if (unit.warrantyUrl != null) View.VISIBLE else View.GONE
        unit.warrantyUrl?.let { warrantyUrl ->
            warrantyInfoButton.setOnClickListener {
                showMessage(
                    titleId = R.string.leave_app_dialog_title,
                    messageId = R.string.leave_app_kubota_usa_website_msg
                )
                    .map { idx ->
                        if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                        val url = warrantyUrl.toString()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
            }
        }

        instructionalVideoButton.visibility =
            if (unit.hasInstrucationalVideo) View.VISIBLE else View.GONE
        instructionalVideoButton.setOnClickListener {
            this.flowActivity?.addFragmentToBackStack(
                VideoListFragment.createInstance(
                    modelName = unit.model,
                    videoInfo = unit.instructionalVideos
                )
            )
        }
    }

    private fun getTopCategoryAttachmentsSize(subcategories: Map<EquipmentCategory, List<Any>>): Int {
        val equipmentSizes = subcategories.map {
            if (it.key.hasSubCategories) {
                it.value.sumOf { child ->
                    getTopCategoryAttachmentsSize(child as Map<EquipmentCategory, List<Any>>)
                }
            } else {
                it.value.size
            }
        }
        return equipmentSizes.sum()
    }

    private fun displayCompatibleAttachments(categories: List<Map<EquipmentCategory, List<Any>>>) {
        val attachments =
            categories.first().values.first() as List<Map<EquipmentCategory, List<Any>>>

        val adapter = AttachmentListAdapter(
            list = listOf(
                AttachmentCategoryItemState(
                    categoryName = "",
                    categorySize = 0,
                    categoryIcon = null
                ) //static See All Item
            ) + attachments.map { category ->

                AttachmentCategoryItemState(
                    categoryName = category.keys.first().category,
                    categorySize = getTopCategoryAttachmentsSize(category),
                    categoryIcon = category.keys.first().imageResources?.fullUrl
                        ?: category.keys.first().imageResources?.iconUrl
                )
            },
            attachmentAdapterDelegate = object : AttachmentAdapterDelegate {
                override fun onItemClicked(attachmentItem: AttachmentCategoryItemState) {
                    Toast.makeText(requireContext(), "item", Toast.LENGTH_SHORT).show()
                }

                override fun onSeeAllItemClicked(attachmentItem: AttachmentCategoryItemState) {
                    Toast.makeText(requireContext(), "see all", Toast.LENGTH_SHORT).show()
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

        containerAttachmentSlider.isVisible = true

        attachmentSlider.adapter = adapter
        circleIndicator.attachToRecyclerView(attachmentSlider, requireContext().dpToPx(16))

        btnMinimizeSlider.setOnClickListener {
            adapter.minimize()
        }
    }
}

private fun EquipmentUnit.canModifyRestart() = telematics?.restartInhibitStatus?.canModify == true

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

private class AttachmentItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView), AttachmentItemViewBehaviour {

    var item: AttachmentCategoryItemState? = null

    val ivIcon: ImageView = itemView.findViewById(R.id.iv_attachment)
    val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
    val tvViewAll: TextView = itemView.findViewById(R.id.tv_view_all)

    val container: CardView = itemView.findViewById(R.id.container_attachment)

    val minItemSizeDp = 48
    val maxItemSizeDp = 155

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

private class AttachmentSeeAllItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    AttachmentItemViewBehaviour {

    val container: CardView = itemView.findViewById(R.id.container_see_all)
    val tvSeeAll: TextView = itemView.findViewById(R.id.tv_see_all)

    val minItemSizeDp = 48
    val maxItemSizeDp = 200

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

private class AttachmentListAdapter(
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
