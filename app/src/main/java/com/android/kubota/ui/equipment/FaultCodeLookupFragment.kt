package com.android.kubota.ui.equipment

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.SearchFaultCode
import com.kubota.service.domain.FaultCode


enum class LookupMode {
    All,
    Dtc,
    J1939
}

data class FaultLookupScreenState(
    val mode: LookupMode,
    val searchQuery: SearchFaultCode,
    val results: List<FaultCode>? // when null is actively loading
)

class FaultCodeLookupViewModel : ViewModel() {
    private val equipmentService = AppProxy.proxy.serviceManager.equipmentService
    lateinit var modelName: String

    val currentState by lazy {
        MutableLiveData(
            FaultLookupScreenState(
                mode = LookupMode.All,
                searchQuery = SearchFaultCode.All(modelName, ""),
                results = emptyList()
            )
        )
    }

    fun init(
        modelName: String,
        results: List<FaultCode>,
        mode: LookupMode,
        searchQuery: SearchFaultCode
    ) {
        this.modelName = modelName
        currentState.value = currentState.value?.copy(
            mode = mode,
            results = results,
            searchQuery = searchQuery
        )
    }

    fun searchFaultCodes(spn: String?, fmi: String?) {
        val query = SearchFaultCode.J1939(modelName, spn, fmi)
        currentState.value = currentState.value!!.copy(searchQuery = query, results = null)
        equipmentService.searchFaultCodes(query).done {
            currentState.value = currentState.value!!.copy(results = it)
        }
    }

    fun searchFaultCodes(code: String) {
        val query = when (currentState.value!!.mode) {
            LookupMode.All -> SearchFaultCode.All(modelName, code)
            LookupMode.Dtc -> SearchFaultCode.Dtc(modelName, code)
            LookupMode.J1939 -> throw IllegalStateException()
        }
        currentState.value = currentState.value!!.copy(searchQuery = query, results = null)
        equipmentService.searchFaultCodes(query).done {
            currentState.value = currentState.value!!.copy(results = it)
        }
    }

    fun modeChanged(mode: LookupMode) {
        currentState.value = currentState.value!!.copy(mode = mode)
    }
}

class FaultCodeLookupFragment : Fragment() {
    companion object {
        private const val MODEL_NAME_KEY = "MODEL_NAME_KEY"

        fun createInstance(modelName: String): FaultCodeLookupFragment {
            return FaultCodeLookupFragment().apply {
                val data = Bundle(1)
                data.put(MODEL_NAME_KEY, modelName)
                arguments = data
            }
        }
    }

