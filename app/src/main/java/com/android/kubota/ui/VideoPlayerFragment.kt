package com.android.kubota.ui


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.kubota.service.domain.VideoInfo


private const val KEY_MODEL_NAME = "model_name"
private const val INSTRUCTION_VIDEO_URI = "video_uri"

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        fun intent(context: Context, modelName: String, video: VideoInfo): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(KEY_MODEL_NAME, modelName)
                putExtra(INSTRUCTION_VIDEO_URI, video)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame_layout)

        val video = intent?.getParcelableExtra<VideoInfo>(INSTRUCTION_VIDEO_URI)
            ?: throw IllegalArgumentException("Missing $INSTRUCTION_VIDEO_URI")

        val model = intent.getStringExtra(KEY_MODEL_NAME)
            ?: throw IllegalArgumentException("Missing $KEY_MODEL_NAME")

        if (supportFragmentManager.findFragmentByTag("video") == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.frame_layout_container, VideoPlayerFragment.createInstance(model, video), "video")
                .commit()
        }
    }
}


class VideoPlayerFragment : BaseFragment() {

    private lateinit var videoView: PlayerView
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem

    override val layoutResId: Int = R.layout.fragment_instruction_video_player

    private val viewModel: VideoPlayerViewModel by viewModels()

    class VideoPlayerViewModel : ViewModel() {
        lateinit var modelName: String
        lateinit var instructionalVideos: VideoInfo

    }

    companion object {
        fun createInstance(
            modelName: String,
            video: VideoInfo
        ): VideoPlayerFragment {
            val fragment = VideoPlayerFragment()
            val arguments = Bundle(1)
            arguments.putString(KEY_MODEL_NAME, modelName)
            arguments.putParcelable(INSTRUCTION_VIDEO_URI, video)
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

        viewModel.modelName = model ?: throw IllegalArgumentException("Missing $KEY_MODEL_NAME")

        val instructionalVideos = arguments
            ?.getParcelable<VideoInfo>(INSTRUCTION_VIDEO_URI)
            ?: throw IllegalArgumentException("Missing $INSTRUCTION_VIDEO_URI")

        viewModel.instructionalVideos = instructionalVideos
        return true
    }

    @SuppressLint("StringFormatInvalid")
    override fun loadData() {
        val model = viewModel.modelName
        requireActivity().title = getString(R.string.instructional_videos_title, model)
    }

    @SuppressLint("StringFormatInvalid")
    override fun initUi(view: View) {
        mediaItem = MediaItem.fromUri(viewModel.instructionalVideos.url.toString())

        videoView = view.findViewById(R.id.videoView)
        player = SimpleExoPlayer.Builder(requireContext()).build()
        videoView.player = player
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        activity?.title = getString(
            R.string.instructional_videos_title, viewModel.modelName ?: ""
        )
    }
}


