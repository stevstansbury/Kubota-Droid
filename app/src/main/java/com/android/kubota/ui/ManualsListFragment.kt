package com.android.kubota.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.kubota.R
import androidx.fragment.app.viewModels
import com.android.kubota.ui.equipment.EditEquipmentFragment
import com.android.kubota.ui.equipment.ModelManualFragment
import com.kubota.service.domain.ManualInfo
import kotlinx.android.synthetic.main.fragment_manuals_page.view.*


class ManualsListViewAdapter(
    private val mValues: List<ManualInfo>,
    private val mListener: ManualsListInteractionListener?
) : RecyclerView.Adapter<ManualsListViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as ManualInfo
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_manuals_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mContentView.text = item.title

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mContentView: TextView = mView.content
        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}

interface ManualsListInteractionListener {
    fun onListFragmentInteraction(item: ManualInfo?)
}

class ManualsListFragment : BaseFragment(), ManualsListInteractionListener {

    override val layoutResId = R.layout.fragment_manuals_list

    class ManualViewModel: ViewModel() {
        var modelName = ""
        var manualInfo = emptyList<ManualInfo>()
    }

    private val viewModel: ManualViewModel by viewModels()

    private var recyclerView: RecyclerView? = null

    companion object {
        private const val KEY_MODEL = "KEY_MODEL"
        private const val KEY_MANUAL_INFO = "KEY_MANUAL_INFO"

        @JvmStatic
        fun createInstance(modelName: String, manualInfo: List<ManualInfo>) =
            ManualsListFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_MODEL, modelName)
                    putParcelableArrayList(KEY_MANUAL_INFO, ArrayList(manualInfo))
                }
            }
    }

    override fun initUi(view: View) {
        val recyclerView = view as RecyclerView
        // Set the adapter
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = ManualsListViewAdapter(listOf(), this@ManualsListFragment)
        }

        this.recyclerView = recyclerView
    }

    override fun loadData() {
        val model = viewModel.modelName
        requireActivity().title = getString(R.string.manual_list_title, model)

        recyclerView?.adapter = ManualsListViewAdapter(viewModel.manualInfo, this)
    }

    override fun hasRequiredArgumentData(): Boolean {
        val model = arguments?.getString(KEY_MODEL)
        viewModel.modelName = model ?: ""

        val manualInfo = arguments?.getParcelableArrayList<ManualInfo>(KEY_MANUAL_INFO)
        viewModel.manualInfo = manualInfo ?: ArrayList()
        return model != null && manualInfo != null
    }

    override fun onListFragmentInteraction(item: ManualInfo?) {
        if (item == null) return

        if (item.url.path.endsWith("pdf", ignoreCase = true)) {
            // go to selection recycler view
            flowActivity?.addFragmentToBackStack(PDFFragment.createInstance(item.copy(title = viewModel.modelName)))
        } else {
            // go to selection recycler view
            flowActivity?.addFragmentToBackStack(ModelManualFragment.createInstance(item.copy(title = viewModel.modelName)))
        }
    }
}
