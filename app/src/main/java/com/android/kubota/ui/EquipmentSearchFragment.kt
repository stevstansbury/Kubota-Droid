package com.android.kubota.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.databinding.FragmentManualEquipmentSearchBinding
import com.android.kubota.databinding.ListItemManualEquipmentSearchResultBinding
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.equipment.AddEquipmentFlow
import com.android.kubota.ui.equipment.AddEquipmentFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.SearchModelType
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import kotlinx.android.synthetic.main.fragment_manual_equipment_search.view.*
import java.util.*

class EquipmentSearchFragment : Fragment(), AddEquipmentFragment {

    private val addEquipmentFlowActivity: AddEquipmentFlow by lazy { requireActivity() as AddEquipmentFlow }
    private var b: FragmentManualEquipmentSearchBinding? = null
    private val binding get() = b!!
    private val equipmentSearchViewModel: EquipmentSearchViewModel by viewModels()

    companion object {
        fun newInstance() = EquipmentSearchFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.equipment_search)
        setHasOptionsMenu(false)
    }

    // BaseFragment does not offer data binding so suppress this warning
    @SuppressLint("MissingSuperCall")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_manual_equipment_search,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.viewModel = equipmentSearchViewModel
        binding.searchResults.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.submit.setOnClickListener {
            it.isEnabled = false
            it.hideKeyboard()
            showResults()
        }
        binding.loading.setOnClickListener { showFormError() }
        binding.pin.onRightDrawableClicked { it.text.clear() }
        binding.three.onRightDrawableClicked { it.text.clear() }

        return binding.root
    }

    private fun showResults() {
        binding.loading.visibility = View.VISIBLE

        AppProxy.proxy.serviceManager.equipmentService
                .searchModels(
                    SearchModelType.PartialModelAndSerial(
                        partialModel = binding.three.text.toString(),
                        serial = binding.pin.text.toString()
                    )
                )
                .done { models ->
                    binding.root.searchResults.adapter =
                        EquipmentSearchResultAdapter(models.map { it.model }) {
                            val equipment = createEquipmentUnit(
                                identifierType = EquipmentUnitIdentifier.Pin.name,
                                pinOrSerial = binding.textView3.text.toString(),
                                model = models[it].model
                            )
                            addEquipmentFlowActivity.addEquipment(equipment)
                        }

                    binding.loading.visibility = View.GONE
                    binding.searchResults.visibility = View.VISIBLE
                    binding.resultsTopDivider.visibility = View.VISIBLE
                }
                .catch {
                    binding.loading.visibility = View.GONE
                    showFormError(it)
                }
    }

    private fun showForm() {
        binding.error.visibility = View.GONE
        binding.loading.visibility = View.GONE
        binding.instructionContainer.visibility = View.GONE
        binding.resultsTopDivider.visibility = View.INVISIBLE
    }

    private fun showFormError(error: Throwable? = null) {
        showForm()
        binding.instructionContainer.manualSearchInstructions.visibility = View.GONE
        binding.instructionContainer.error.visibility = View.VISIBLE
        binding.instructionContainer.error.text = when (error) {
            null,
            is KubotaServiceError.NotFound -> {
                activity?.getString(
                    R.string.unable_to_find_error,
                    equipmentSearchViewModel.pin,
                    equipmentSearchViewModel.three
                )
            }
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                activity?.getString(R.string.connectivity_error_message)
            else ->
                activity?.getString(R.string.server_error_message)
        }
    }

    override fun onDestroy() {
        b = null
        super.onDestroy()
    }

    override fun showProgressBar() {

    }

    override fun hideProgressBar() {

    }

    override fun showError(throwable: Throwable) {
        val stringId = when (throwable) {
            is KubotaServiceError.NetworkConnectionLost,
            is KubotaServiceError.NotConnectedToInternet ->
                R.string.connectivity_error_message
            else ->
                R.string.server_error_message
        }

        Snackbar.make(binding.root, stringId, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    private fun createEquipmentUnit(identifierType: String, pinOrSerial: String?, model: String): EquipmentUnit {
        return EquipmentUnit(
            id = UUID.randomUUID(),
            model = model,
            category = null,
            identifierType = identifierType,
            pinOrSerial = pinOrSerial,
            pin = "",
            serial = null,
            nickName = null,
            userEnteredEngineHours = null,
            telematics = null,
            modelFullUrl = null,
            modelHeroUrl = null,
            modelIconUrl = null,
            guideUrl = null,
            manualInfo = emptyList(),
            warrantyUrl = null,
            hasFaultCodes = false,
            hasMaintenanceSchedules = false
        )
    }
}

class EquipmentSearchResultAdapter(
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
