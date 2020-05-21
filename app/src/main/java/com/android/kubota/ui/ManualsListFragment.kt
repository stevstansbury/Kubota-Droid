package com.android.kubota.ui

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import kotlinx.android.parcel.Parcelize
import androidx.fragment.app.viewModels
import com.android.kubota.ui.equipment.ManualLink
import com.android.kubota.ui.equipment.ModelManualFragment
import com.kubota.service.api.KubotaServiceError
import kotlinx.android.synthetic.main.fragment_manuals_page.view.*

@Parcelize data class ModelManual(val model: String, val manual: String?): Parcelable

class ManualsListViewAdapter(
    private val mValues: List<ModelManual>,
    private val mListener: ManualsListInteractionListener?
) : RecyclerView.Adapter<ManualsListViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as ModelManual
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
        holder.mContentView.text = item.model

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
    fun onListFragmentInteraction(item: ModelManual?)
}

class ManualsListFragment : BaseFragment(), ManualsListInteractionListener {

    protected override val layoutResId = R.layout.fragment_manuals_list

    class ManualViewModel: ViewModel() {
        var modelName = MutableLiveData<String>()
    }

    private val viewModel: ManualViewModel by viewModels()

    private var recyclerView: RecyclerView? = null

    companion object {
        private const val KEY_MODEL = "KEY_MODEL"

        @JvmStatic
        fun createInstance(modelName: String) =
            ManualsListFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_MODEL, modelName)
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
        val model = viewModel.modelName.value ?: return
        requireActivity().title = getString(R.string.manual_title, model)

        this.showProgressBar()
        AppProxy.proxy.serviceManager.equipmentService.getManualURL(model=model)
            .done {
                val uri = it.toURI().toString()
                val item = ModelManual(model=model, manual=uri)
                recyclerView?.adapter = ManualsListViewAdapter(listOf(item), this@ManualsListFragment )
            }
            .catch {
                when (it) {
                    is KubotaServiceError.NotFound -> {}
                    else -> this.showError(it)
                }
            }
            .finally { this.hideProgressBar() }


    }

    override fun hasRequiredArgumentData(): Boolean {
        val model = arguments?.getString(KEY_MODEL)
        viewModel.modelName.value = model
        return model != null
    }

    override fun onListFragmentInteraction(item: ModelManual?) {
        if (item == null) return
        // go to selection recycler view
        this.parentFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentPane,ModelManualFragment.createInstance(model=ManualLink(title=item.model, uri=Uri.parse(item.manual))))
            .addToBackStack(null)
            .commit()
    }
}
