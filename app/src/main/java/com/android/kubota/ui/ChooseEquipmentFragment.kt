package com.android.kubota.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.ChooseEquipmentViewModel

class ChooseEquipmentFragment : AddEquipmentControlledFragment() {

    companion object {
        private const val CATEGORY_KEY = "EquipmentCategory"

        fun createInstance(category: CategoryUtils.EquipmentCategory): ChooseEquipmentFragment {
            return ChooseEquipmentFragment().apply {
                arguments = Bundle(1).apply {
                    putString(CATEGORY_KEY, category.toString())
                }
            }
        }
    }

    private lateinit var currentCategory: CategoryUtils.EquipmentCategory
    private lateinit var viewModel: ChooseEquipmentViewModel
    private lateinit var nextButton: Button
    private lateinit var recycleView: RecyclerView
    private lateinit var selectedModel: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideChooseEquipmentViewModel()
        viewModel = ViewModelProviders.of(this, factory).get(ChooseEquipmentViewModel::class.java)

        arguments?.getString(CATEGORY_KEY)?.let {categoryKey ->
            CategoryUtils.CATEGORY_MAP[categoryKey]?.let { equipmentCategory ->
                currentCategory = equipmentCategory
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (!::currentCategory.isInitialized) {
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        val view = inflater.inflate(R.layout.fragment_choose_equipment, null)

        val buttonBar = view.findViewById<View>(R.id.actionButtonLayout)
        nextButton = view.findViewById<Button>(R.id.nextButton).apply {
            setOnClickListener {
                flowController.onModelAndCategorySelected(selectedModel, currentCategory)
                flowController.onActionButtonClicked()
            }
        }

        recycleView = view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(true)
            addItemDecoration(ItemDivider(requireContext(), R.drawable.divider))
        }

        viewModel.isLoading.observe(this, Observer { loading ->
            if (loading == true) {
                flowController.showProgressBar()
                buttonBar.visibility = View.INVISIBLE
            } else {
                flowController.hideProgressBar()
            }
        })

        viewModel.serverError.observe(this, Observer { error ->
            if (error == true) {
                flowController.hideProgressBar()
                flowController.showServerErrorSnackBar()
            }
        })

        viewModel.categories.observe(this, Observer { categories ->
            categories?.let {map ->
                map[currentCategory.toString()]?.let {equipmentList ->
                    buttonBar.visibility = View.VISIBLE
                    recycleView.adapter = EquipmentModelAdapter(currentCategory, equipmentList) {
                        nextButton.isEnabled = true
                        selectedModel = it
                    }
                }
            }
        })

        return view
    }
}

private class EquipmentModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.imageView)
    private val selectedRadioButton: RadioButton = itemView.findViewById(R.id.radioButton)

    fun onBind(modelName: String, @DrawableRes resId: Int, isSelected: Boolean, onItemSelected: () -> Unit) {
        selectedRadioButton.text = modelName
        selectedRadioButton.isChecked = isSelected
        imageView.setImageResource(resId)

        itemView.setOnClickListener {
            if (!selectedRadioButton.isChecked) {
                selectedRadioButton.isChecked = true
                onItemSelected.invoke()
            }
        }
    }
}

private class EquipmentModelAdapter(private val category: CategoryUtils.EquipmentCategory, private val data: List<String>,
                                             private val onItemSelected: (selectedModel: String) -> Unit):
    RecyclerView.Adapter<EquipmentModelViewHolder>() {

    private var selectedViewIdx = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.select_equipment_view, parent, false)
        return EquipmentModelViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: EquipmentModelViewHolder, position: Int) {
        holder.onBind(data[position], CategoryUtils.getEquipmentImage(category, data[position]), position == selectedViewIdx) {
            val oldIndex = selectedViewIdx
            selectedViewIdx = position
            notifyItemChanged(oldIndex)
            onItemSelected.invoke(data[position])
        }
    }
}
