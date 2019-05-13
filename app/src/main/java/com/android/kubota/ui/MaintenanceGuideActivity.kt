package com.android.kubota.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.android.kubota.R
import com.kubota.repository.prefs.GuidePage
import com.kubota.repository.prefs.GuidesRepo
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*

private const val KEY_MODEL_NAME = "model_name"
private const val KEY_GUIDE_ITEM = "guide_item"

class MaintenanceGuideActivity: AppCompatActivity() {

    companion object {
        fun launchMaintenanceGuideActivity(context: Context, model: String, guideItem: String) {
            val intent = Intent(context, MaintenanceGuideActivity::class.java)
            intent.putExtra(KEY_MODEL_NAME, model)
            intent.putExtra(KEY_GUIDE_ITEM, guideItem)

            context.startActivity(intent)
        }
    }

    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)
    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)

    private var isPaused = false
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomViewGroup: View
    private lateinit var stepCounter: TextView
    private lateinit var viewPager: ViewPager
    private lateinit var audioStartStopButton: ImageView
    private lateinit var audioTitle: TextView
    private lateinit var next: TextView
    private lateinit var back: TextView
    private lateinit var guide: String

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maintenance_guide)

        setSupportActionBar(findViewById(R.id.toolbar))
        stepCounter = findViewById(R.id.step)
        viewPager = findViewById(R.id.viewPager)
        audioStartStopButton = findViewById(R.id.audioAction)
        audioTitle = findViewById(R.id.audioTitle)
        next = findViewById(R.id.next)
        back = findViewById(R.id.back)
        bottomViewGroup = findViewById(R.id.bottomViewGroup)
        progressBar = findViewById(R.id.toolbarProgressBar)

        val model = intent.getStringExtra(KEY_MODEL_NAME)
        val guideItem = intent.getStringExtra(KEY_GUIDE_ITEM)

        if (model != null && guideItem != null) {
            guide = guideItem
            title = guide
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            loadGuides(model)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (item.itemId){
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        if (isPaused) {
            isPaused = false
            val adapter = viewPager.adapter

            if (adapter is PagerAdapter) {
                adapter.getMp3Path(viewPager.currentItem)?.let {mp3Path ->
                    initMediaPlayer(mp3Path)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        isPaused = true
        mediaPlayer?.pause()
        mediaPlayer?.release()
        mediaPlayer = null
        audioStartStopButton.setImageResource(R.drawable.ic_guides_play_40dp)
    }

    private fun loadGuides(model: String) {
        val repo = GuidesRepo(model)
        backgroundScope.launch {
            when (val result = repo.getGuideList()) {
                is GuidesRepo.Response.Success ->
                    loadPages(model, repo.getGuidePages(index = result.data.indexOf(guide), guideList = result.data))
                is GuidesRepo.Response.Failure -> showServerErrorSnackbar()
            }

        }
    }

    private suspend fun loadPages(model: String, pages: GuidesRepo.Response<List<GuidePage>?>) {
        when (pages) {
            is GuidesRepo.Response.Success -> {
                withContext(uiScope.coroutineContext) {
                    pages.data?.let { pages ->
                        viewPager.adapter = PagerAdapter(model, guide, pages, supportFragmentManager)
                        prepareUI(pages)
                        bottomViewGroup.visibility = View.VISIBLE
                        progressBar.visibility = View.INVISIBLE
                    }
                }
            }
            is GuidesRepo.Response.Failure -> showServerErrorSnackbar()
        }
    }

    private fun showServerErrorSnackbar() {
        Snackbar.make(viewPager, getString(R.string.server_error_message), Snackbar.LENGTH_INDEFINITE).apply {
            setAction(getString(R.string.dismiss)) {}
        }
    }

    private fun prepareUI(pages: List<GuidePage>) {
        onPositionChanged(0, pages)

        next.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }

        back.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    audioStartStopButton.setImageResource(R.drawable.ic_guides_play_40dp)
                }
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {

            }

            override fun onPageSelected(p0: Int) {
                onPositionChanged(p0, pages)
            }

        })

        audioStartStopButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true){
                mediaPlayer?.pause()
                audioStartStopButton.setImageResource(R.drawable.ic_guides_play_40dp)
            } else if (mediaPlayer != null && mediaPlayer?.isPlaying == false){
                mediaPlayer?.start()
                audioStartStopButton.setImageResource(R.drawable.ic_guides_pause_28dp)
            }
        }
    }

    private fun onPositionChanged(position: Int, data: List<GuidePage>) {
        val adjustedCurrentPage = position + 1
        back.visibility = if (adjustedCurrentPage != 1) View.VISIBLE else View.GONE
        next.visibility = if (adjustedCurrentPage < data.size) View.VISIBLE else View.GONE
        stepCounter.text = getString(R.string.guides_step_fmt, adjustedCurrentPage, data.size)
        audioTitle.text = getString(R.string.guides_audio_track_fmt, adjustedCurrentPage, guide)

        initMediaPlayer(data[position].mp3Path)
    }

    private fun initMediaPlayer(mp3Path: String) {
        mediaPlayer = null
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(mp3Path)
            prepareAsync()
            setOnCompletionListener {
                audioStartStopButton.setImageResource(R.drawable.ic_guides_play_40dp)
            }
        }
    }

}

