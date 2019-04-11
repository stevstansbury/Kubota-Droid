package com.android.kubota.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.viewmodel.UIModel
import com.kubota.repository.prefs.GuidesRepo
import kotlinx.coroutines.*

private const val KEY_MODEL_NAME = "model_name"

class GuidesListFragment: BaseFragment() {

    companion object {
        fun createInstance(uiModel: UIModel): GuidesListFragment {
            val fragment = GuidesListFragment()
            val arguments = Bundle(1)
            arguments.putString(KEY_MODEL_NAME, uiModel.modelName)
            fragment.arguments = arguments

            return fragment
        }
    }

    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)
    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)
    private lateinit var model: String
    private lateinit var repo: GuidesRepo
    private lateinit var recyclerListView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_guides_list, null)
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
        }
        val model = arguments?.getString(KEY_MODEL_NAME)

        if (model != null) {
            flowActivity?.showProgressBar()
            this.model = model
            repo = GuidesRepo(this.model)
            activity?.title = getString(R.string.guides_list_title, model)
            loadGuideList()
        } else {
            activity?.onBackPressed()
        }

        return view
    }

    private fun loadGuideList() {
        backgroundScope.launch {
            val results = repo.getGuideList()

            withContext(uiScope.coroutineContext) {
                onGuideListLoaded(results)
            }
        }
    }

    private fun onGuideListLoaded(guideList: List<String>) {
        recyclerListView.adapter = GuidesListAdapter(guideList, object : GuideItemView.OnClickListener {
            override fun onClick(guideItem: String) {
                MaintenanceGuideActivity.launchMaintenanceGuideActivity(requireContext(), model, guideItem)
            }

        })
        flowActivity?.hideProgressBar()
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


private class GuidesListAdapter(private val data: List<String>, val clickListener: GuideItemView.OnClickListener): RecyclerView.Adapter<GuideItemView>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GuideItemView {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.view_guide_list_item, viewGroup, false)

        return GuideItemView(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: GuideItemView, position: Int) {
        holder.onBind(data[position], clickListener)
    }

}