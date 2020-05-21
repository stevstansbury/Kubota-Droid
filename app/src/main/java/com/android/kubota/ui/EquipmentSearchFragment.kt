package com.android.kubota.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.databinding.FragmentManualEquipmentSearchBinding
import com.android.kubota.databinding.ListItemManualEquipmentSearchResultBinding
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.KubotaServiceError
import kotlinx.android.synthetic.main.fragment_manual_equipment_search.view.*
import kotlinx.android.synthetic.main.manual_equipment_search_form.view.*
import kotlinx.android.synthetic.main.manual_equipment_search_results.view.*
import java.util.*

class EquipmentSearchFragment(override val layoutResId: Int) : BaseFragment() {

    private var b: FragmentManualEquipmentSearchBinding? = null
    private val binding get() = b!!
    private val equipmentSearchViewModel: EquipmentSearchViewModel by viewModels()

    companion object {
        fun newInstance() = EquipmentSearchFragment(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.equipment_search)
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
        binding.root.searchResults.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.root.submit.setOnClickListener {
            it.isEnabled = false
            it.hideKeyboard()
            showResults()
        }
        binding.results.loading.setOnClickListener { showFormError() }
        binding.form.pin.onRightDrawableClicked { it.text.clear() }
        binding.form.three.onRightDrawableClicked { it.text.clear() }
        binding.results.pin.onRightDrawableClicked { it.text.clear() }
        binding.results.three.onRightDrawableClicked { it.text.clear() }
        return binding.root
    }

    override fun initUi(view: View) {

    }

    override fun loadData() {

    }

    private fun showResults() {
        binding.root.form.visibility = View.GONE
        binding.root.results.visibility = View.VISIBLE
        binding.results.pin.text = binding.form.pin.text
        binding.results.three.text = binding.form.three.text
        binding.results.loading.visibility = View.VISIBLE

        AppProxy.proxy.serviceManager.equipmentService
                .searchModels(partialModel = binding.form.three.text.toString(), serial = binding.form.pin.text.toString())
                .done { models ->
                    binding.root.results.searchResults.adapter =
                        EquipmentSearchResultAdapter(models.map { it.model }, itemCLickListener)

                    binding.results.loading.visibility = View.GONE
                    binding.results.searchResults.visibility = View.VISIBLE
                    binding.results.resultsTopDivider.visibility = View.VISIBLE
                }
                .catch {
                    binding.results.loading.visibility = View.GONE
                    showFormError(it)
                }
    }

    private fun showForm() {
        binding.root.form.visibility = View.VISIBLE
        binding.root.results.visibility = View.INVISIBLE
        binding.results.resultsTopDivider.visibility = View.INVISIBLE
    }

    private fun showFormError(error: Throwable? = null) {
        showForm()
        binding.form.instructionContainer.manualSearchInstructions.visibility = View.GONE
        binding.form.instructionContainer.error.visibility = View.VISIBLE
        binding.form.instructionContainer.error.text = when (error) {
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

    val itemCLickListener = View.OnClickListener { v ->
        val equipment = v?.tag as ListItemManualEquipmentSearchResultBinding
        this@EquipmentSearchFragment.parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentPane,
                EquipmentDetailFragment.createInstance(UUID.randomUUID()) // TODO: equipment.equipmentId goes here
            )
            .disallowAddToBackStack()
            .commit()
    }

}

class EquipmentSearchResultAdapter(
    private val items: List<String>,
    private val clickListener: View.OnClickListener
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

        holder.itemView.setOnClickListener { clickListener.onClick(it) }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)
}
