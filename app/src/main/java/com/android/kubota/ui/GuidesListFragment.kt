package com.android.kubota.ui

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure

private const val KEY_MODEL_NAME = "model_name"

class GuidesListFragment: BaseFragment() {

    class GuideViewModel: ViewModel() {
        private var mModelName = MutableLiveData<String>()
        var modelName: LiveData<String> = mModelName

        fun updateModelName(name: String) {
            mModelName.value = name
        }
    }

    companion object {
        fun createInstance(modelName: String): GuidesListFragment {
            val fragment = GuidesListFragment()
            val arguments = Bundle(1)
            arguments.putString(KEY_MODEL_NAME, modelName)
            fragment.arguments = arguments

            return fragment
        }
    }

    override val layoutResId: Int = R.layout.fragment_guides_list

    private val viewModel: GuideViewModel by viewModels()

    private lateinit var recyclerListView: RecyclerView

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getString(KEY_MODEL_NAME)?.let {
            viewModel.updateModelName(it)
            true
        } ?: false
    }

    override fun initUi(view: View) {
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        this.viewModel.modelName.observe(this.viewLifecycleOwner, Observer {
            val model = it

            this.showProgressBar()
            AppProxy.proxy.serviceManager.guidesService.getGuideList(model)
                .done { onGuideListLoaded(model=model, guideList=it) }
                .ensure { this.hideProgressBar() }
                .catch { this.showError(it) }
        })

        activity?.setTitle(getString(R.string.guides_list_title, viewModel.modelName.value ?: ""))
    }

    override fun loadData() {
    }

    private fun onGuideListLoaded(model: String, guideList: List<String>) {
        recyclerListView.adapter = GuidesListAdapter(guideList, object : GuideItemView.OnClickListener {
            override fun onClick(guideItem: String) {
                if (isResumed) {
                    MaintenanceGuideActivity.launchMaintenanceGuideActivity(requireContext(), model, guideItem)
                }
            }

        })
    }
}

private class GuideItemView(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val textView: TextView = itemView.findViewById(R.id.textView)

    fun onBind(guideItem: String, listener: OnClickListener) {
        textView.text = guideItem
        itemView.setOnClickListener { listener.onClick(guideItem) }
    }

    interface OnClickListener {
        fun onClick(guideItem: String)
    }
}


private class GuidesListAdapter(
    private val data: List<String>,
    val clickListener: GuideItemView.OnClickListener
): RecyclerView.Adapter<GuideItemView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GuideItemView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_guide_list_item, viewGroup, false)

        return GuideItemView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: GuideItemView, position: Int) {
        holder.onBind(data[position], clickListener)
    }

}