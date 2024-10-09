package com.outgoer.ui.video_preview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.gowtham.library.utils.TrimVideo
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVideoPreviewBinding
import com.outgoer.ui.music.AddMusicActivity
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.ui.sponty.CreateSpontyActivity
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

class VideoPreviewActivity : BaseActivity() {

    private var postType: String = CreateMediaType.post.name
    private lateinit var binding: ActivityVideoPreviewBinding
    private var videoPath: String = ""

    private var countdownTimer: CountDownTimer? = null
    lateinit var videoPlayer: SimpleExoPlayer

    companion object {
        private val INTENT_EXTRA_VIDEO_PATH = "INTENT_EXTRA_VIDEO_PATH"
        private val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"

        fun launchActivity(context: Context, mediaType: String, videoPath: String): Intent {
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra(INTENT_EXTRA_VIDEO_PATH, videoPath)
            intent.putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
            return intent
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("testing", "onCreate VideoPreviewActivity")
        initPlayer()
        initUI()
    }

    val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.getData() != null) {
            val uri = Uri.parse(TrimVideo.getTrimmedVideoPath(result.getData()))

            videoPlayer.release()
            videoPlayer.stop()

            initPlayer()
            buildMediaSource(uri)
            Log.d("TAG", "Trimmed path:: " + uri)
            Log.d("TAG", "Trimmed path:: " + uri.path)
            videoPath = uri.path ?: ""

            val duration = getFormattedVideoDuration(videoPath)
            binding.timerAppCompatTextView.text = duration
            binding.seekBarTimer.text = duration

        }else
            Log.v("TAG","videoTrimResultLauncher data is null");
    }

    private fun initUI() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_VIDEO_PATH)) {
                val videoPath = it.getStringExtra(INTENT_EXTRA_VIDEO_PATH)
                postType = it.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: CreateMediaType.post.name

                if (!videoPath.isNullOrEmpty()) {
                    this.videoPath = videoPath
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

                    val duration = getFormattedVideoDuration(videoPath)
                    binding.timerAppCompatTextView.text = duration
                    binding.seekBarTimer.text = duration
                    buildMediaSource(Uri.parse(videoPath))
                    binding.playButton.isVisible = true
                    binding.videoControlsLayout.visibility = View.GONE
                } else {
                    finish()
                }


                binding.addMusicAppCompatImageView.visibility = if(postType.equals(CreateMediaType.reels_video.name) || postType.equals(CreateMediaType.reels.name) || postType.equals(CreateMediaType.post_video.name) || postType.equals(CreateMediaType.post.name)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } else {
                finish()
            }
        } ?: finish()

        binding.cropAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {

            TrimVideo.activity(videoPath)
                .setAccurateCut(true)
                .setHideSeekBar(true)
                .start(this,startForResult)
        }

        binding.playButton.throttleClicks().subscribeAndObserveOnMainThread {
            binding.playButton.isVisible = false
            binding.videoControlsLayout.visibility = View.VISIBLE
            videoPlayer.play()
            countdownTimer?.start()
            binding.timerAppCompatTextView.isVisible = false
            binding.ivPause.setImageResource(R.drawable.ic_pause)
        }.autoDispose()


        binding.addMusicAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(AddMusicActivity.getIntent(this@VideoPreviewActivity, intent?.getStringExtra(INTENT_EXTRA_MEDIA_TYPE) ?: "", videoPath))
//            finish()
        }.autoDispose()

        binding.ivPause.throttleClicks().subscribeAndObserveOnMainThread {
            if (videoPlayer.isPlaying) {
                binding.ivPause.setImageResource(R.drawable.ic_play_reels)
                videoPlayer.pause()
            } else {
                binding.ivPause.setImageResource(R.drawable.ic_pause)
                videoPlayer.play()
                countdownTimer?.start()
            }
        }.autoDispose()
        binding.ivDelete.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()


//        binding.addMusicAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivity(AddMusicActivity.getIntent(this@VideoPreviewActivity))
//        }.autoDispose()

        binding.btnAdd.throttleClicks().subscribeAndObserveOnMainThread {
            if(postType.equals(CreateMediaType.sponty_video.name) || postType.equals(CreateMediaType.sponty.name)) {
                startActivity(CreateSpontyActivity.getIntent(this@VideoPreviewActivity, postType, videoPath))
            } else {
                val intent = AddNewPostInfoActivity.launchActivity(
                    this,
                    postType = postType,
                    videoPath = videoPath
                )
                startActivityWithDefaultAnimation(intent)
//                finish()
            }
        }.autoDispose()

        countdownTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (videoPlayer.isPlaying) {
                    binding.seekbar.progress = videoPlayer.currentPosition.toInt()
                }

            }

            override fun onFinish() {

            }
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

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Check if the SeekBar is at its maximum value
                if (seekBar?.progress == seekBar?.max) {
                    countdownTimer?.cancel()
                    seekBar?.progress = 0
                    binding.ivPause.setImageResource(R.drawable.ic_play_reels)
                } else {
                    if (seekBar != null) {
                        binding.ivPause.setImageResource(R.drawable.ic_pause)
                        videoPlayer.seekTo(seekBar.progress.toLong())
//                        videoPlayer.play()
                        io.reactivex.Observable.timer(100,TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                             videoPlayer.play()
                        }.autoDispose()
                    }
                }
            }
        })



    }

    fun getVideoDuration(videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)

        val durationString: String = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toString()
        retriever.release()

        return if(durationString == "null") 0 else durationString.toLong()
    }

    fun getFormattedVideoDuration(videoPath: String?): String? {
        val videoDuration = getVideoDuration(videoPath)

        // Convert milliseconds to HH:MM:ss format
        var seconds = videoDuration / 1000
        binding.seekbar.max = videoDuration.toInt()

        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        seconds = seconds % 60
        return java.lang.String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }


    private fun initPlayer() {
        try {
            videoPlayer = SimpleExoPlayer.Builder(this).build()
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
            binding.playerView.setPlayer(videoPlayer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MOVIE)
                    .build()
                videoPlayer.setAudioAttributes(audioAttributes, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMediaSource(mUri: Uri) {
        try {
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, "Outgoer")
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri))
            videoPlayer.addMediaSource(mediaSource)
            videoPlayer.prepare()
            videoPlayer.playWhenReady = false
        } catch (e: java.lang.Exception) {
            Timber.tag("VideoPreviewActivity").i("Error : $e")
        }
    }
    override fun onResume() {
        super.onResume()
        initUI()
        Log.d("testing", "onResume video preview")
    }
    override fun onPause() {
        super.onPause()
        videoPlayer.stop()
        Log.d("testing", "onPause video preview")
    }
    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.stop()
        Log.d("testing", "onDestroy video preview")
    }
    override fun onBackPressed() {
        videoPlayer.stop()
        finish()
        Log.d("testing", "onBackPressed video preview")
    }


}