package com.android.kubota.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.kubota.R
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ChooseEquipmentViewModel
import com.android.kubota.viewmodel.UIModel
import java.util.*

class ChooseEquipmentFragment : BaseFragment() {
    private lateinit var viewModel: ChooseEquipmentViewModel
    private lateinit var expandableListView: ExpandableListView
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideChooseEquipmentViewModel()
        viewModel = ViewModelProviders.of(this, factory).get(ChooseEquipmentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.my_equipment_list_title)

        val view = inflater.inflate(R.layout.fragment_choose_equipment, null)
        expandableListView = view.findViewById(R.id.expandableListView)
        nextButton = view.findViewById(R.id.nextButton)

        viewModel.isLoading.observe(this, Observer { loading ->
            if (loading == true) {
                flowActivity?.showProgressBar()
                nextButton.visibility = View.INVISIBLE
            } else {
                flowActivity?.hideProgressBar()
                nextButton.visibility = View.VISIBLE
            }
        })

        viewModel.serverError.observe(this, Observer { error ->
            if (error == true) {
                flowActivity?.hideProgressBar()
                Snackbar.make(view, getString(R.string.server_error_message), Snackbar.LENGTH_LONG).show()
            }
        })

        viewModel.categories.observe(this, Observer { categories ->
            categories?.let {
                expandableListView.setAdapter(CategoryListAdapter(requireContext(), it, ::onItemSelected))
            }
        })

        return view
    }

    private fun onItemSelected(categoryResId: Int, imageResId: Int, model: String) {
        nextButton.isEnabled = true
        nextButton.setOnClickListener {
            val fragment = AddEquipmentFragment.createInstance(UIModel(
                id = 1,
                modelName = model,
                serialNumber = null,
                categoryResId = categoryResId,
                imageResId = imageResId,
                hasManual = false,
                hasMaintenanceGuides = false
            ))
            flowActivity?.addFragmentToBackStack(fragment)
        }
    }
}

private class CategoryListAdapter(
    context: Context,
    private val map: Map<String, List<String>>,
    // Todo: return model object
    private val onItemSelected: (categoryResId: Int, imageResId: Int, model: String) -> Unit)
    : BaseExpandableListAdapter() {

    private var selectedGroupIdx = -1
    private var selectedChildIdx = -1
    private val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getGroup(groupPosition: Int): String = map.keys.elementAt(groupPosition)

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun hasStableIds(): Boolean = true

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val groupView = convertView ?: layoutInflater.inflate(R.layout.view_category_list_item, null)

        groupView.findViewById<TextView>(R.id.categoryTextView).text = getGroup(groupPosition)
        groupView.findViewById<ImageView>(R.id.chevronImageView).setImageResource(if (isExpanded) R.drawable.ic_chevron_up_24dp else R.drawable.ic_chevron_down_24dp)

        return groupView
    }

    override fun getChildrenCount(groupPosition: Int): Int = map[getGroup(groupPosition)]?.size ?: 0

    override fun getChild(groupPosition: Int, childPosition: Int): String = map[getGroup(groupPosition)]!![childPosition]

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
        val resId = when(group) {
            "Construction" -> R.drawable.ic_construction_category_thumbnail
            "Mowers" -> R.drawable.ic_mower_category_thumbnail
            "Tractors" -> R.drawable.ic_tractor_category_thumbnail
            else -> R.drawable.ic_utv_category_thumbnail
        }
        modelView.findViewById<ImageView>(R.id.imageView).apply {
            // TODO: Use image from server side
            setImageResource(resId)
        }

        val categoryResId = when(group) {
            "Construction" -> R.string.equipment_construction_category
            "Mowers" -> R.string.equipment_mowers_category
            "Tractors" -> R.string.equipment_tractors_category
            else -> R.string.equipment_utv_category
        }
        val child = getChild(groupPosition, childPosition)
        radioButton.text = child
        radioButton.isChecked = isSelectedChildView(groupPosition, childPosition)
        radioButton.setOnClickListener {
            if (!isSelectedChildView(groupPosition, childPosition)) {
                selectedGroupIdx = groupPosition
                selectedChildIdx = childPosition
                onItemSelected(categoryResId, resId, child)
                notifyDataSetChanged()
            }
        }

        modelView.setOnClickListener { radioButton.callOnClick() }

        return modelView
    }

    private fun isSelectedChildView(groupPosition: Int, childPosition: Int) =
        selectedGroupIdx == groupPosition && selectedChildIdx == childPosition

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = Objects.hashCode(map[getGroup(groupPosition)]!![childPosition]).toLong()

    override fun getGroupCount(): Int = map.keys.size
}
