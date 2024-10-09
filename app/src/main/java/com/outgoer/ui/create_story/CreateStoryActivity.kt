package com.outgoer.ui.create_story

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahmedadeltito.photoeditorsdk.BrushDrawingView
import com.ahmedadeltito.photoeditorsdk.OnPhotoEditorSDKListener
import com.ahmedadeltito.photoeditorsdk.PhotoEditorSDK
import com.ahmedadeltito.photoeditorsdk.PhotoEditorSDK.PhotoEditorSDKBuilder
import com.ahmedadeltito.photoeditorsdk.ViewType
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityCreateStoryBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.service.StoryUploadingService
import com.outgoer.ui.create_story.model.SelectedMedia
import com.outgoer.ui.create_story.view.ColorPickerAdapter
import com.outgoer.ui.create_story.view.StoryMediaAdapter
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.music.AddMusicActivity
import com.outgoer.ui.music.view.AudioWaveAdapter
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.utils.Utility.generateRandomString
import com.outgoer.utils.Utility.getConvertedFile
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.pow


class CreateStoryActivity : BaseActivity(), OnPhotoEditorSDKListener {

    private lateinit var binding: ActivityCreateStoryBinding
    private var listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
    private lateinit var storyMediaAdapter: StoryMediaAdapter
    private var videoPath: String = ""
    private var countdownTimer: CountDownTimer? = null
    private var countDownTimer: CountDownTimer? = null
    lateinit var videoPlayer: SimpleExoPlayer
    private var downloadId: Long? = null
    private lateinit var audioPlayer: SimpleExoPlayer
    private var fileName: String = ""
    private var handler: Handler? = null
    var startPosition: Int = 0
    var endPosition: Int = 0
    private var videoDuration: Int = 0
    private var mp3Uri: File? = null
    private var counter: Int = 0
    private var singleItem = false
    private var type = CreateMediaType.story.name
    private var colorCodeTextView = -1
    private var colorPickerColors: ArrayList<Int> = arrayListOf()
    private var photoEditorSDK: PhotoEditorSDK? = null
    private var isSaved: Boolean = false

    companion object {
        const val LIST_OF_MEDIA = "LIST_OF_MEDIA"
        const val TYPE = "TYPE"
        private const val OLD_VIDEO_PATH = "OLD_VIDEO_PATH"
        private const val NEW_VIDEO_PATH = "NEW_VIDEO_PATH"
        const val VIDEO_MAP_INFO = "VIDEO_MAP_INFO"
        const val GOOGLE_MAP_INFO = "GOOGLE_MAP_INFO"
        private var LOCATION_GET = 1997
        private var ADD_MUSIC = 1998

        fun getIntent(context: Context, listOfMedia: ArrayList<String>, postType: String): Intent {
            val intent = Intent(context, CreateStoryActivity::class.java)
            intent.putExtra(LIST_OF_MEDIA, listOfMedia)
            intent.putExtra(TYPE, postType)
            return intent
        }

        fun launchData(context: Context, oldVideoPath: String, newVideoPath: String): Intent {
            val intent = Intent(context, CreateStoryActivity::class.java)
            intent.putExtra(OLD_VIDEO_PATH, oldVideoPath)
            intent.putExtra(NEW_VIDEO_PATH, newVideoPath)

            return intent
        }
    }

    private fun getVideoDuration(videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val durationString: String =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toString()
        retriever.release()
        return durationString.toLong()
    }

    private fun getFormattedVideoDuration(videoPath: String?): String? {
        val videoDuration = getVideoDuration(videoPath)
        var seconds = videoDuration / 1000
        binding.seekbar.max = videoDuration.toInt()

        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds %= 60
        return java.lang.String.format(
            Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds
        )
    }

