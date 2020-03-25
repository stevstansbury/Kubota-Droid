package com.android.kubota.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.camera.CameraSource
import com.android.kubota.extensions.showServerErrorSnackBar
import com.android.kubota.utility.CategoryUtils
import kotlinx.android.synthetic.main.fragment_scanner.*
import java.io.IOException

private const val VIEW_MODE_KEY = "ViewMode"
private const val SCAN_MODE = 0
private const val MANUAL_ENTRY_MODE = 1

class AddEquipmentFlowFragment: BaseFragment(), AddEquipmentFlowController, BackableFragment {

    companion object {
        fun createScanModeInstance(): AddEquipmentFlowFragment {
            return AddEquipmentFlowFragment().apply {
                arguments = Bundle(1).apply {
                    putInt(VIEW_MODE_KEY, SCAN_MODE)
                }
            }
        }

        fun createManualEntryModeInstance(): AddEquipmentFlowFragment {
            return AddEquipmentFlowFragment().apply {
                arguments = Bundle(1).apply {
                    putInt(VIEW_MODE_KEY, MANUAL_ENTRY_MODE)
                }
            }
        }
    }

    private var viewMode = MANUAL_ENTRY_MODE

    private var equipmentName: String? = null
    private var modelName: String? = null
    private var pinNumber: String? = null
    private var category: CategoryUtils.EquipmentCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.title = getString(R.string.add_equipment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_equipment_flow, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewMode = arguments?.getInt(VIEW_MODE_KEY) ?: MANUAL_ENTRY_MODE
            val targetFragment = when(viewMode) {
                SCAN_MODE -> ScannerFragment()
                else -> AddEquipmentFragment()
            }
            childFragmentManager.beginTransaction().replace(R.id.childFragmentPane, targetFragment).commit()
        }
    }

    override fun onBackPressed(): Boolean {

        val currentFragment = childFragmentManager.findFragmentById(R.id.childFragmentPane)
            ?: return false

        if (currentFragment is ChooseEquipmentFragment || (viewMode == MANUAL_ENTRY_MODE && currentFragment is ScannerFragment)) {
            loadAddEquipmentFragment()
            return true
        } else if (viewMode == SCAN_MODE && currentFragment is AddEquipmentFragment) {
            childFragmentManager.beginTransaction().replace(R.id.childFragmentPane, ScannerFragment()).commit()
            return true
        } else if (viewMode == MANUAL_ENTRY_MODE && currentFragment is ScannerFragment) {
            loadAddEquipmentFragment()
            return true
        }

        return false
    }

    override fun onConstructionCategorySelected() {
        loadChooseEquipmentFragment(CategoryUtils.EquipmentCategory.Construction())
    }

    override fun onMowersCategorySelected() {
        loadChooseEquipmentFragment(CategoryUtils.EquipmentCategory.Mowers())
    }

    override fun onTractorsCategorySelected() {
        loadChooseEquipmentFragment(CategoryUtils.EquipmentCategory.Tractors())
    }

    override fun onUTVCategorySelected() {
        loadChooseEquipmentFragment(CategoryUtils.EquipmentCategory.UtilityVehicles())
    }

    override fun setPinNumber(pin: String) {
        pinNumber = pin
    }

    override fun setEquipmentName(name: String) {
        equipmentName = name
    }

    override fun onModelAndCategorySelected(model: String, cat: CategoryUtils.EquipmentCategory) {
        modelName = model
        category = cat
    }

    override fun onActionButtonClicked() {
        val currentFragment = childFragmentManager.findFragmentById(R.id.childFragmentPane)

        if (currentFragment is ChooseEquipmentFragment) {
            loadAddEquipmentFragment()
        } else {
            flowActivity?.clearBackStack()
        }
    }

    override fun onScanLinkClicked() {
        childFragmentManager.beginTransaction().replace(R.id.childFragmentPane, ScannerFragment()).commit()
    }

    override fun showProgressBar() {
        flowActivity?.showProgressBar()
    }

    override fun hideProgressBar() {
        flowActivity?.hideProgressBar()
    }

    override fun showServerErrorSnackBar() {
        flowActivity?.showServerErrorSnackBar()
    }

    private fun loadChooseEquipmentFragment(category: CategoryUtils.EquipmentCategory) {
        childFragmentManager.beginTransaction().replace(R.id.childFragmentPane,
            ChooseEquipmentFragment.createInstance(category)).commit()
    }

    private fun loadAddEquipmentFragment() {
        val targetFragment = AddEquipmentFragment.createInstance(category,
            modelName, pinNumber, equipmentName)
        childFragmentManager.beginTransaction().replace(R.id.childFragmentPane, targetFragment).commit()
    }
}

interface AddEquipmentFlowController {
    fun onConstructionCategorySelected()
    fun onMowersCategorySelected()
    fun onTractorsCategorySelected()
    fun onUTVCategorySelected()
    fun setPinNumber(pin: String)
    fun setEquipmentName(name: String)
    fun onModelAndCategorySelected(model: String, cat: CategoryUtils.EquipmentCategory)
    fun onActionButtonClicked()
    fun onScanLinkClicked()
    fun showProgressBar()
    fun hideProgressBar()
    fun showServerErrorSnackBar()
}

abstract class AddEquipmentControlledFragment: Fragment() {
    protected lateinit var flowController: AddEquipmentFlowController

    override fun onAttach(context: Context) {
        super.onAttach(context)
        flowController = parentFragment as AddEquipmentFlowController
    }
}

class CustomCompoundButton: CompoundButton {

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
}