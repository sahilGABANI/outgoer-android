package com.outgoer.ui.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityPostCameraBinding
import java.io.File

class PostCameraActivity : BaseActivity() {

    companion object {
        const val RC_CAPTURE_PICTURE = 10001
        const val INTENT_EXTRA_IS_CAPTURE_PHOTO = "INTENT_EXTRA_IS_CAPTURE_PHOTO"
        const val INTENT_EXTRA_FILE_PATH = "INTENT_EXTRA_FILE_PATH"
        fun launchActivity(context: Context, isCapturePhoto: Boolean): Intent {
            val intent = Intent(context, PostCameraActivity::class.java)
            intent.putExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, isCapturePhoto)
            return intent
        }
    }

    private lateinit var binding: ActivityPostCameraBinding
    private var isCapturePhoto: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        isCapturePhoto = intent?.getBooleanExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, true) ?: true

        binding.cameraView.setLifecycleOwner(this)
        binding.cameraView.useDeviceOrientation = true
        binding.cameraView.addCameraListener(cameraListener)

        if (isCapturePhoto) {
            binding.cameraView.mode = Mode.PICTURE
            binding.ivCapturePhoto.visibility = View.VISIBLE
            binding.ivStartVideoRecord.visibility = View.GONE
            binding.ivStopVideoRecord.visibility = View.GONE
        } else {
            binding.cameraView.mode = Mode.VIDEO
            binding.ivCapturePhoto.visibility = View.GONE
            binding.ivStartVideoRecord.visibility = View.VISIBLE
            binding.ivStopVideoRecord.visibility = View.GONE
        }

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivFlipCamera.throttleClicks().subscribeAndObserveOnMainThread {
            when (binding.cameraView.facing) {
                Facing.BACK -> binding.cameraView.facing = Facing.FRONT
                Facing.FRONT -> binding.cameraView.facing = Facing.BACK
            }
        }.autoDispose()

        binding.ivCapturePhoto.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cameraView.takePictureSnapshot()
        }.autoDispose()

        binding.ivStartVideoRecord.throttleClicks().subscribeAndObserveOnMainThread {
            val videoFile = File(cacheDir, System.currentTimeMillis().toString().plus(".mp4"))
            videoFile.let {
                binding.cameraView.takeVideoSnapshot(it)
            }
        }.autoDispose()

        binding.ivStopVideoRecord.throttleClicks().subscribeAndObserveOnMainThread {
            binding.cameraView.stopVideo()
        }.autoDispose()
    }

    private val cameraListener: CameraListener = object : CameraListener() {

        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            binding.ivClose.visibility = View.INVISIBLE

            binding.ivFlipCamera.visibility = View.INVISIBLE
            binding.ivStartVideoRecord.visibility = View.GONE
            binding.ivStopVideoRecord.visibility = View.VISIBLE
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)

            binding.ivClose.visibility = View.VISIBLE

            binding.ivFlipCamera.visibility = View.VISIBLE
            binding.ivStartVideoRecord.visibility = View.VISIBLE
            binding.ivStopVideoRecord.visibility = View.GONE

            val videoPath = result.file.absolutePath
            if (!videoPath.isNullOrEmpty()) {
                returnResult(videoPath)
            }
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            result.toFile(File(cacheDir, System.currentTimeMillis().toString().plus(".jpg"))) { photoFile ->
                if (photoFile != null) {
                    returnResult(photoFile.absolutePath)
                }
            }
        }
    }

    private fun returnResult(absolutePath: String) {
        val intent = Intent()
        intent.putExtra(INTENT_EXTRA_IS_CAPTURE_PHOTO, isCapturePhoto)
        intent.putExtra(INTENT_EXTRA_FILE_PATH, absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cameraView.removeCameraListener(cameraListener)
    }
}