    private fun initPlayer() {
        binding.playerView.player?.release()
        if(::videoPlayer.isInitialized) {
            videoPlayer.release()
        }
        try {
            videoPlayer = SimpleExoPlayer.Builder(this).build()
            audioPlayer = SimpleExoPlayer.Builder(this).build()
            binding.playerView.player = videoPlayer
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            videoPlayer.setAudioAttributes(audioAttributes, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, "Outgoer")
            val mediaSource: MediaSource =
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                    MediaItem.fromUri(mUri)
                )
            videoPlayer.setMediaSource(mediaSource)
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
            videoPlayer.addListener(object : Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    Timber.tag("VideoPreviewActivity").i("playbackState : $playbackState")
                    if (playbackState == ExoPlayer.STATE_ENDED) {
                        if (this@CreateStoryActivity::audioPlayer.isInitialized) {
                            binding.buttonPlay.isVisible = true
                            audioPlayer.pause()
                            play()
                        }
                    }
                }
            })
            binding.buttonPlay.isVisible = false
        } catch (e: java.lang.Exception) {
            Timber.tag("VideoPreviewActivity").i("Error : $e")
        }
    }

    private fun initPhotoEditor() {
        photoEditorSDK = PhotoEditorSDKBuilder(this@CreateStoryActivity)
            .parentView(binding.parentImageRelativeLayout) // add parent image view
            .childView(binding.storyImageRoundedImageView) // add the desired image view
            .deleteView(binding.deleteRl) // add the deleted view that will appear during the movement of the views
            .brushDrawingView(binding.drawingView)
            .buildPhotoEditorSDK() // build photo editor sdk

        photoEditorSDK?.setOnPhotoEditorSDKListener(this@CreateStoryActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("testing", "onCreate CreateStoryActivity")
        val dir = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
        val outgoerDir = File(dir, BaseConstants.TEXT_IMAGE_FOLDER_NAME)
        if (outgoerDir.isDirectory && outgoerDir.exists()) {
            for(fileInfo in outgoerDir.listFiles()!!) {
                fileInfo.delete()
            }
        }

        initPhotoEditor()

        colorPickerColors.add(resources.getColor(R.color.white, null))
        colorPickerColors.add(resources.getColor(R.color.black, null))
        colorPickerColors.add(resources.getColor(R.color.blue_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.green_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.yellow_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.orange_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.pink_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.strawbary_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.purple_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.red_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.light_pink_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.pich_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.light_yellow_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.light_yellow_1_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.occur_color_picker, null))
        colorPickerColors.add(resources.getColor(R.color.light_brown_picker, null))
        colorPickerColors.add(resources.getColor(R.color.brown_picker, null))
        colorPickerColors.add(resources.getColor(R.color.dark_green_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_1_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_2_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_3_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_4_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_5_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_6_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_7_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_8_picker, null))
        colorPickerColors.add(resources.getColor(R.color.grey_91_picker, null))
        initPlayer()
        initUI()
    }

    private fun openAddTextPopupWindow(text: String, colorCode: Int) {
        colorCodeTextView = colorCode
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val addTextPopupWindowRootView: View =
            inflater.inflate(R.layout.add_text_popup_window, null)
        val addTextEditText =
            addTextPopupWindowRootView.findViewById<View>(R.id.add_text_edit_text) as EditText
        val addTextDoneTextView =
            addTextPopupWindowRootView.findViewById<View>(R.id.add_text_done_tv) as TextView
        val addTextColorPickerRecyclerView =
            addTextPopupWindowRootView.findViewById<View>(R.id.add_text_color_picker_recycler_view) as RecyclerView
        val layoutManager =
            LinearLayoutManager(this@CreateStoryActivity, LinearLayoutManager.HORIZONTAL, false)
        addTextColorPickerRecyclerView.layoutManager = layoutManager
        addTextColorPickerRecyclerView.setHasFixedSize(true)
        val colorPickerAdapter = ColorPickerAdapter(this@CreateStoryActivity, colorPickerColors)
        colorPickerAdapter.setOnColorPickerClickListener(object :
            ColorPickerAdapter.OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                addTextEditText.setTextColor(colorCode)
                colorCodeTextView = colorCode
            }
        })
        addTextColorPickerRecyclerView.adapter = colorPickerAdapter
        if (stringIsNotEmpty(text)) {
            addTextEditText.setText(text)
            addTextEditText.setTextColor(if (colorCode == -1) resources.getColor(R.color.white) else colorCode)
        }
        val pop = PopupWindow(this@CreateStoryActivity)
        pop.contentView = addTextPopupWindowRootView
        pop.width = LinearLayout.LayoutParams.MATCH_PARENT
        pop.height = LinearLayout.LayoutParams.MATCH_PARENT
        pop.isFocusable = true
        pop.setBackgroundDrawable(null)
        pop.showAtLocation(addTextPopupWindowRootView, Gravity.TOP, 0, 0)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        addTextDoneTextView.setOnClickListener { view ->
            addText(addTextEditText.text.toString(), colorCodeTextView)
            val inputMethodService = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodService.hideSoftInputFromWindow(view.windowToken, 0)
            pop.dismiss()
        }
    }

    private fun addText(text: String, colorCodeTextView: Int) {
        Timber.tag("PhotoEditorSDK").d("addText() -> text: $text")
        photoEditorSDK?.addText(text, colorCodeTextView)
    }


    private fun stringIsNotEmpty(string: String?): Boolean {
        if (string != null && string != "null") {
            if (string.trim { it <= ' ' } != "") {
                return true
            }
        }
        return false
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initUI() {
        intent?.let {
            val list = it.getStringArrayListExtra(LIST_OF_MEDIA)
            list?.forEach { item ->
                listOfSelectedFiles.add(SelectedMedia(item, false))
            }
            type = it.getStringExtra(TYPE) ?: CreateMediaType.story.name
            if (type == CreateMediaType.post.name) {
                binding.tvUsername.text = resources.getString(R.string.post_preview)
            }
            singleItem = listOfSelectedFiles.size == 1

            println("Is video: " + listOfSelectedFiles[counter].isVideo())
            if (listOfSelectedFiles[counter].isVideo()) {
                binding.storyImageRoundedImageView.visibility = View.GONE
                binding.parentImageRelativeLayout.visibility = View.GONE
                binding.addMusicAppCompatImageView.visibility = View.VISIBLE
                binding.playerView.visibility = View.VISIBLE

                list?.first()?.let { videoPath ->
                    this.videoPath = videoPath
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val width =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.let { it1 -> Integer.valueOf(it1) }
                    val height =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.let { it1 -> Integer.valueOf(it1) }
                    retriever.release()
                    if (width != null && height != null) {
                        binding.playerView.resizeMode = if (width > height) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                    }
                }

                val duration = getFormattedVideoDuration(videoPath)
                binding.timerAppCompatTextView.text = duration
                binding.seekBarTimer.text = duration
                val secondDuration = getVideoDuration(videoPath) / 1000
                videoDuration = getVideoDuration(videoPath).toInt()
                binding.durationTxt.text = secondDuration.toString()
                buildMediaSource(Uri.parse(videoPath))
                binding.videoControlsLayout.visibility = View.GONE
                binding.ivText.isVisible = false


            } else {
                binding.parentImageRelativeLayout.visibility = View.VISIBLE
                binding.storyImageRoundedImageView.visibility = View.VISIBLE
                binding.addMusicAppCompatImageView.visibility = View.VISIBLE
                binding.playerView.visibility = View.GONE

                binding.buttonPlay.visibility = View.GONE
                binding.ivText.isVisible = type == CreateMediaType.story.name || type == CreateMediaType.story_video.name
                if (type == CreateMediaType.post.name) {
                    binding.locationAppCompatImageView.isVisible = false
                }

                videoDuration = 15000
                binding.durationTxt.text = "15"

                Glide.with(this@CreateStoryActivity).load(File(list?.first()))
                    .placeholder(R.drawable.venue_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.storyImageRoundedImageView)
            }
        }


        binding.ivText.throttleClicks().subscribeAndObserveOnMainThread {
            openAddTextPopupWindow("", -1)
        }.autoDispose()

        storyMediaAdapter = StoryMediaAdapter(this@CreateStoryActivity).apply {
            storySelectionAction.subscribeAndObserveOnMainThread {
                try {
                    photoEditorSDK?.clearAllViews()
                } catch (e: Exception) {
                    Timber.e(e, "Error clearing all views")
                }
                audioPlayer.pause()
                videoPlayer.pause()
                val index = listOfSelectedFiles.indexOf(it)
                Timber.tag("PhotoEditorSDK").d("storySelectionAction -> before counter: $counter && index: $index")

                counter = index
                Timber.tag("PhotoEditorSDK").d("storySelectionAction -> after counter: $counter")
                listOfSelectedFiles[index].isSelected = true
                storyMediaAdapter.listOfFilePath = listOfSelectedFiles


                if (listOfSelectedFiles[index].location != null || listOfSelectedFiles[index].googleLocation != null) {
                    binding.locationAppCompatImageView.visibility = View.GONE
                    binding.locationLinearLayout.visibility = View.VISIBLE
                    binding.locationAppCompatTextView.text =
                        if (listOfSelectedFiles[index].location != null) listOfSelectedFiles[index].location?.name
                            ?: "" else listOfSelectedFiles[index].googleLocation?.name ?: ""
                } else {
                    binding.locationAppCompatImageView.visibility = View.VISIBLE
                    binding.locationLinearLayout.visibility = View.GONE
                }

                binding.profileRelativeLayout.visibility =
                    if (listOfSelectedFiles[index].musicResponse != null) View.VISIBLE else View.GONE

                if (it.isVideo()) {

                    binding.parentImageRelativeLayout.visibility = View.GONE
                    binding.storyImageRoundedImageView.visibility = View.GONE
                    binding.playerView.visibility = View.VISIBLE
                    binding.addMusicAppCompatImageView.visibility = View.VISIBLE

                    binding.storyImageRoundedImageView.visibility = View.GONE
                    binding.playerView.visibility = View.VISIBLE

                    it.filePath.let { video ->
                        videoPath = video
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(videoPath)
                        val width =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                ?.let { it1 -> Integer.valueOf(it1) }
                        val height =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                ?.let { it1 -> Integer.valueOf(it1) }
                        retriever.release()

                        Timber.tag("CreateStoryActivity").i("Width: $width")
                        Timber.tag("CreateStoryActivity").i("height: $height")
                        Timber.tag("CreateStoryActivity").i("videoPath: $videoPath")

                        if (width != null && height != null) {
                            binding.playerView.resizeMode = if (width > height) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FILL
                            }
                        }
                    }

                    val duration = getFormattedVideoDuration(videoPath)
                    binding.timerAppCompatTextView.text = duration
                    binding.seekBarTimer.text = duration
                    val secondDuration = getVideoDuration(videoPath) / 1000
                    videoDuration = getVideoDuration(videoPath).toInt()
                    binding.durationTxt.text = secondDuration.toString()
                    buildMediaSource(Uri.parse(videoPath))
                    binding.videoControlsLayout.visibility = View.GONE
                    if (it.location != null) {
                        binding.locationLinearLayout.visibility = View.VISIBLE
                        binding.locationAppCompatTextView.text = it.location?.name
                    } else {
                        binding.locationLinearLayout.visibility = View.GONE
                    }
                    if (it.musicResponse != null) {
                        Glide.with(this@CreateStoryActivity).load(it.musicResponse?.songImage)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(binding.ivProfile)
                        binding.musicTitleAppCompatTextView.text = it.musicResponse?.songTitle
                        binding.singerNameAppCompatTextView.text = it.musicResponse?.songSubtitle
                        Glide.with(this@CreateStoryActivity).load(it.musicResponse?.songImage)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(binding.ivProfileTop)
                        binding.musicTitleTopAppCompatTextView.text = it.musicResponse?.songTitle
                        binding.singerNameTopAppCompatTextView.text = it.musicResponse?.songSubtitle
                        if (!it.musicFileName.isNullOrEmpty()) {
                            binding.videoControlsLayout.isVisible = false
                            binding.profileRelativeLayout.isVisible = false
                            binding.llOption.isVisible = false
                            binding.timerAppCompatTextView.isVisible = false
                            binding.buttonPlay.isVisible = false
                            binding.musicInfo.isVisible = true
                            binding.btnDone.isVisible = true
                            binding.rangeFrameView.isVisible = true
                            binding.locationLinearLayout.isVisible = false
                            binding.profileTopRelativeLayout.isVisible = true
                            setMusic(it.musicFileName)
                        } else {
                            binding.llOption.isVisible = true
                            binding.btnDone.isVisible = false
                            binding.profileTopRelativeLayout.isVisible = false
                            binding.musicInfo.isVisible = false
                            binding.rangeFrameView.isVisible = false

                        }
                        if (!it.trimMusicFileName.isNullOrEmpty()) {
                            binding.profileRelativeLayout.isVisible = true
                            binding.profileTopRelativeLayout.isVisible = false
                            binding.llOption.isVisible = true
                            binding.btnDone.isVisible = false
                            binding.addMusicAppCompatImageView.isVisible = false
                            binding.timerAppCompatTextView.isVisible = false
                            binding.ivDelete.isVisible = true
                            binding.musicInfo.isVisible = false
                            binding.rangeFrameView.isVisible = false
                            videoPlayer.playWhenReady = false
                            listOfSelectedFiles[counter].durationSet = false
                            setMusic(listOfSelectedFiles[counter].trimMusicFileName)
                        }
                    } else {
                        binding.llOption.isVisible = true
                        binding.btnDone.isVisible = false
                        binding.profileTopRelativeLayout.isVisible = false
                        binding.profileRelativeLayout.isVisible = false
                        binding.musicInfo.isVisible = false
                        binding.rangeFrameView.isVisible = false
                        binding.addMusicAppCompatImageView.isVisible = true
                    }
                    if (type == CreateMediaType.post.name) {
                        binding.locationAppCompatImageView.isVisible = false
                        binding.ivText.isVisible = false
                    }
                } else {
                    binding.parentImageRelativeLayout.visibility = View.VISIBLE
                    binding.storyImageRoundedImageView.visibility = View.VISIBLE
                    binding.playerView.visibility = View.GONE
                    binding.addMusicAppCompatImageView.visibility = View.VISIBLE

                    Glide.with(this@CreateStoryActivity)
                        .load(File(it.filePath))
                        .apply(RequestOptions().placeholder(R.drawable.venue_placeholder).diskCacheStrategy(DiskCacheStrategy.NONE))
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Timber.tag("PhotoEditorSDK").i("Image loading failed")
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Timber.tag("PhotoEditorSDK").i("Image loading successfully && isSaved: $isSaved")
                                if (isSaved) {
                                    isSaved = false
                                    Timber.tag("PhotoEditorSDK").i("isSaved true")

                                    if (photoEditorSDK == null) {
                                        Timber.tag("PhotoEditorSDK").e("photoEditorSDK is null")
                                        initPhotoEditor()
                                    }

                                    runOnUiThread {
                                        Timber.tag("PhotoEditorSDK").d("photoEditorSDK runOnUiThread")
                                        try {
                                            photoEditorSDK?.clearAllViews()
                                        } catch (e: Exception) {
                                            Timber.e(e, "Error clearing all views")
                                        }
                                    }
                                }
                                return false
                            }
                        })
                        .into(binding.storyImageRoundedImageView)

                    videoDuration = 15000
                    binding.durationTxt.text = "15"


                    binding.buttonPlay.isVisible = false
                    binding.videoControlsLayout.visibility = View.GONE
                    binding.ivText.isVisible = type == CreateMediaType.story.name || type == CreateMediaType.story_video.name
                    if (type == CreateMediaType.post.name) {
                        binding.locationAppCompatImageView.isVisible = false
                    }
                    if (it.musicResponse != null) {
                        binding.btnAdd.background =
                            resources.getDrawable(R.drawable.bg_grey_border, null)
                        Glide.with(this@CreateStoryActivity).load(it.musicResponse?.songImage)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(binding.ivProfile)
                        binding.musicTitleAppCompatTextView.text = it.musicResponse?.songTitle
                        binding.singerNameAppCompatTextView.text = it.musicResponse?.songSubtitle
                        Glide.with(this@CreateStoryActivity).load(it.musicResponse?.songImage)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(binding.ivProfileTop)
                        binding.musicTitleTopAppCompatTextView.text = it.musicResponse?.songTitle
                        binding.singerNameTopAppCompatTextView.text = it.musicResponse?.songSubtitle
                        if (!it.musicFileName.isNullOrEmpty()) {
                            binding.videoControlsLayout.isVisible = false
                            binding.profileRelativeLayout.isVisible = false
                            binding.llOption.isVisible = false
                            binding.timerAppCompatTextView.isVisible = false
                            binding.buttonPlay.isVisible = false
                            binding.musicInfo.isVisible = true
                            binding.btnDone.isVisible = true
                            binding.rangeFrameView.isVisible = true
                            binding.locationLinearLayout.isVisible = false
                            binding.profileTopRelativeLayout.isVisible = true
                            setMusic(it.musicFileName)
                        } else {
                            binding.llOption.isVisible = true
                            binding.btnDone.isVisible = false
                            binding.profileTopRelativeLayout.isVisible = false
                            binding.musicInfo.isVisible = false
                            binding.rangeFrameView.isVisible = false
                            binding.addMusicAppCompatImageView.isVisible = true
                        }
                        if (!it.trimMusicFileName.isNullOrEmpty()) {
                            binding.profileRelativeLayout.isVisible = true
                            binding.profileTopRelativeLayout.isVisible = false
                            binding.llOption.isVisible = true
                            binding.btnDone.isVisible = false
                            binding.addMusicAppCompatImageView.isVisible = false
                            binding.timerAppCompatTextView.isVisible = false
                            binding.ivDelete.isVisible = true
                            binding.musicInfo.isVisible = false
                            binding.rangeFrameView.isVisible = false
                            videoPlayer.playWhenReady = false
                            listOfSelectedFiles[counter].durationSet = false
                            setMusic(listOfSelectedFiles[counter].trimMusicFileName)
                        }
                    } else {
                        binding.btnAdd.background =
                            resources.getDrawable(R.drawable.bg_purple_border, null)
                        binding.llOption.isVisible = true
                        binding.btnDone.isVisible = false
                        binding.profileTopRelativeLayout.isVisible = false
                        binding.profileRelativeLayout.isVisible = false
                        binding.musicInfo.isVisible = false
                        binding.rangeFrameView.isVisible = false
                        binding.addMusicAppCompatImageView.isVisible = true
                    }
                }
            }
        }

        binding.itemListRecyclerView.apply {
            adapter = storyMediaAdapter
            layoutManager =
                LinearLayoutManager(this@CreateStoryActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        storyMediaAdapter.listOfFilePath = listOfSelectedFiles

        binding.profileRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            audioPlayer.pause()
            videoPlayer.pause()

            if(binding.profileRelativeLayout.isVisible) {
                startActivityForResult(
                    AddMusicActivity.getIntent(
                        this@CreateStoryActivity, CreateMediaType.story.name, videoPath
                    ), ADD_MUSIC
                )
            }
        }

        binding.itemListRecyclerView.visibility = if (singleItem) View.GONE else View.VISIBLE

        binding.buttonPlay.throttleClicks().subscribeAndObserveOnMainThread {
            videoPlayer.seekTo(0)
            audioPlayer.seekTo(0)
            videoPlayer.playWhenReady = true
            audioPlayer.playWhenReady = true
            binding.buttonPlay.isVisible = false
            countdownTimer?.start()
            binding.timerAppCompatTextView.isVisible = false
//            listOfSelectedFiles[counter].durationSet = false
        }.autoDispose()

        binding.ivPause.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoPlayer.isPlaying) {
                binding.ivPause.setImageResource(R.drawable.ic_play_reels)
                videoPlayer.pause()
            } else {
                binding.buttonPlay.isVisible = false

                binding.ivPause.setImageResource(R.drawable.ic_pause)
                videoPlayer.play()
                countdownTimer?.start()
            }
        }.autoDispose()

        binding.ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
            binding.ivDelete.isVisible = false
            binding.profileRelativeLayout.isVisible = false
            audioPlayer.pause()
            videoPlayer.pause()
            binding.addMusicAppCompatImageView.isVisible = true
            listOfSelectedFiles[counter].musicResponse = null
            listOfSelectedFiles[counter].isTrimMusic = false
            listOfSelectedFiles[counter].durationSet = false
        }.autoDispose()

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.audioWaveView.layoutManager = layoutManager

        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
                if (fromUser) {
                    videoPlayer.seekTo(0)
                    val itemCountLayout = layoutManager.itemCount
                    val scrollToPosition =
                        ((progress.toFloat() / 100) * (itemCountLayout - 1)).toInt()
                    layoutManager.scrollToPosition(scrollToPosition)

                    if (itemCountLayout > 0) {
                        val totalDuration = audioPlayer.duration
                        startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                        val audioPlayerLength = audioPlayer.currentPosition.toString().length
                        // Update audio playback position based on the scroll
                        val newIntVideoDuration = videoDuration / 1000
                        if (audioPlayerLength >= 3) {
                            startPosition -= calculateAdjustment(
                                audioPlayerLength, newIntVideoDuration
                            )
                        } else {
                            if (audioPlayerLength == 2) {
                                if (startPosition >= 20) {
                                    startPosition -= newIntVideoDuration
                                } else {
                                    startPosition
                                }
                            }
                        }
                        endPosition =
                            (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())
                        audioPlayer.seekTo(startPosition.toLong())
                        audioPlayer.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE

                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer.isPlaying) {
                                        val currentPos = audioPlayer.currentPosition
                                        if (currentPos >= endPosition) {
                                            audioPlayer.pause()
                                            binding.playButton.visibility = View.VISIBLE
                                            binding.pauseButton.visibility = View.INVISIBLE
                                        } else {
                                            handler?.postDelayed(
                                                this, 100
                                            ) // Check every 100 milliseconds
                                        }
                                    } else {
                                        // Handle the case where audioPlayer is not playing or in an invalid state
                                        // You might want to release or reset the audioPlayer in this case
                                    }
                                } catch (e: IllegalStateException) {
                                    // Handle IllegalStateException, log it, or take appropriate action
                                    Timber.e(e, "Error in handler postDelayed")
                                }
                            }
                        }, 100)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do nothing for now
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do nothing for now
            }
        })

        binding.audioWaveView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                videoPlayer.seekTo(0)
                val itemCountLayout = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (itemCountLayout > 0) {
                    val progress =
                        ceil((firstVisibleItem.toFloat() / (itemCountLayout - 1)) * 100).toInt()
                    Timber.tag("Progress").d("progress: %s", progress)

                    if (progress in 95..100) {
                        binding.audioSeekBar.progress = 100
                    } else {
                        binding.audioSeekBar.progress = progress
                    }

                    val totalDuration = audioPlayer.duration
                    startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                    val audioPlayerLength = audioPlayer.currentPosition.toString().length

                    val newIntVideoDuration = videoDuration / 1000
                    if (audioPlayerLength >= 3) {
                        startPosition -= calculateAdjustment(audioPlayerLength, newIntVideoDuration)
                    } else {
                        if (audioPlayerLength == 2) {
                            if (startPosition >= 20) {
                                startPosition -= newIntVideoDuration
                            } else {
                                startPosition
                            }
                        }
                    }
                    endPosition =
                        (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())

                    audioPlayer.seekTo(startPosition.toLong())
                    audioPlayer.play()
                    binding.pauseButton.visibility = View.VISIBLE
                    binding.playButton.visibility = View.GONE

                    handler?.postDelayed(object : Runnable {
                        override fun run() {
                            try {
                                // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                if (audioPlayer.isPlaying) {
                                    val currentPos = audioPlayer.currentPosition
                                    if (currentPos >= endPosition) {
                                        audioPlayer.pause()
                                        audioPlayer.seekTo(startPosition.toLong())
                                        audioPlayer.play()
                                    } else {
                                        handler?.postDelayed(
                                            this, 100
                                        )
                                    }
                                } else {
                                    // Handle the case where audioPlayer is not playing or in an invalid state
                                    // You might want to release or reset the audioPlayer in this case
                                }
                            } catch (e: IllegalStateException) {
                                // Handle IllegalStateException, log it, or take appropriate action
                                Timber.e(e, "Error in handler postDelayed")
                            }
                        }
                    }, 100)

                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        audioPlayer.seekTo(startPosition.toLong())
                        audioPlayer.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE
                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer.isPlaying) {
                                        val currentPos = audioPlayer.currentPosition
                                        if (currentPos >= endPosition) {
                                            audioPlayer.pause()
                                            audioPlayer.seekTo(startPosition.toLong())
                                            audioPlayer.play()
//                                            binding.playButton.visibility = View.VISIBLE
//                                            binding.pauseButton.visibility = View.INVISIBLE
                                        } else {
                                            handler?.postDelayed(
                                                this, 100
                                            ) // Check every 100 milliseconds
                                        }
                                    } else {
                                        // Handle the case where audioPlayer is not playing or in an invalid state
                                        // You might want to release or reset the audioPlayer in this case
                                    }
                                } catch (e: IllegalStateException) {
                                    // Handle IllegalStateException, log it, or take appropriate action
                                    Timber.e(e, "Error in handler postDelayed")
                                }
                            }
                        }, 100)
                    }
                }
            }
        })

        binding.btnAdd.throttleClicks().subscribeAndObserveOnMainThread {
            Timber.tag("PhotoEditorSDK").i("btnAdd clicked")
            Handler(Looper.getMainLooper()).postDelayed({
                if (!binding.profileTopRelativeLayout.isVisible) {
                    counter = 0
                    sendProcess()
                }
            }, 500)
        }.autoDispose()

        binding.playButton.setOnClickListener {
            pause()
            if (audioPlayer != null) {
                binding.buttonPlay.isVisible = false
                videoPlayer.seekTo(0)
                audioPlayer.seekTo(startPosition.toLong())
                audioPlayer.play()
                videoPlayer.play()
                handler?.postDelayed(object : Runnable {
                    override fun run() {
                        val currentPos = audioPlayer.currentPosition
                        if (currentPos >= endPosition) {
                            audioPlayer.pause()
                            play()
                        } else {
                            handler?.postDelayed(this, 100) // Check every 100 milliseconds
                        }
                    }
                }, 100)
            }
        }

        binding.pauseButton.setOnClickListener {
            play()
            audioPlayer.pause()
            videoPlayer.pause()
        }

        countdownTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (videoPlayer.isPlaying) {
                    binding.seekbar.progress = videoPlayer.currentPosition.toInt()
                }
            }

            override fun onFinish() {}
        }

        binding.ivSpeaker.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoPlayer.isDeviceMuted) {
                binding.ivSpeaker.setImageResource(R.drawable.ic_reel_unmute)
                videoPlayer.isDeviceMuted = false
            } else {
                binding.ivSpeaker.setImageResource(R.drawable.ic_reel_mute)
                videoPlayer.isDeviceMuted = true
            }
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress == seekBar?.max) {
                    countdownTimer?.cancel()
                    seekBar.progress = 0
                    binding.ivPause.setImageResource(R.drawable.ic_play_reels)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar?.progress == seekBar?.max) {
                    countdownTimer?.cancel()
                    seekBar?.progress = 0
                    binding.ivPause.setImageResource(R.drawable.ic_play_reels)
                } else {
                    if (seekBar != null) {
                        binding.ivPause.setImageResource(R.drawable.ic_pause)
                        videoPlayer.seekTo(seekBar.progress.toLong())
                        io.reactivex.Observable.timer(100, TimeUnit.MILLISECONDS)
                            .subscribeAndObserveOnMainThread {
                                videoPlayer.play()
                            }.autoDispose()
                    }
                }
            }
        })

        binding.locationAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityForResult(
                AddStoryLocationActivity.getIntent(this@CreateStoryActivity), LOCATION_GET
            )
        }.autoDispose()

        binding.addMusicAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if(binding.addMusicAppCompatImageView.isVisible) {
                startActivityForResult(
                    AddMusicActivity.getIntent(
                        this@CreateStoryActivity, CreateMediaType.story.name, videoPath
                    ), ADD_MUSIC
                )
            }
        }.autoDispose()

        binding.locationAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            @Suppress("DEPRECATION")
            startActivityForResult(
                AddStoryLocationActivity.getIntent(this@CreateStoryActivity), LOCATION_GET
            )
        }.autoDispose()

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.locationLinearLayout.visibility = View.GONE
            binding.locationAppCompatTextView.text = ""
            binding.locationAppCompatImageView.visibility = View.VISIBLE
        }.autoDispose()

        binding.editMusic.throttleClicks().subscribeAndObserveOnMainThread {
            audioPlayer.pause()
            videoPlayer.pause()
            if(binding.editMusic.isVisible) {
                startActivityForResult(
                    AddMusicActivity.getIntent(
                        this@CreateStoryActivity, CreateMediaType.story.name, videoPath
                    ), ADD_MUSIC
                )
            }
        }.autoDispose()

        binding.btnDone.throttleClicks().subscribeAndObserveOnMainThread {
            audioPlayer.pause()
            videoPlayer.pause()
            binding.progress.isVisible = true
            binding.btnDone.isVisible = false
            downloadTrimAudio()
        }.autoDispose()
    }

    private fun sendProcess() {
        binding.progress.isVisible = true
        if (counter < listOfSelectedFiles.size) {
            Timber.tag("PhotoEditorSDK").d(
                "sendProcess() -> " +
                        "if(counter < listOfSelectedFiles.size): ${counter < listOfSelectedFiles.size}"
            )
            if (listOfSelectedFiles[counter].isCheckVideo()) {
                if (!listOfSelectedFiles[counter].trimMusicFileName.isNullOrEmpty()) {
                    listOfSelectedFiles[counter].trimMusicFileName?.let {
                        mergeAudioVideo(
                            listOfSelectedFiles[counter].filePath, it
                        )
                    }
                } else {
                    counter++
                    sendProcess()
                }
            } else {
                if (!listOfSelectedFiles[counter].trimMusicFileName.isNullOrEmpty()) {
                    listOfSelectedFiles[counter].trimMusicFileName?.let {
                        mergeAudioImage(
                            listOfSelectedFiles[counter].filePath, it
                        )
                    }
                } else {
                    counter++
                    sendProcess()
                }
            }
        } else {
            Timber.tag("PhotoEditorSDK")
                .d("sendProcess() -> else (counter < listOfSelectedFiles.size): ${counter < listOfSelectedFiles.size}")
            if (type == CreateMediaType.post.name) {
                val intent = AddNewPostInfoActivity.getIntent(
                    this, postType = type, listOfSelectedFiles
                )
                startActivity(intent)
            } else {
                val intent = Intent(this, StoryUploadingService::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(StoryUploadingService.LIST_OF_STORY_DATA, listOfSelectedFiles)
                intent.putExtra(StoryUploadingService.LIST_OF_STORY_DATA, listOfSelectedFiles)
                startService(intent)
                startActivity(HomeActivity.getIntent(this@CreateStoryActivity))
            }
        }
    }

    private fun mergeAudioImage(imagePath: String, audioPath: String) {
        Timber.tag("CreateStoryActivity").i("mergeAudioImage -> position :$counter")
        Timber.tag("CreateStoryActivity").i("mergeAudioImage -> videoPath :$videoPath")
        Timber.tag("CreateStoryActivity").i("mergeAudioImage -> audioPath :$audioPath")
        val outputFileName = if (singleItem) {
            "merge_video_file.mp4"
        } else {
            "merge_video_file_$counter.mp4"
        }

        val mp4UriAfterTrim =
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
                getConvertedFile(
                    it.absolutePath, outputFileName
                )
            }

        val cmd = arrayOf(
            "-y",
            "-loop",
            "1",
            "-r",
            "1",
            "-i",
            imagePath,
            "-i",
            audioPath,
            "-acodec",
            "aac",
            "-vcodec",
            "mpeg4",
            "-strict",
            "experimental",
            "-b:a",
            "32k",
            "-shortest",
            "-f",
            "mp4",
            "-r",
            "2",
            mp4UriAfterTrim?.path
        )

        Thread {
            val result: Int = FFmpeg.execute(cmd)
            Timber.tag("VideoTrim").d("result: $result")
            when (result) {
                0 -> {
                    Timber.tag("CreateStoryActivity")
                        .i("mergeAudioImage: ${mp4UriAfterTrim?.path} Position :$counter")
                    listOfSelectedFiles[counter].mergeAudioImagePath = mp4UriAfterTrim?.path
                    listOfSelectedFiles[counter].isMergeAudioVideo = true
                    counter++
                    sendProcess()
                    Timber.tag("VideoTrim").i("result: Success")
                }
                255 -> {
                    Timber.tag("VideoTrim").d("result: Canceled")
                }
                else -> {
                    Timber.tag("VideoTrim").e("result: Failed")
                }
            }
        }.start()
    }

    private fun mergeAudioVideo(videoPath: String, audioPath: String) {
        Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> position :$counter")
        Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> videoPath :$videoPath")
        Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> audioPath :$audioPath")
        val outputFileName = if (singleItem) {
            "merge_video_file.mp4"
        } else {
            "merge_video_file_$counter.mp4"
        }

        val mp4UriAfterTrim =
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
                getConvertedFile(
                    it.absolutePath, outputFileName
                )
            }

        val cmd = arrayOf(
            "-y",
            "-i",
            videoPath,
            "-i",
            audioPath,
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-shortest",
            mp4UriAfterTrim?.path
        )

        Thread {
            val result: Int = FFmpeg.execute(cmd)
            Timber.tag("VideoTrim").d("result: $result")
            Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> result: $result")
            Timber.tag("CreateStoryActivity")
                .i("mergeAudioVideo -> mp4UriAfterTrim :$mp4UriAfterTrim")
            Timber.tag("CreateStoryActivity").i("mergeAudioVideo -> cmd :$cmd")
            Timber.tag("CreateStoryActivity")
                .i("mergeAudioAndVideo: ${mp4UriAfterTrim?.path} Position :$counter")
            when (result) {
                0 -> {
                    listOfSelectedFiles[counter].mergeAudioVideoPath = mp4UriAfterTrim?.path
                    listOfSelectedFiles[counter].isMergeAudioVideo = true
                    counter++
                    sendProcess()
                    Timber.tag("VideoTrim").i("result: Success")
                }
                255 -> {
                    Timber.tag("VideoTrim").d("result: Canceled")
                }
                else -> {
                    Timber.tag("VideoTrim").e("result: Failed")
                }
            }
        }.start()
    }

    private fun calculateAdjustment(length: Int, newIntVideoDuration: Int): Int {
        return newIntVideoDuration * 10.0.pow((length - 3).coerceAtLeast(0)).toInt()
    }

    private fun getFileNameFromUri(uriString: String): String {
        val uri = Uri.parse(uriString)
        val path = uri.lastPathSegment
        return path ?: ""
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == LOCATION_GET) {
            val info = data?.getParcelableExtra<VenueMapInfo>(VIDEO_MAP_INFO)
            listOfSelectedFiles[counter].location = info

            binding.locationAppCompatImageView.visibility = View.GONE
            if (info != null) {
                listOfSelectedFiles[counter].location = info

                info.name?.let {
                    binding.locationLinearLayout.visibility = View.VISIBLE
                    binding.locationAppCompatTextView.text = info.name
                }
            } else {
                val inform = data?.getParcelableExtra<GooglePlaces>(GOOGLE_MAP_INFO)
                listOfSelectedFiles[counter].googleLocation = inform

                inform?.name?.let {
                    binding.locationLinearLayout.visibility = View.VISIBLE
                    binding.locationAppCompatTextView.text = inform.name
                }
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == ADD_MUSIC) {
            val info = data?.getParcelableExtra<MusicResponse>("INTENT_ADD_MUSIC_INFO")
            if (info != null) {
                binding.btnAdd.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_grey_border, null)
                binding.btnAdd.isEnabled = false
                binding.itemListRecyclerView.isVisible = false
                if (!listOfSelectedFiles[counter].isCheckVideo()) {
                    Glide.with(this@CreateStoryActivity)
                        .load(File(listOfSelectedFiles[counter].filePath))
                        .placeholder(R.drawable.venue_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.storyImageRoundedImageView)
                }
                listOfSelectedFiles[counter].musicResponse = info
                fileName = getFileNameFromUri((info.songFile ?: ""))

                buildMediaSource(Uri.parse(videoPath))
                setUpAudio(info)
            }
        }
    }

    private fun pause() {
        binding.pauseButton.visibility = View.VISIBLE
        binding.playButton.visibility = View.GONE
    }

    private fun play() {
        binding.pauseButton.visibility = View.GONE
        binding.playButton.visibility = View.VISIBLE
    }

    private fun setUpAudio(info: MusicResponse?) {
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadAudio(info)
        videoPlayer.playWhenReady = false
        binding.videoControlsLayout.isVisible = false
        binding.profileRelativeLayout.isVisible = false
        binding.llOption.isVisible = false
        binding.timerAppCompatTextView.isVisible = false
        binding.buttonPlay.isVisible = false
        binding.musicInfo.isVisible = true
        binding.rangeFrameView.isVisible = true
        binding.locationLinearLayout.isVisible = false
        binding.profileTopRelativeLayout.isVisible = true
        binding.progressBar.visibility = View.VISIBLE
        binding.audioWaveView.visibility = View.GONE
        Glide.with(this).load(info?.songImage).diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.ivProfileTop)
        binding.musicTitleTopAppCompatTextView.text = info?.songTitle
        binding.singerNameTopAppCompatTextView.text = info?.songSubtitle
    }

    private fun setUpTrimAudio(info: MusicResponse?) {
        binding.profileRelativeLayout.isVisible = true
        binding.profileTopRelativeLayout.isVisible = false
        binding.llOption.isVisible = true
        binding.btnDone.isVisible = false
        binding.addMusicAppCompatImageView.isVisible = false
        binding.timerAppCompatTextView.isVisible = false
        binding.ivDelete.isVisible = true
        binding.musicInfo.isVisible = false
        binding.rangeFrameView.isVisible = false
        Glide.with(this).load(info?.songImage).diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.ivProfile)
        binding.musicTitleAppCompatTextView.text = info?.songTitle
        binding.singerNameAppCompatTextView.text = info?.songSubtitle
        binding.progress.isVisible = false
    }

    private fun setMusic(musicFileName: String?) {
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(musicFileName))
            audioPlayer.setMediaItem(mediaItem)
            audioPlayer.prepare()
            audioPlayer.addListener(object : Player.Listener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == ExoPlayer.STATE_READY) {
                        if (listOfSelectedFiles[counter].durationSet == false) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnDone.visibility = View.VISIBLE

                            if (!audioPlayer.isPlaying) {
                                val duration = audioPlayer.duration
                                val granularity = 1000
                                val itemCount = (duration / granularity).toInt()
                                val itemList = MutableList(itemCount) {}
                                binding.audioWaveView.adapter = AudioWaveAdapter(itemList)
                                audioPlayer.playWhenReady = true
                                videoPlayer.playWhenReady = true
                            }
                            listOfSelectedFiles[counter].durationSet = true
                        }
                    }
                }
            })
        } catch (e: java.lang.Exception) {
            Timber.tag("CreateStoryActivity").i("Error : $e")
        }
    }

    private fun downloadAudio(info: MusicResponse?) {
        val request = DownloadManager.Request(Uri.parse(info?.songFile))
        request.setTitle("Downloading $fileName")
        request.setDescription("Downloading $fileName")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val destinationUri = Uri.fromFile(
            File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer")), fileName
            )
        )
        request.setDestinationUri(destinationUri)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Timber.tag("CreateStoryActivity").i("downloadId :$downloadId")
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            Timber.tag("CreateStoryActivity").i("id :$id")
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
                val mp4UriAfterTrim =
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
                        getConvertedFile(
                            it.absolutePath, fileName
                        )
                    }

                binding.audioWaveView.visibility = View.VISIBLE
                listOfSelectedFiles[counter].musicFileName = mp4UriAfterTrim?.path

                Timber.tag("CreateStoryActivity").i("Download Completed")
                Timber.tag("CreateStoryActivity").i("Download path :${mp4UriAfterTrim?.path}")
                Timber.tag("CreateStoryActivity").i("Download path :${mp4UriAfterTrim?.exists()}")
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(mp4UriAfterTrim?.path.toString()))
                    audioPlayer.setMediaItem(mediaItem)
                    audioPlayer.prepare()
                    audioPlayer.addListener(object : Player.Listener {
                        override fun onPlayerStateChanged(
                            playWhenReady: Boolean, playbackState: Int
                        ) {
                            if (playbackState == ExoPlayer.STATE_READY) {
                                println("listOfSelectedFiles.size: " + listOfSelectedFiles.size)
                                if(counter == listOfSelectedFiles.size) {
                                    counter -= 1
                                }
                                if (listOfSelectedFiles[counter].durationSet == false) {
                                    binding.progressBar.visibility = View.GONE
                                    binding.btnDone.visibility = View.VISIBLE
                                    binding.btnDone.isEnabled = true

                                    if (!audioPlayer.isPlaying) {
                                        val duration = audioPlayer.duration
                                        val granularity = 1000
                                        val itemCount = (duration / granularity).toInt()
                                        val itemList = MutableList(itemCount) {}
                                        binding.audioWaveView.adapter = AudioWaveAdapter(itemList)
                                        audioPlayer.playWhenReady = true
                                        videoPlayer.playWhenReady = true
                                        videoPlayer.play()
                                    }
                                    listOfSelectedFiles[counter].durationSet = true
                                }
                            }
                        }
                    })
                } catch (e: java.lang.Exception) {
                    Timber.tag("CreateStoryActivity").i("Error : $e")
                }
            }
        }
    }

    private fun downloadTrimAudio() {
        val outputFileName = if (singleItem) {
            "${fileName}_trimmed_audio.mp3"
        } else {
            "${fileName}_${counter}_trimmed_audio.mp3"
        }
        val fileUri =
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer")), fileName)
        if (!fileUri.exists()) {
            Timber.tag("CreateStoryActivity")
                .e("Input file does not exist: ${fileUri.absolutePath}")
            return
        }

        mp3Uri = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
            getConvertedFile(
                it.absolutePath, outputFileName
            )
        }


        val startTimeInSeconds = startPosition / 1000.0
        val endTimeInSeconds = videoDuration / 1000.0

        Timber.tag("CreateStoryActivity")
            .d("startTimeInSeconds: $startTimeInSeconds & endTimeInSeconds: $endTimeInSeconds")

        val cmd = mp3Uri?.let {
            arrayOf(
                "-y",
                "-i",
                fileUri.absolutePath,
                "-ss",
                "$startTimeInSeconds",
                "-t",
                "$endTimeInSeconds",
                "-c",
                "copy",
                it.path
            )
        }

        Timber.tag("CreateStoryActivity").d("cmd :$cmd")
        Thread {
            try {
                val result: Int = FFmpeg.execute(cmd)
                Timber.tag("CreateStoryActivity").d("result: %s", result)
                when (result) {
                    0 -> {
                        Timber.tag("CreateStoryActivity").i("result: Success")
                        Timber.tag("CreateStoryActivity").i("result: Success ${mp3Uri?.path}")
                        listOfSelectedFiles[counter].isTrimMusic = true
                        listOfSelectedFiles[counter].trimMusicFileName = mp3Uri?.path.toString()

                        runOnUiThread {
                            if (listOfSelectedFiles[counter].isTrimMusic == true) {
                                binding.progress.isVisible = false
                                binding.itemListRecyclerView.isVisible = true
                                binding.btnAdd.background =
                                    ResourcesCompat.getDrawable(resources, R.drawable.bg_purple_border, null)
                                binding.btnAdd.isEnabled = true
                                Timber.tag("CreateStoryActivity")
                                    .i("trimMusicFileName :${listOfSelectedFiles[counter].trimMusicFileName}")
                                setUpTrimAudio(listOfSelectedFiles[counter].musicResponse)
                                listOfSelectedFiles[counter].durationSet = false
                                setMusic(listOfSelectedFiles[counter].trimMusicFileName)
                                countDownTimer?.cancel()
                            }
                        }
                    }

                    255 -> {
                        Timber.tag("CreateStoryActivity").d("result: Canceled")

                        runOnUiThread {
                            binding.progress.isVisible = false
                            binding.itemListRecyclerView.isVisible = true
                            binding.btnAdd.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.bg_purple_border, null)
                            binding.btnAdd.isEnabled = true
                            Timber.tag("CreateStoryActivity")
                                .i("trimMusicFileName :${listOfSelectedFiles[counter].trimMusicFileName}")
                            setUpTrimAudio(listOfSelectedFiles[counter].musicResponse)
                            listOfSelectedFiles[counter].durationSet = false
                            setMusic(listOfSelectedFiles[counter].musicFileName)
                            countDownTimer?.cancel()
                        }
                    }

                    else -> {
                        Timber.tag("CreateStoryActivity").e("result: Failed")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CreateStoryActivity").e(e)
            }

        }.start()
    }

    override fun onStop() {
        super.onStop()
        audioPlayer.pause()
        videoPlayer.pause()
        binding.progress.isVisible = false
    }

    override fun onPause() {
        super.onPause()
        if(audioPlayer != null)
            audioPlayer.pause()
        videoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        videoPlayer.release()
    }
    override fun onResume() {
        super.onResume()
        videoPlayer.play()
    }

    override fun onEditTextChangeListener(text: String?, colorCode: Int) {
        openAddTextPopupWindow(text ?: "", colorCode)
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Timber.tag("PhotoEditorSDK").i("onAddViewListener()")
        Handler(Looper.getMainLooper()).postDelayed({
            saveImageILocalDirectory()
        }, 1000)

        when (viewType) {
            ViewType.BRUSH_DRAWING -> Timber.tag("BRUSH_DRAWING").i("onAddViewListener")
            ViewType.EMOJI -> Timber.tag("EMOJI").i("onAddViewListener")
            ViewType.IMAGE -> Timber.tag("IMAGE").i("onAddViewListener")
            ViewType.TEXT -> Timber.tag("TEXT").i("onAddViewListener")
            else -> {}
        }
    }

    override fun onRemoveViewListener(numberOfAddedViews: Int) {
        Timber.tag("PhotoEditorSDK").i("onRemoveViewListener()")
        saveImageILocalDirectory()
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        Timber.tag("PhotoEditorSDK").i("onStartViewChangeListener()")
        when (viewType) {
            ViewType.BRUSH_DRAWING -> Timber.tag("BRUSH_DRAWING").i("onStartViewChangeListener")
            ViewType.EMOJI -> Timber.tag("EMOJI").i("onStartViewChangeListener")
            ViewType.IMAGE -> Timber.tag("IMAGE").i("onStartViewChangeListener")
            ViewType.TEXT -> Timber.tag("TEXT").i("onStartViewChangeListener")
            else -> {}
        }
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        Timber.tag("PhotoEditorSDK").i("onStopViewChangeListener()")
        saveImageILocalDirectory()
        when (viewType) {
            ViewType.BRUSH_DRAWING -> Timber.tag("BRUSH_DRAWING").i("onStopViewChangeListener")
            ViewType.EMOJI -> Timber.tag("EMOJI").i("onStopViewChangeListener")
            ViewType.IMAGE -> Timber.tag("IMAGE").i("onStopViewChangeListener")
            ViewType.TEXT -> Timber.tag("TEXT").i("onStopViewChangeListener")
            else -> {}
        }
    }

    private fun saveImageILocalDirectory() {
        Timber.tag("PhotoEditorSDK").d("saveImageILocalDirectory() -> counter: $counter")
        val dir = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
        val outgoerDir = File(dir, BaseConstants.TEXT_IMAGE_FOLDER_NAME)
        Timber.tag("PhotoEditorSDK").d("saveImageILocalDirectory() -> outgoerDir.isDirectory: ${outgoerDir.isDirectory}")
        Timber.tag("PhotoEditorSDK").d("saveImageILocalDirectory() -> outgoerDir.exists(): ${outgoerDir.exists()}")
        if (!listOfSelectedFiles[counter].isVideo()) {
            val photoEditor =  photoEditorSDK?.saveImage(BaseConstants.TEXT_IMAGE_FOLDER_NAME, "story_info_${generateRandomString(3)}_${System.currentTimeMillis()}.jpg")
            if (photoEditor != null) {
                Timber.tag("PhotoEditorSDK").d("saveImageILocalDirectory() -> Before listOfSelectedFiles[counter].filePath: ${listOfSelectedFiles[counter].filePath}")
                listOfSelectedFiles[counter].filePath = photoEditor
                Timber.tag("PhotoEditorSDK").d("saveImageILocalDirectory() -> After listOfSelectedFiles[counter].filePath: ${listOfSelectedFiles[counter].filePath}")
                val savedFileName = File(photoEditor).name
                val expectedFileName = listOfSelectedFiles[counter].filePath.let { File(it).name }
                isSaved = savedFileName == expectedFileName
            }
            storyMediaAdapter.listOfFilePath = listOfSelectedFiles
        }
    }
}