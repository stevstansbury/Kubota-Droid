package com.android.kubota.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.kubota.service.domain.VideoInfo
import kotlin.collections.ArrayList


private const val KEY_MODEL_NAME = "model_name"
private const val INSTRUCTION_VIDEO_URI = "video_uri"


class InstructionVideoPlayerFragment : BaseFragment() {

    private lateinit var videoView: PlayerView
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem

    override val layoutResId: Int = R.layout.fragment_instruction_video_player

    private val viewModel: InstructionVideoPlayerViewModel by viewModels()

    class InstructionVideoPlayerViewModel : ViewModel() {
        var modelName = ""

        var instructionalVideos = emptyList<VideoInfo>()

    }

    companion object {
        fun createInstance(
            modelName: String,
            instructionalVideos: List<VideoInfo>
        ): InstructionVideoPlayerFragment {
            val fragment = InstructionVideoPlayerFragment()
            val arguments = Bundle(1)
            arguments.putString(KEY_MODEL_NAME, modelName)
            arguments.putParcelableArrayList(INSTRUCTION_VIDEO_URI, ArrayList(instructionalVideos))
            fragment.arguments = arguments

            return fragment
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            activity?.title = getString(R.string.guides_list_title, (viewModel.modelName ?: ""))
        }
    }

    override fun hasRequiredArgumentData(): Boolean {
        val model = arguments?.getString(KEY_MODEL_NAME)

        viewModel.modelName = model ?: ""

        val instructionalVideos = arguments
            ?.getParcelableArrayList<VideoInfo>(INSTRUCTION_VIDEO_URI)

        viewModel.instructionalVideos = instructionalVideos ?: ArrayList()
        return model != null && instructionalVideos != null
    }

    @SuppressLint("StringFormatInvalid")
    override fun loadData() {
        val model = viewModel.modelName
        requireActivity().title = getString(R.string.instructional_videos_title, model)
    }

    @SuppressLint("StringFormatInvalid")
    override fun initUi(view: View) {
        mediaItem = MediaItem.fromUri(viewModel.instructionalVideos.first().url.toString())

        videoView = view.findViewById(R.id.videoView)
        player = context?.let { SimpleExoPlayer.Builder(it).build() }!!
        videoView.player = player
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        activity?.title = getString(
            R.string.instructional_videos_title, viewModel.modelName ?: ""
        )
    }
}


