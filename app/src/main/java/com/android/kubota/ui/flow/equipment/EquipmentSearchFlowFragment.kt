package com.android.kubota.ui.flow.equipment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.coordinator.state.EquipmentSearchInput
import com.android.kubota.databinding.FragmentManualEquipmentSearchBinding
import com.android.kubota.databinding.ListItemManualEquipmentSearchResultBinding
import com.android.kubota.extensions.displayName
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.EquipmentSearchViewModel
import com.android.kubota.ui.onRightDrawableClicked
import com.inmotionsoftware.flowkit.android.FlowFragment
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.caseInsensitiveSort
import com.kubota.service.domain.EquipmentModel
import kotlinx.android.synthetic.main.fragment_manual_equipment_search.view.*

class EquipmentSearchFlowFragment
    : FlowFragment<EquipmentSearchInput, EquipmentSearchFlowFragment.Result>() {

    sealed class Result {
        class Search(val serial: String, val modelName: String) : Result()
        class Select(val serial: String, val model: EquipmentModel) : Result()
    }

    private var b: FragmentManualEquipmentSearchBinding? = null
    private val binding get() = b!!
    private val equipmentSearchViewModel: EquipmentSearchViewModel by viewModels()

    private var input: MutableLiveData<EquipmentSearchInput> = MutableLiveData()

    override fun onInputAttached(input: EquipmentSearchInput) {
        this.input.postValue(input)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    // BaseFragment does not offer data binding so suppress this warning
    @SuppressLint("MissingSuperCall")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_manual_equipment_search,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.viewModel = equipmentSearchViewModel
        binding.root.searchResults.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.btnSubmit.setOnClickListener {
            it.isEnabled = false
            it.hideKeyboard()
            showResults()
        }
        binding.loading.setOnClickListener { showFormError() }
        binding.etPinSerial.onRightDrawableClicked { it.text.clear() }
        binding.etModel.onRightDrawableClicked { it.text.clear() }

        this.input.observe(viewLifecycleOwner, {
            this.updateView(it)
        })

        return binding.root
    }

    private fun showResults() {
        binding.loading.visibility = View.VISIBLE
        binding.instructionContainer.visibility = View.GONE
        binding.resultsTopDivider.visibility = View.GONE
        binding.searchResults.visibility = View.GONE

        this.resolve(
            Result.Search(
                serial = binding.etPinSerial.text.toString(),
                modelName = binding.etModel.text.toString()
            )
        )

    }

    private fun updateView(input: EquipmentSearchInput) {
        equipmentSearchViewModel.setEquipmentType(input.equipmentType)

        if (input.equipmentType == EquipmentModel.Type.Machine) {
            activity?.setTitle(R.string.equipment_search)
        } else {
            activity?.setTitle(R.string.add_equipment_attachment)

            binding.etModel.doOnTextChanged { text, _, _, _ ->
                if (text.toString().isNotEmpty()) {
                    this.resolve(
                        Result.Search(
                            serial = binding.etPinSerial.text.toString(),
                            modelName = text.toString()
                        )
                    )
                } else {
                    binding.instructionContainer.manualSearchInstructions.visibility = View.VISIBLE
                    showForm()
                }
            }
        }

        val models = input.result.caseInsensitiveSort { it.displayName }

        when {
            models.isNotEmpty() -> {
                binding.root.searchResults.adapter =
                    EquipmentSearchFlowResultAdapter(models.map { it.displayName }) {
                        if (binding.etPinSerial.text.toString().length >= 5) {
                            this.resolve(
                                Result.Select(
                                    serial = binding.etPinSerial.text.toString(),
                                    model = models[it]
                                )
                            )
                        }
                    }

                binding.instructionContainer.manualSearchInstructions.visibility = View.GONE
                binding.instructionContainer.error.visibility = View.GONE
                binding.loading.visibility = View.GONE
                binding.searchResults.visibility = View.VISIBLE
                binding.resultsTopDivider.visibility = View.VISIBLE
            }
            binding.loading.visibility == View.VISIBLE -> {
                this.showFormError(input.error)
            }
            binding.etModel.text.toString().isNotEmpty() -> {
                this.showFormError(KubotaServiceError.NotFound())
            }
        }
    }

    private fun showForm() {
        binding.resultsTopDivider.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.searchResults.visibility = View.GONE
    }

    private fun showFormError(error: Throwable? = null) {
        showForm()
        binding.instructionContainer.manualSearchInstructions.visibility = View.GONE
        binding.instructionContainer.error.visibility = View.VISIBLE
        binding.instructionContainer.visibility = View.VISIBLE
        binding.instructionContainer.error.text = when (error) {
            null,
            is KubotaServiceError.NotFound -> {
                activity?.getString(
                    R.string.unable_to_find_error,
                    equipmentSearchViewModel.pinOrSerial,
                    equipmentSearchViewModel.model
                )
            }
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                activity?.getString(R.string.connectivity_error_message)
            is KubotaServiceError.ServerMaintenance ->
                activity?.getString(R.string.server_maintenance)
            else ->
                activity?.getString(R.string.server_error_message)
        }
    }

    override fun onDestroy() {
        b = null
        super.onDestroy()
    }
}

class EquipmentSearchFlowResultAdapter(
    private val items: List<String>,
    private val clickListener: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: ListItemManualEquipmentSearchResultBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.list_item_manual_equipment_search_result,
            parent,
            false
        )

        binding.root.tag = binding
        return BindingHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding: ViewDataBinding =
            holder.itemView.tag as ListItemManualEquipmentSearchResultBinding
        binding.setVariable(BR.equipmentId, items[position])
        //        binding.setVariable(BR.equipment, equipment) // TODO: this is where we need to bind data

        holder.itemView.setOnClickListener { clickListener.invoke(position) }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}
