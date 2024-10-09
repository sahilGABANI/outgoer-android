package com.outgoer.ui.music

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityTrimMusicBinding
import com.outgoer.ui.create_story.CreateStoryActivity
import com.outgoer.ui.music.view.AudioWaveAdapter
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.utils.MediaUtils
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.ceil
import kotlin.math.pow


class TrimMusicActivity : BaseActivity() {

    private var downloadId: Long? = null
    private var postType: String = CreateMediaType.post_video.name

    private lateinit var binding: ActivityTrimMusicBinding
    private var videoPath: String = ""
    private lateinit var videoPlayer: SimpleExoPlayer
    private lateinit var audioPlayer: SimpleExoPlayer
    private var handler: Handler? = null
    var startPosition: Int = 0
    var endPosition: Int = 0
    private lateinit var musicResponse: MusicResponse
    private var fileName: String = ""
    private var audioPath: String = ""
    private var videoDuration: Int = 0

    private var mp3Uri: File? = null
    private var durationSet: Boolean = false

    companion object {
        private const val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private const val INTENT_EXTRA_AUDIO_PATH = "INTENT_EXTRA_AUDIO_PATH"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"

        private const val TAG = "TrimMusicActivity"

        fun launchActivity(context: Context, mediaType: String, videoPath: String, songFile: MusicResponse): Intent {
            return Intent(context, TrimMusicActivity::class.java)
                .putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
                .putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
                .putExtra(INTENT_EXTRA_AUDIO_PATH, songFile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrimMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler = Handler()
        videoPath = intent?.getStringExtra(INTENT_EXTRA_VIDEO_PATH) ?: ""
        musicResponse = intent?.getParcelableExtra(INTENT_EXTRA_AUDIO_PATH) ?: return
        postType = intent?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: CreateMediaType.post_video.name
        fileName = getFileNameFromUri(musicResponse.songFile ?: "")

        initUI()
        initPlayer()
    }

    private fun initPlayer() {
        try {
            videoPlayer = SimpleExoPlayer.Builder(this).build()
            audioPlayer = SimpleExoPlayer.Builder(this).build()
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            val height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            retriever.release()

            binding.playerView.resizeMode = if(width > height) {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            binding.playerView.player = videoPlayer
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MOVIE)
                    .build()
                videoPlayer.setAudioAttributes(audioAttributes, true)
            }

            if (!videoPath.isNullOrEmpty()) {
                val duration = getFormattedVideoDuration(videoPath)
                videoDuration = getVideoDuration(videoPath).toInt()
                Log.d(TAG, "videoDuration: $videoDuration")
                binding.durationTxt.text = duration
                buildMediaSource(Uri.parse(videoPath))
            } else {
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val dataSourceFactory: DataSource.Factory =
                DefaultDataSourceFactory(this, resources.getString(R.string.app_name))
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(mUri))
            val loopingMediaSource = LoopingMediaSource(mediaSource)
            videoPlayer.setMediaSource(loopingMediaSource)
            videoPlayer.prepare()
            videoPlayer.volume = 0.0f
//            videoPlayer.playWhenReady = true
        } catch (e: java.lang.Exception) {
            Timber.tag(TAG).i("Error : $e")
        }
    }

    private fun getFormattedVideoDuration(videoPath: String?): String {
        val videoDuration = getVideoDuration(videoPath)

        // Convert milliseconds to HH:MM:ss format
        var seconds = videoDuration / 1000
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds %= 60

        return when {
            hours > 0 -> String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )

            minutes > 0 -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%02d", seconds)
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

    private fun initUI() {

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadAudio()
        binding.progressBar.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.audioWaveView.layoutManager = layoutManager

        Glide.with(this)
            .load(musicResponse.songImage)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivProfile)

        binding.musicTitleAppCompatTextView.text = musicResponse.songTitle
        binding.singerNameAppCompatTextView.text = musicResponse.songSubtitle

        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    videoPlayer.seekTo(0)
                    val itemCountLayout = layoutManager.itemCount
                    val scrollToPosition =
                        ((progress.toFloat() / 100) * (itemCountLayout - 1)).toInt()
                    layoutManager.scrollToPosition(scrollToPosition)

                    if (itemCountLayout > 0) {
                        val totalDuration = audioPlayer?.duration ?: 0
                        startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                        val audioPlayerLength =
                            audioPlayer?.currentPosition.toString().length
                        // Update audio playback position based on the scroll
                        val newIntVideoDuration = videoDuration / 1000
                        if (audioPlayerLength >= 3) {
                            startPosition -= calculateAdjustment(
                                audioPlayerLength,
                                newIntVideoDuration
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
                        endPosition = (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())
                        audioPlayer?.seekTo(startPosition.toLong())
                        audioPlayer?.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE

                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer?.isPlaying == true) {
                                        val currentPos = audioPlayer?.currentPosition ?: 0
                                        if (currentPos >= endPosition) {
                                            audioPlayer?.pause()
                                            binding.playButton.visibility = View.VISIBLE
                                            binding.pauseButton.visibility = View.INVISIBLE
                                        } else {
                                            handler?.postDelayed(
                                                this,
                                                100
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
                // Update the SeekBar progress based on the current scroll position
                videoPlayer.seekTo(0)
                val itemCountLayout = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (itemCountLayout > 0) {
                    val progress =
                        ceil((firstVisibleItem.toFloat() / (itemCountLayout - 1)) * 100).toInt()
                    Log.d("Progress", "progress: $progress")

                    if (progress in 95..100) {
                        binding.audioSeekBar.progress = 100
                    } else {
                        binding.audioSeekBar.progress = progress
                    }

                    val totalDuration = audioPlayer?.duration ?: 0
                    startPosition = ((progress.toFloat() / 100) * totalDuration).toInt()

                    val audioPlayerLength = audioPlayer?.currentPosition.toString().length
                    // Update audio playback position based on the scroll
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
                    endPosition = (startPosition + videoDuration).coerceAtMost(totalDuration.toInt())

                    audioPlayer?.seekTo(startPosition.toLong())
                    audioPlayer?.play()
                    binding.pauseButton.visibility = View.VISIBLE
                    binding.playButton.visibility = View.GONE

                    handler?.postDelayed(object : Runnable {
                        override fun run() {
                            try {
                                // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                if (audioPlayer?.isPlaying == true) {
                                    val currentPos = audioPlayer?.currentPosition ?: 0
                                    if (currentPos >= endPosition) {
                                        audioPlayer?.pause()
                                        audioPlayer?.seekTo(startPosition.toLong())
                                        audioPlayer?.play()
//                                        binding.playButton.visibility = View.VISIBLE
//                                        binding.pauseButton.visibility = View.INVISIBLE
                                    } else {
                                        handler?.postDelayed(
                                            this,
                                            100
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

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        audioPlayer?.seekTo(startPosition.toLong())
                        audioPlayer?.play()
                        binding.pauseButton.visibility = View.VISIBLE
                        binding.playButton.visibility = View.GONE
                        handler?.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    // Ensure audioPlayer is not null and is prepared before accessing its currentPosition
                                    if (audioPlayer?.isPlaying == true) {
                                        val currentPos = audioPlayer?.currentPosition ?: 0
                                        if (currentPos >= endPosition) {
                                            audioPlayer?.pause()
                                            audioPlayer?.seekTo(startPosition.toLong())
                                            audioPlayer?.play()
//                                            binding.playButton.visibility = View.VISIBLE
//                                            binding.pauseButton.visibility = View.INVISIBLE
                                        } else {
                                            handler?.postDelayed(
                                                this,
                                                100
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

        binding.playButton.setOnClickListener {
            pause()
            if (audioPlayer != null) {
                videoPlayer.seekTo(0)
                audioPlayer.seekTo(startPosition.toLong())
                audioPlayer.play()
                videoPlayer.play()
                handler?.postDelayed(object : Runnable {
                    override fun run() {
                        val currentPos = audioPlayer?.currentPosition ?: 0
                        if (currentPos >= endPosition) {
                            audioPlayer?.pause()
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
            audioPlayer?.pause()
            videoPlayer?.pause()
        }

        binding.btnAdd.throttleClicks().subscribeAndObserveOnMainThread {
            binding.progress.isVisible = true
            audioPlayer?.pause()
            videoPlayer?.pause()
            downloadTrimAudio()
        }.autoDispose()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressedDispatcher.onBackPressed()
        }.autoDispose()

    }

    private fun pause() {
        binding.pauseButton.visibility = View.VISIBLE
        binding.playButton.visibility = View.GONE
    }

    private fun play() {
        binding.pauseButton.visibility = View.GONE
        binding.playButton.visibility = View.VISIBLE
    }

    private fun getFileNameFromUri(uriString: String): String {
        val uri = Uri.parse(uriString)
        val path = uri.lastPathSegment
        return path ?: ""
    }

    private fun calculateAdjustment(length: Int, newIntVideoDuration: Int): Int {
        return newIntVideoDuration * 10.0.pow((length - 3).coerceAtLeast(0)).toInt()
    }

    object Utils {
        fun getConvertedFile(directoryPath: String, fileName: String): File {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return File(directory, fileName)
        }
    }

    private fun downloadAudio() {
        val request = DownloadManager.Request(Uri.parse(musicResponse.songFile))
        request.setTitle("Downloading $fileName")
        request.setDescription("Downloading $fileName")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val destinationUri =
            Uri.fromFile(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer")), fileName))
        request.setDestinationUri(destinationUri)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Timber.tag("TrimMusicActivity").i("downloadId :$downloadId")
    }

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            Timber.tag("TrimMusicActivity").i("id :$id")
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadId == id) {
//                Toast.makeText(this@TrimMusicActivity, "Download Completed", Toast.LENGTH_SHORT).show()
                val mp4UriAfterTrim = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
                    Utils.getConvertedFile(
                        it.absolutePath, fileName
                    )
                }
                Timber.tag("TrimMusicActivity").i("Download Completed")
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(mp4UriAfterTrim?.path.toString()))
                    audioPlayer.setMediaItem(mediaItem)
                    audioPlayer.prepare()
                    audioPlayer.addListener(object : Player.Listener {
                        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                            if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                                binding.progressBar.visibility = View.GONE
                                if (!audioPlayer.isPlaying) {
                                    val duration = audioPlayer.duration ?: 0
                                    val granularity = 1000
                                    val itemCount = (duration / granularity).toInt()
                                    val itemList = MutableList(itemCount) {}
                                    binding.audioWaveView.adapter = AudioWaveAdapter(itemList)
                                    audioPlayer.playWhenReady = true
                                    videoPlayer.playWhenReady = true
                                }
                                durationSet = true
                            }
                        }
                    })
                } catch (e: java.lang.Exception) {
                    Timber.tag(TAG).i("Error : $e")
                }
            }
        }
    }

    private fun downloadTrimAudio() {
        val outputFileName = "${fileName}_trimmed_audio.mp3"

        val fileUri = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer")), fileName)

        if (!fileUri.exists()) {
            Log.e("AudioTrim", "Input file does not exist: ${fileUri.absolutePath}")
            return
        }

        val dirUri = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer")), fileName)
        if(!dirUri.exists()) {
            dirUri.mkdir()
        }


        mp3Uri = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))?.let {
            Utils.getConvertedFile(
                it.absolutePath, outputFileName
            )
        }

        val startTimeInSeconds = startPosition / 1000.0
        val endTimeInSeconds = videoDuration / 1000.0

        Log.d(TAG, "startTimeInSeconds: $startTimeInSeconds & endTimeInSeconds: $endTimeInSeconds")

        val cmd = mp3Uri?.let {
            arrayOf(
                "-y",
                "-i", fileUri.absolutePath,
                "-ss", "$startTimeInSeconds",
                "-t", "$endTimeInSeconds",
                "-c", "copy",
                it.path
            )
        }

        Thread {
            val result: Int = FFmpeg.execute(cmd)
            Log.d("AudioTrim", "result: $result")
            when (result) {
                0 -> {
                    Log.i("AudioTrim", "result: Success")
                    audioPath = mp3Uri?.path.toString()

                    handler?.postDelayed({
                        if(postType.equals(CreateMediaType.story.name) || postType.equals(CreateMediaType.story.name)) {
                            val mergedVideoPath = MediaUtils.mergeVideoAndAudio(this@TrimMusicActivity, videoPath, audioPath)
                            startActivity(mergedVideoPath?.let { it1 ->
                                CreateStoryActivity.launchData(this@TrimMusicActivity, videoPath,
                                    it1
                                )
                            })
                            binding.progress.isVisible = false
                        } else {
                            val intent = AddNewPostInfoActivity.launchActivity(
                                this,
                                postType = getIntent()?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: "",
                                videoPath = videoPath,
                                audioResponse = musicResponse,
                                audioPath = audioPath,
                            )
                            startActivityWithDefaultAnimation(intent)
                            binding.progress.isVisible = false
                        }
                    }, 1000)

                }
                255 -> {
                    Log.d("AudioTrim", "result: Canceled")
                }
                else -> {
                    Log.e("AudioTrim", "result: Failed")
                }
            }
        }.start()
    }

    override fun onStop() {
        super.onStop()
        audioPlayer?.pause()
    }

    override fun onPause() {
        super.onPause()
        audioPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
        videoPlayer.release()
    }
}