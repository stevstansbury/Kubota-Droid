package com.android.kubota.ui

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import com.android.kubota.R
import com.android.kubota.extensions.showServerErrorSnackBar
import com.android.kubota.ui.SearchActivity.Companion.ADD_EQUIPMENT_MODE
import com.android.kubota.ui.SearchActivity.Companion.KEY_MODE
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.utility.Utils
import com.android.kubota.viewmodel.ChooseEquipmentViewModel
import com.android.kubota.viewmodel.EquipmentUIModel
import java.util.*

class ChooseEquipmentFragment : BaseFragment() {
    companion object {
        const val KEY_SEARCH_RESULT = "SEARCH_RESULT"
        private const val SEARCH_REQUEST_CODE = 100
    }

    private lateinit var viewModel: ChooseEquipmentViewModel
    private lateinit var nextButton: Button
    private lateinit var expandableListView: ExpandableListView
    private var lastExpandedGroupPosition = -1

    private var selectedEquipmentModel: EquipmentUIModel? = null
        set(value) {
            field = value
            nextButton.isEnabled = (value != null)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideChooseEquipmentViewModel()
        viewModel = ViewModelProviders.of(this, factory).get(ChooseEquipmentViewModel::class.java)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.add_equipment)

        val view = inflater.inflate(R.layout.fragment_choose_equipment, null)

        nextButton = view.findViewById<Button>(R.id.nextButton).apply {
            setOnClickListener {
                selectedEquipmentModel?.let { model ->
                    flowActivity?.addFragmentToBackStack(AddEquipmentFragment.createInstance(model))
                }
            }
        }

        expandableListView = view.findViewById<ExpandableListView>(R.id.expandableListView).apply {
            setOnGroupClickListener { _, _, groupPosition, _ ->
                smoothScrollToPositionFromTop(groupPosition,0)
                if (lastExpandedGroupPosition != -1 && lastExpandedGroupPosition != groupPosition) {
                    collapseGroup(lastExpandedGroupPosition)
                }

                if (isGroupExpanded(groupPosition)) {
                    collapseGroup(groupPosition)
                }
                else {
                    lastExpandedGroupPosition = groupPosition
                    expandGroup(groupPosition)
                }
                true
            }
        }

        viewModel.isLoading.observe(this, Observer { loading ->
            if (loading == true) {
                flowActivity?.showProgressBar()
                nextButton.visibility = View.INVISIBLE
            } else {
                flowActivity?.hideProgressBar()
            }
        })

        viewModel.serverError.observe(this, Observer { error ->
            if (error == true) {
                flowActivity?.hideProgressBar()
                flowActivity?.showServerErrorSnackBar()
            }
        })

        viewModel.categories.observe(this, Observer { categories ->
            categories?.let {
                nextButton.visibility = View.VISIBLE
                expandableListView.setAdapter(CategoryListAdapter(requireContext(), categories) {
                    selectedEquipmentModel = it
                })
            }
        })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuItem = menu.findItem(R.id.search)

        menuItem?.setOnMenuItemClickListener {
            val intent = Intent(this.activity, SearchActivity::class.java)
                .putExtra(KEY_MODE, ADD_EQUIPMENT_MODE)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

            startActivityForResult(intent, SEARCH_REQUEST_CODE)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getParcelableExtra<EquipmentUIModel>(KEY_SEARCH_RESULT)?.let {
                selectedEquipmentModel = it
                flowActivity?.addFragmentToBackStack(AddEquipmentFragment.createInstance(it))
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

private class CategoryListAdapter(
    context: Context,
    private val map: Map<String, List<String>>,
    private val onItemSelected: (model: EquipmentUIModel) -> Unit
) : BaseExpandableListAdapter() {

    private var selectedGroupIdx = -1
    private var selectedChildIdx = -1
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getGroup(groupPosition: Int): String = map.keys.elementAt(groupPosition)

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun hasStableIds(): Boolean = true

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val groupView = convertView ?: layoutInflater.inflate(R.layout.view_category_list_item, null)

        groupView.findViewById<TextView>(R.id.categoryTextView).text = getGroup(groupPosition)
        groupView.findViewById<ImageView>(R.id.chevronImageView)
            .setImageResource(if (isExpanded) R.drawable.ic_chevron_up_24dp else R.drawable.ic_chevron_down_24dp)

        return groupView
    }

    override fun getChildrenCount(groupPosition: Int): Int = map[getGroup(groupPosition)]?.size ?: 0

    override fun getChild(groupPosition: Int, childPosition: Int): String =
        map.getValue(getGroup(groupPosition))[childPosition]

    override fun getGroupId(groupPosition: Int): Long = Objects.hashCode(getGroup(groupPosition)).toLong()

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val modelView = convertView ?: layoutInflater.inflate(R.layout.select_equipment_view, null)

        val radioButton = modelView.findViewById<RadioButton>(R.id.radioButton)
        val group = getGroup(groupPosition)
        val child = getChild(groupPosition, childPosition)
        val resId = when (group) {
            "Construction" -> Utils.getModelImage(group, child)
            "Mowers" -> R.drawable.ic_mower_category_thumbnail
            "Tractors" -> R.drawable.ic_tractor_category_thumbnail
            else -> R.drawable.ic_utv_category_thumbnail
        }
        modelView.findViewById<ImageView>(R.id.imageView).apply {
            // TODO: Use image from server side
            setImageResource(resId)
        }

        val categoryResId = when (group) {
            "Construction" -> R.string.equipment_construction_category
            "Mowers" -> R.string.equipment_mowers_category
            "Tractors" -> R.string.equipment_tractors_category
            else -> R.string.equipment_utv_category
        }
        radioButton.text = child
        radioButton.isChecked = isSelectedChildView(groupPosition, childPosition)
        radioButton.setOnClickListener {
            if (!isSelectedChildView(groupPosition, childPosition)) {
                selectedGroupIdx = groupPosition
                selectedChildIdx = childPosition
                onItemSelected(
                    EquipmentUIModel(
                        id = 1,
                        name = child,
                        categoryResId = categoryResId,
                        imageResId = resId
                    )
                )
                notifyDataSetChanged()
            }
        }

        modelView.setOnClickListener { radioButton.callOnClick() }

        return modelView
    }

    private fun isSelectedChildView(groupPosition: Int, childPosition: Int) =
        selectedGroupIdx == groupPosition && selectedChildIdx == childPosition

    override fun getChildId(groupPosition: Int, childPosition: Int): Long =
        Objects.hashCode(map[getGroup(groupPosition)]!![childPosition]).toLong()

    override fun getGroupCount(): Int = map.keys.size
}
