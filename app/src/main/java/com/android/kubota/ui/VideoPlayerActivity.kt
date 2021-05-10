package com.android.kubota.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.kubota.R
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.inmotionsoftware.flowkit.android.put
import com.kubota.service.domain.VideoInfo

private const val INSTRUCTION_VIDEO_URI = "video_uri"

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        fun intent(context: Context, video: VideoInfo): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(INSTRUCTION_VIDEO_URI, video)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame_layout)

        val video = intent?.getParcelableExtra<VideoInfo>(INSTRUCTION_VIDEO_URI)
            ?: throw IllegalArgumentException("Missing $INSTRUCTION_VIDEO_URI")

        if (supportFragmentManager.findFragmentByTag("video") == null) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.frame_layout_container,
                    VideoPlayerFragment.createInstance(video),
                    "video"
                )
                .commit()
        }
    }
}


class VideoPlayerFragment : BaseFragment() {

    private lateinit var videoView: PlayerView
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaItem: MediaItem

    private val video: VideoInfo
        get() = arguments
            ?.getParcelable(INSTRUCTION_VIDEO_URI)
            ?: throw IllegalArgumentException("Missing $INSTRUCTION_VIDEO_URI")

    override val layoutResId: Int = R.layout.fragment_instruction_video_player

    companion object {
        fun createInstance(video: VideoInfo): VideoPlayerFragment {
            val fragment = VideoPlayerFragment()
            val arguments = Bundle()
            arguments.putParcelable(INSTRUCTION_VIDEO_URI, video)
            fragment.arguments = arguments

            return fragment
        }
    }

    override fun loadData() {

    }

    override fun initUi(view: View) {
        mediaItem = MediaItem.fromUri(video.url.toString())

        videoView = view.findViewById(R.id.videoView)
        videoView.setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
        player = SimpleExoPlayer.Builder(requireContext()).build()
        videoView.player = player
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        activity?.title = getString(R.string.instructional_videos_title)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.put("seekPosition", player.currentPosition)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getLong("seekPosition")?.let {
            player.seekTo(it)
        }
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }
}