private class PagerAdapter(private val model: String, private val guideName: String, private val data: List<GuidePage>, fm: FragmentManager): FragmentPagerAdapter(fm) {

    override fun getItem(p0: Int): Fragment {
        return GuidesPageFragment.createInstance(UIGuidePage(p0 + 1, model, guideName, data[p0].textPath, data[p0].imagePath))
    }

    override fun getCount(): Int {
        return data.size
    }

    fun getMp3Path(position: Int): String? {
        if (position < 0 || position > data.size) return null

        return data[position].mp3Path
    }
}

data class UIGuidePage(val pageNumber: Int, val model:String, val guideName: String, val textUrl: String, val imageUrl: String): Parcelable {

    constructor(parcel: Parcel) : this(
        pageNumber = parcel.readInt(),
        model = parcel.readString(),
        guideName = parcel.readString(),
        textUrl = parcel.readString(),
        imageUrl = parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(pageNumber)
        parcel.writeString(model)
        parcel.writeString(guideName)
        parcel.writeString(textUrl)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UIGuidePage> {
        override fun createFromParcel(parcel: Parcel): UIGuidePage {
            return UIGuidePage(parcel)
        }

        override fun newArray(size: Int): Array<UIGuidePage?> {
            return arrayOfNulls(size)
        }
    }
}

class GuidesPageFragment: Fragment() {

    private val uiJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + uiJob)
    private val backgroundJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + backgroundJob)

    private lateinit var title: TextView
    private lateinit var step: TextView
    private lateinit var instructions: TextView
    private lateinit var image: ImageView

    companion object {
        private const val KEY_GUIDE_PAGE = "guide_page"

        fun createInstance(guidePage: UIGuidePage): GuidesPageFragment {
            return GuidesPageFragment().apply {
                val args = Bundle(1)
                args.putParcelable(KEY_GUIDE_PAGE, guidePage)

                this.arguments = args
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_guides_page, null)
        title = view.findViewById(R.id.title)
        step = view.findViewById(R.id.step)
        instructions = view.findViewById(R.id.instruction)
        image = view.findViewById(R.id.stepImage)

        arguments?.getParcelable<UIGuidePage>(KEY_GUIDE_PAGE)?.let {
            title.text = it.guideName
            step.text = getString(R.string.guides_page_step_fmt, it.pageNumber)

            val repo = GuidesRepo(it.model)

            Picasso.with(requireContext())
                .load(it.imageUrl)
                .into(image)

            backgroundScope.launch {
                val text = repo.getGuidePageWording(it.textUrl)

                withContext(uiScope.coroutineContext) {
                    instructions.text = text
                }
            }
        }

        return view
    }

}
