package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.android.kubota.ui.BaseFragment
import com.kubota.service.domain.FaultCode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FaultCodeListFragment : BaseFragment() {

    companion object {
        private const val FAULT_CODES_KEY = "FAULT_CODES_KEY"
        fun createInstance(faultCodes: List<FaultCode>): FaultCodeListFragment {
            return FaultCodeListFragment().apply {
                val data = Bundle(1)
                data.putParcelableArrayList(FAULT_CODES_KEY, ArrayList(faultCodes))
                arguments = data
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_recycler

    val faultCodes: List<FaultCode>
        get() = arguments?.getParcelableArrayList(FAULT_CODES_KEY)!!

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getParcelableArrayList<FaultCode>(FAULT_CODES_KEY) != null
    }

    override fun initUi(view: View) {
        view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = FaultCodeActiveAdapter(
                data = faultCodes,
                onItemClickListener = { faultCode ->
                    flowActivity?.addFragmentToBackStack(
                        FaultCodeResultsFragment.createInstance(faultCode)
                    )
                }
            )
        }
    }

    override fun loadData() {}
}

private class FaultCodeActiveAdapter(
    val data: List<FaultCode>,
    val onItemClickListener: (faultCode: FaultCode) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_item_fault_code_single_line, parent, false)
        return BindingHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val faultCode = data[position]

        val context = holder.itemView.context
        holder.itemView.findViewById<TextView>(R.id.codeText).text = faultCode.code?.toString()
            ?.let { "${context.getString(R.string.fault_mode_e_code)}: $it" }
            ?: faultCode.run { context.getString(R.string.fault_code_j1939, j1939Spn, j1939Fmi) }

        holder.itemView.findViewById<TextView>(R.id.codeTime).text =
            faultCode.timeReported?.alertTimeFormat()

        holder.itemView.setOnClickListener { this.onItemClickListener(faultCode) }
    }

    data class BindingHolder(val item: View) : RecyclerView.ViewHolder(item)

    private fun Date.alertTimeFormat(): String {
        val format = DateFormat.SHORT
        val date = SimpleDateFormat.getDateInstance(format).format(this)
        val time = SimpleDateFormat.getTimeInstance(format).format(this)
        return "$date $time"
    }
}