    private val viewModel: FaultCodeLookupViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val selectedColor by lazy { ContextCompat.getColorStateList(
        requireContext(),
        R.color.fault_code_type_selected
    ) }
    private val unselectedColor by lazy { ContextCompat.getColorStateList(
        requireContext(),
        R.color.fault_code_type_unselected
    ) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelName: String = arguments
            ?.getT(MODEL_NAME_KEY)
            ?: throw IllegalArgumentException()

        val faultResults = savedInstanceState?.getParcelableArrayList<FaultCode>("lookupState")
        val mode = savedInstanceState?.getString("mode")
            ?.let { LookupMode.valueOf(it) }
            ?: LookupMode.All

        val searchFaultCode = savedInstanceState?.let {
            when (mode) {
                LookupMode.All -> SearchFaultCode.All(modelName, it.getString("code1") ?: "")
                LookupMode.Dtc -> SearchFaultCode.Dtc(modelName, it.getString("code1") ?: "")
                LookupMode.J1939 -> SearchFaultCode.J1939(
                    model = modelName,
                    spn = it.getString("code1") ?: "",
                    fmi = it.getString("code2") ?: ""
                )
            }
        } ?: SearchFaultCode.All(modelName, "")

        faultResults?.let {
            val fragment = FaultCodeListFragment.createInstance(it)
            childFragmentManager
                .beginTransaction()
                .replace(R.id.resultsFragmentContainer, fragment, "faultList")
                .commit()
        }

        viewModel.init(modelName, faultResults ?: emptyList(), mode, searchFaultCode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val results = viewModel.currentState.value?.results ?: emptyList()
        outState.putParcelableArrayList("lookupState", ArrayList(results))
        outState.putString("mode", viewModel.currentState.value?.mode?.name)
        outState.putString(
            "code1",
            view?.findViewById<TextView>(R.id.faultCodeEditText)?.text?.toString()
        )
        outState.putString(
            "code2",
            view?.findViewById<TextView>(R.id.faultCodeEditTextExtra)?.text?.toString()
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_fault_lookup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.fault_code_screen_title)

        val etLayout = view.findViewById<TextInputLayout>(R.id.faultCodeEditTextLayout)
        val et = etLayout.findViewById<TextInputEditText>(R.id.faultCodeEditText)
        val etLayoutExtra = view.findViewById<TextInputLayout>(R.id.faultCodeEditTextLayoutExtra)
        val etExtra = etLayoutExtra.findViewById<TextInputEditText>(R.id.faultCodeEditTextExtra)
        val progressBar = view.findViewById<ProgressBar>(R.id.faultLookupProgress)
        val resultsGroup = view.findViewById<Group>(R.id.faultResultsGroup)

        val searchAllButton = view.findViewById<MaterialButton>(R.id.searchAllButton)
        val searchECodeButton = view.findViewById<MaterialButton>(R.id.searchECodeButton)
        val searchJ1939Button = view.findViewById<MaterialButton>(R.id.searchJ1939Button)

        val setButtonColor : (LookupMode) -> Unit = { mode ->
            searchAllButton.backgroundTintList = if (mode == LookupMode.All) selectedColor else unselectedColor
            searchECodeButton.backgroundTintList = if (mode == LookupMode.Dtc) selectedColor else unselectedColor
            searchJ1939Button.backgroundTintList = if (mode == LookupMode.J1939) selectedColor else unselectedColor
        }

        viewModel.currentState.observe(this.viewLifecycleOwner) { state ->
            setButtonColor(state!!.mode)

            when (state.results == null) {
                true -> {
                    progressBar.visibility = View.VISIBLE
                    resultsGroup.visibility = View.GONE
                }
                false -> {
                    val existingFaults = childFragmentManager
                        .findFragmentByTag("faultList")
                        ?.let { it as FaultCodeListFragment }
                        ?.faultCodes

                    if (existingFaults != state.results) {
                        val fragment = FaultCodeListFragment.createInstance(state.results)
                        childFragmentManager
                            .beginTransaction()
                            .replace(R.id.resultsFragmentContainer, fragment, "faultList")
                            .commit()
                    }
                    Handler(view.context.mainLooper).postDelayed(delayFor = 200L) {
                        resultsGroup.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }
            }

            when (state.mode) {
                LookupMode.All -> renderAll(etLayout, etLayoutExtra)
                LookupMode.Dtc -> renderECode(etLayout, etLayoutExtra)
                LookupMode.J1939 -> renderJ1939(etLayout, etLayoutExtra)
            }

            view.findViewById<MaterialButton>(R.id.lookUpButton).setOnClickListener { _ ->
                hideKeyboard()
                when (state.mode) {
                    LookupMode.J1939 -> {
                        val spn = et.text?.toString()
                        val fmi = etExtra.text?.toString()
                        viewModel.searchFaultCodes(spn = spn, fmi = fmi)
                    }
                    LookupMode.All,
                    LookupMode.Dtc -> {
                        val code = et.text?.toString() ?: ""
                        viewModel.searchFaultCodes(code = code)
                    }
                }
            }
        }

        searchAllButton.setOnClickListener { viewModel.modeChanged(LookupMode.All) }
        searchECodeButton.setOnClickListener { viewModel.modeChanged(LookupMode.Dtc) }
        searchJ1939Button.setOnClickListener { viewModel.modeChanged(LookupMode.J1939) }
    }

    private fun renderAll(etLayout: TextInputLayout, etLayoutExtra: TextInputLayout) {
        etLayout.hint = getString(R.string.search_all_fault_codes)
        etLayout.prefixText = ""
        etLayoutExtra.visibility = View.GONE
    }

    private fun renderECode(etLayout: TextInputLayout, etLayoutExtra: TextInputLayout) {
        etLayout.hint = getString(R.string.search_e_fault_codes)
        etLayout.prefixText = getString(R.string.fault_e)
        etLayoutExtra.visibility = View.GONE
    }

    private fun renderJ1939(etLayout: TextInputLayout, etLayoutExtra: TextInputLayout) {
        etLayout.hint = getString(R.string.search_spn_fault_codes)
        etLayout.prefixText = getString(R.string.fault_spn)
        etLayoutExtra.visibility = View.VISIBLE
        etLayoutExtra.hint = getString(R.string.search_fmi_fault_codes)
        etLayoutExtra.prefixText = getString(R.string.fmi_fault)
    }

    private fun hideKeyboard() {
        requireContext()
            .getSystemService(Activity.INPUT_METHOD_SERVICE)
            .let { it as InputMethodManager}
            .hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}

private fun Handler.postDelayed(delayFor: Long, action: () -> Unit): Boolean {
    return postDelayed({ action() }, delayFor)
}