package com.android.kubota.ui.equipment.filter

import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import com.android.kubota.R
import com.android.kubota.ui.BaseFragment
import com.android.kubota.viewmodel.equipment.EquipmentTreeFilter
import com.google.android.flexbox.FlexboxLayout

class EquipmentFiltersFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_equipment_filters

    private lateinit var btnClose: TextView
    private lateinit var containerFilters: FlexboxLayout

    var delegate: BottomSheetDelegate? = null

    companion object {
        fun instance(filters: List<EquipmentTreeFilter>?): EquipmentFiltersFragment {
            return EquipmentFiltersFragment().apply {
                arguments = bundleOf(EquipmentTreeFilterFragment.SELECTED_FILTERS to filters)
            }
        }
    }

    override fun initUi(view: View) {
        btnClose = view.findViewById(R.id.btn_close)
        containerFilters = view.findViewById(R.id.container_filters)

        btnClose.setOnClickListener {
            delegate?.closeBottomSheet()
        }
    }

    override fun loadData() {
        val items = mutableListOf<EquipmentTreeFilter>()

        val activeFilters = arguments
            ?.getParcelableArrayList<EquipmentTreeFilter>(EquipmentTreeFilterFragment.SELECTED_FILTERS)
            ?: emptyList()

        if (activeFilters.none { it is EquipmentTreeFilter.Discontinued }) {
            items.add(EquipmentTreeFilter.Discontinued)
        }

        containerFilters.removeAllViews()
        items.forEach {
            addFilterView(it)
        }
    }

    private fun addFilterView(filter: EquipmentTreeFilter) {
        val item = layoutInflater.inflate(
            R.layout.view_equipment_tree_filter,
            containerFilters,
            false
        )

        if (filter is EquipmentTreeFilter.Discontinued) {
            item.findViewById<TextView>(R.id.tv_filter_title).text =
                getString(R.string.equipment_tree_filter_discontinued)
        }


        item.setOnClickListener {
            delegate?.onFilterClicked(filter)
            delegate?.closeBottomSheet()
        }

        containerFilters.addView(item)
    }
}