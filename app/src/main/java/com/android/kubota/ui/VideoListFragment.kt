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
import com.kubota.service.domain.VideoInfo
import kotlinx.android.synthetic.main.fragment_manuals_page.view.*


class VideoListViewAdapter(
    private val mValues: List<VideoInfo>,
    private val onListFragmentInteraction: ((VideoInfo) -> Unit)
) : RecyclerView.Adapter<VideoListViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as VideoInfo
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            onListFragmentInteraction(item)
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

class VideoListFragment : BaseFragment() {

    override val layoutResId = R.layout.fragment_manuals_list

    class VideoViewModel : ViewModel() {
        var modelName = ""
        var videoInfo = emptyList<VideoInfo>()
    }

    private val viewModel: VideoViewModel by viewModels()

    companion object {
        private const val KEY_MODEL = "KEY_MODEL"
        private const val KEY_VIDEO_INFO = "KEY_MANUAL_INFO"

        @JvmStatic
        fun createInstance(modelName: String, videoInfo: List<VideoInfo>) =
            VideoListFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_MODEL, modelName)
                    putParcelableArrayList(KEY_VIDEO_INFO, ArrayList(videoInfo))
                }
            }
    }

    override fun initUi(view: View) {
        val recyclerView = view as RecyclerView
        // Set the adapter
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = VideoListViewAdapter(viewModel.videoInfo) {
                val intent = VideoPlayerActivity.intent(
                    context = requireContext(),
                    video = it
                )
                activity?.startActivity(intent)
            }
        }
    }

    override fun loadData() {
        requireActivity().title = getString(R.string.videos_for_model, viewModel.modelName)
    }

    override fun hasRequiredArgumentData(): Boolean {
        val model = arguments?.getString(KEY_MODEL)
        viewModel.modelName = model ?: ""

        val manualInfo = arguments?.getParcelableArrayList<VideoInfo>(KEY_VIDEO_INFO)
        viewModel.videoInfo = manualInfo ?: ArrayList()
        return model != null && manualInfo != null
    }
}
