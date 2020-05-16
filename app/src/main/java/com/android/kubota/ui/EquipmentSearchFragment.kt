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
import com.android.kubota.databinding.FragmentManualEquipmentSearchBinding
import com.android.kubota.databinding.ListItemManualEquipmentSearchResultBinding
import com.android.kubota.extensions.hideKeyboard
import com.android.kubota.ui.equipment.EquipmentDetailFragment
import kotlinx.android.synthetic.main.fragment_manual_equipment_search.view.*
import kotlinx.android.synthetic.main.manual_equipment_search_form.view.*
import kotlinx.android.synthetic.main.manual_equipment_search_results.view.*
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class EquipmentSearchFragment(override val layoutResId: Int) : BaseFragment() {

    private var b: FragmentManualEquipmentSearchBinding? = null
    private val binding get() = b!!
    private val equipmentSearchViewModel: EquipmentSearchViewModel by viewModels()
    private var showResults: Boolean = false

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
            showResults() // TODO: actually get data from the API
        }
        binding.results.loading.setOnClickListener {
            loadJob?.cancel()
            showForm()
            binding.form.instructionContainer.manualSearchInstructions.visibility = View.GONE
            binding.form.instructionContainer.error.visibility = View.VISIBLE
            binding.form.instructionContainer.error.text = activity?.getString(
                R.string.unable_to_find_error,
                equipmentSearchViewModel.pin,
                equipmentSearchViewModel.three
            )
        }
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

    private fun getEquipmentList(): Flow<List<String>> = flow {
        delay(100)
        emit((0..20).map { "${equipmentSearchViewModel.three}-${equipmentSearchViewModel.pin + it}" })
    }

    private var loadJob: Job? = null

    private fun showResults() {
        binding.root.form.visibility = View.GONE
        binding.root.results.visibility = View.VISIBLE
        binding.results.pin.text = binding.form.pin.text
        binding.results.three.text = binding.form.three.text

        loadJob = GlobalScope.launch { // launch a new coroutine in background and continue
            delay(3000L)
            getEquipmentList().collect {
                fakeEquipmentList = it
                activity?.runOnUiThread {
                    binding.root.results.searchResults.adapter =
                        EquipmentSearchResultAdapter(fakeEquipmentList, itemCLickListener)
                    binding.results.loading.visibility = View.GONE
                    binding.results.searchResults.visibility = View.VISIBLE
                    binding.results.resultsTopDivider.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showForm() {
        binding.root.form.visibility = View.VISIBLE
        binding.root.results.visibility = View.GONE
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

    // TODO: [HACK] a list of items to display on screen. Replace with actual equipment
    private lateinit var fakeEquipmentList: List<String>
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
