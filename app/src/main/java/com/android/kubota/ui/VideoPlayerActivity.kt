package com.android.kubota.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.kubota.R
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.inmotionsoftware.flowkit.android.put
import com.kubota.service.domain.VideoInfo
import java.io.File

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
    private lateinit var player: SimpleExoPlayer

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
        val mediaSource = ProgressiveMediaSource
            .Factory(LocalCacheDataSourceFactory(requireContext().applicationContext))
            .createMediaSource(MediaItem.fromUri(video.url.toString()))

        val videoView: PlayerView = view.findViewById(R.id.videoView)
        videoView.setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
        player = SimpleExoPlayer.Builder(requireContext()).build()
        videoView.player = player
        player.setMediaSource(mediaSource)
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

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }
}

class LocalCacheDataSourceFactory(context: Context) : DataSource.Factory {

    private companion object {
        private const val maxSize = 200 * 1024 * 1024L;  // 200MB
        lateinit var applicationContent: Context
        val simpleCache: SimpleCache by lazy {
            SimpleCache(
                File(applicationContent.cacheDir, "media"),
                LeastRecentlyUsedCacheEvictor(maxSize),
                ExoDatabaseProvider(applicationContent)
            )
        }
    }

    init {
        applicationContent = context
    }

    private val cacheDataSink: CacheDataSink = CacheDataSink(simpleCache, maxSize)
    private val fileDataSource: FileDataSource = FileDataSource()
    private val defaultDataSourceFactory: DefaultDataSourceFactory = DefaultDataSourceFactory(
        applicationContent,
        DefaultBandwidthMeter.Builder(context).build(),
        DefaultHttpDataSource.Factory()
    )

    override fun createDataSource(): DataSource {
        return CacheDataSource(
            simpleCache, defaultDataSourceFactory.createDataSource(),
            fileDataSource, cacheDataSink,
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        )
    }
}
