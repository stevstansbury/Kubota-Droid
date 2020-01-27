package com.android.kubota.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.android.kubota.R
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.AddEquipmentViewModel
import com.android.kubota.viewmodel.EquipmentSearchResults

class AddEquipmentFragment : AddEquipmentControlledFragment() {
    private lateinit var viewModel: AddEquipmentViewModel

    private lateinit var addEquipmentButton: Button
    private lateinit var constructionCategoryButton: CustomCompoundButton
    private lateinit var mowersCategoryButton: CustomCompoundButton
    private lateinit var tractorsCategoryButton: CustomCompoundButton
    private lateinit var utvCategoryButton: CustomCompoundButton
    private lateinit var pinEditText: EditText
    private lateinit var equipmentNameEditText: EditText
    private lateinit var equipmentModelTextView: TextView

    companion object {
        const val KEY_SEARCH_RESULT = "SEARCH_RESULT"
        private const val SEARCH_REQUEST_CODE = 100

        private const val CATEGORY_TYPE = "CategoryType"
        private const val MODEL = "Model"
        private const val PIN_NUMBER = "PinNumber"
        private const val EQUIPMENT_NAME = "EquipmentName"

        fun createInstance(category: CategoryUtils.EquipmentCategory?, modelName: String?, pinNumber: String?,
                           equipmentName: String?): AddEquipmentFragment {
            return AddEquipmentFragment().apply {
                arguments = Bundle(4).apply {
                    putString(CATEGORY_TYPE, category.toString())
                    putString(MODEL, modelName)
                    putString(PIN_NUMBER, pinNumber)
                    putString(EQUIPMENT_NAME, equipmentName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = InjectorUtils.provideAddEquipmentViewModel(requireContext())
        viewModel = ViewModelProviders.of(this, factory).get(AddEquipmentViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_equipment, null)

        tractorsCategoryButton = view.findViewById(R.id.tractorsCategoryButton)
        utvCategoryButton = view.findViewById(R.id.utvCategoryButton)
        mowersCategoryButton = view.findViewById(R.id.mowersCategoryButton)
        constructionCategoryButton = view.findViewById(R.id.constructionCategoryButton)
        pinEditText = view.findViewById(R.id.pinEditText)
        equipmentNameEditText = view.findViewById(R.id.equipmentNameEditText)
        equipmentModelTextView = view.findViewById(R.id.modelTextView)
        addEquipmentButton = view.findViewById(R.id.addButton)
        val scanTextView = view.findViewById<TextView>(R.id.scanHelperTextView)
        val scanText = getString(R.string.scan)
        val scanHelperText = getString(R.string.pin_helper_text, scanText)
        val startIdx = scanHelperText.indexOf(scanText)
        val endIdx = startIdx + scanText.length
        val spannableString = SpannableString(scanHelperText)
        spannableString.setSpan(object: ClickableSpan() {

            override fun onClick(widget: View) {
                flowController.onScanLinkClicked()
            }

        }, startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        scanTextView.text = spannableString
        scanTextView.movementMethod = LinkMovementMethod.getInstance()

        tractorsCategoryButton.setOnClickListener {
            onCategoryButtonClicked(it)
        }
        utvCategoryButton.setOnClickListener {
            onCategoryButtonClicked(it)
        }
        mowersCategoryButton.setOnClickListener {
            onCategoryButtonClicked(it)
        }
        constructionCategoryButton.setOnClickListener {
            onCategoryButtonClicked(it)
        }

        arguments?.getString(CATEGORY_TYPE)?.let {categoryKey ->
            CategoryUtils.CATEGORY_MAP[categoryKey]?.let { category ->
                when(category) {
                    is CategoryUtils.EquipmentCategory.Construction -> constructionCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.Mowers -> mowersCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.Tractors -> tractorsCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.UtilityVehicles -> utvCategoryButton.isChecked = true
                }
            }
        }
        arguments?.getString(MODEL)?.let {
            equipmentModelTextView.visibility = View.VISIBLE
            equipmentModelTextView.text = it
        }
        arguments?.getString(PIN_NUMBER)?.let { pinEditText.setText(it) }
        arguments?.getString(EQUIPMENT_NAME)?.let { equipmentNameEditText.setText(it) }

        val hasCategory = constructionCategoryButton.isChecked || mowersCategoryButton.isChecked ||
                tractorsCategoryButton.isChecked || utvCategoryButton.isChecked

        addEquipmentButton.isEnabled = equipmentModelTextView.text.isNullOrBlank().not() && hasCategory
        addEquipmentButton.setOnClickListener {
            addEquipmentButton.isEnabled = false
            viewModel.add(nickName = equipmentNameEditText.text.toString(),
                model = equipmentModelTextView.text.toString(),
                serialNumber = pinEditText.text.toString(), category = when {
                    constructionCategoryButton.isChecked -> CategoryUtils.CONSTRUCTION_CATEGORY
                    mowersCategoryButton.isChecked -> CategoryUtils.MOWERS_CATEGORY
                    tractorsCategoryButton.isChecked -> CategoryUtils.TRACTORS_CATEGORY
                    else -> CategoryUtils.UTILITY_VEHICLES_CATEGORY
                })
            addEquipmentButton.hideKeyboard()
            flowController.onActionButtonClicked()

        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.search -> {
                val intent = Intent(this.activity, SearchActivity::class.java).apply {
                    putExtra(SearchActivity.KEY_MODE, SearchActivity.ADD_EQUIPMENT_MODE)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivityForResult(intent, SEARCH_REQUEST_CODE)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getParcelableExtra<EquipmentSearchResults>(KEY_SEARCH_RESULT)?.let {
                flowController.onModelAndCategorySelected(it.model, it.category)
                constructionCategoryButton.isChecked = false
                mowersCategoryButton.isChecked = false
                tractorsCategoryButton.isChecked = false
                utvCategoryButton.isChecked = false
                when(it.category) {
                    is CategoryUtils.EquipmentCategory.Construction -> constructionCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.Mowers -> mowersCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.Tractors -> tractorsCategoryButton.isChecked = true
                    is CategoryUtils.EquipmentCategory.UtilityVehicles -> utvCategoryButton.isChecked = true
                }
                equipmentModelTextView.text = it.model
                equipmentModelTextView.visibility = View.VISIBLE
                addEquipmentButton.isEnabled = true

                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setPinAndModelName() {
        flowController.setPinNumber(pin = pinEditText.text.toString())
        flowController.setEquipmentName(name = equipmentNameEditText.text.toString())
    }

    private fun onCategoryButtonClicked(view: View) {
        view.hideKeyboard()
        setPinAndModelName()
        when (view) {
            tractorsCategoryButton -> flowController.onTractorsCategorySelected()
            mowersCategoryButton -> flowController.onMowersCategorySelected()
            constructionCategoryButton -> flowController.onConstructionCategorySelected()
            else -> flowController.onUTVCategorySelected()
        }
    }
}