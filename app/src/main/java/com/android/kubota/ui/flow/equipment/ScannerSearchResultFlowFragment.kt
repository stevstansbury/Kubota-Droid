package com.android.kubota.ui.flow.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.android.kubota.R
import com.inmotionsoftware.flowkit.android.FlowFragment
import kotlinx.android.synthetic.main.fragment_manuals_page.view.*

interface ScannerSearchResultListener {
    fun onSelect(item: String)
}

class ScannerSearchResultViewAdapter(
    private val result: List<String>,
    private val listener: ScannerSearchResultListener?
) : RecyclerView.Adapter<ScannerSearchResultViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as String
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            listener?.onSelect(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_manuals_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = result[position]
        holder.contentView.text = item

        with(holder.view) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = result.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.content
        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}

class ScannerSearchResultFlowFragment
    : FlowFragment<List<String>, String>()
    , ScannerSearchResultListener {

    private var result: MutableLiveData<List<String>> = MutableLiveData()
    private var recyclerView: RecyclerView? = null

    override fun onInputAttached(input: List<String>) {
        this.result.postValue(input)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_search_result, container, false)
        this.recyclerView = view.findViewById(R.id.resultList)
        this.recyclerView?.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        this.result.observe(viewLifecycleOwner, Observer {
            this.updateView(it)
        })

        return view
    }

    override fun onSelect(item: String) {
        this.resolve(item)
    }

    private fun updateView(models: List<String>) {
        this.recyclerView?.adapter = ScannerSearchResultViewAdapter(models, this)
    }

}
