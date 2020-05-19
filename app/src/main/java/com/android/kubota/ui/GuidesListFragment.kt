package com.android.kubota.ui

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.kubota.R
import com.android.kubota.extensions.showServerErrorSnackBar
import com.android.kubota.utility.Utils
//import com.kubota.repository.prefs.GuidesRepo

private const val KEY_MODEL_NAME = "model_name"

class GuidesListFragment: BaseFragment() {

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

    private lateinit var model: String
//    private lateinit var repo: GuidesRepo
    private lateinit var recyclerListView: RecyclerView

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getString(KEY_MODEL_NAME)?.let {
            model = it
//            repo = GuidesRepo(this.model)

            true
        } ?: false
    }

    override fun initUi(view: View) {
        recyclerListView = view.findViewById<RecyclerView>(R.id.recyclerList).apply {
            setHasFixedSize(true)
        }

        activity?.setTitle(getString(R.string.guides_list_title, model))
    }

    override fun loadData() {
        // FIXME: Integrate with KubotaService

//        flowActivity?.showProgressBar()
//        Utils.backgroundTask {
//            when (val result = repo.getGuideList()) {
//                is GuidesRepo.Response.Success -> {
//                    Utils.uiTask {
//                        onGuideListLoaded(result.data)
//                    }
//                }
//                is GuidesRepo.Response.Failure -> flowActivity?.showServerErrorSnackBar()
//            }
//            Utils.uiTask {
//                flowActivity?.hideProgressBar()
//            }
//        }
    }

    private fun onGuideListLoaded(guideList: List<String>) {
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