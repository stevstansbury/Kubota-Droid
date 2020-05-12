package com.android.kubota.ui

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
import androidx.recyclerview.widget.LinearLayoutManager
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

class EquipmentSearchFragment : Fragment() {

    private var b: FragmentManualEquipmentSearchBinding? = null
    private val binding get() = b!!
    private val equipmentSearchViewModel: EquipmentSearchViewModel by viewModels()
    private var showResults: Boolean = false

    companion object {
        fun newInstance() = EquipmentSearchFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_manual_equipment_search, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = equipmentSearchViewModel
        binding.root.results.searchResults.adapter = EquipmentSearchResultAdapter(fakeEquipmentList,
            View.OnClickListener { v ->
                val equipment = v?.tag as ListItemManualEquipmentSearchResultBinding
                this@EquipmentSearchFragment.parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentPane,
                        EquipmentDetailFragment.createInstance(UUID.randomUUID()) // TODO: equipment.equipmentId goes here
                    )
                    .addToBackStack(null)
                    .commit()
            })
        binding.root.searchResults.addItemDecoration(
            DividerItemDecoration(
                context,
                (binding.root.searchResults.layoutManager as LinearLayoutManager).orientation
            )
        )
        binding.root.submit.setOnClickListener {
            it.hideKeyboard()
            toggleSearchResults() // TODO: actually get data from the API
        }
        return binding.root
    }

    private fun toggleSearchResults() {
        showResults = showResults xor true

        when (showResults) {
            true -> {
                binding.root.form.visibility = View.GONE
                binding.root.results.visibility = View.VISIBLE
            }
            else -> {
                binding.root.form.visibility = View.VISIBLE
                binding.root.results.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        b = null
        super.onDestroy()
    }
}

// TODO: [HACK] a list of items to display on screen. Replace with actual equipment
private val fakeEquipmentList = (1..20).map { "Item $it" }

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
