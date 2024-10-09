package com.outgoer.ui.deepar

import ai.deepar.ar.*
import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.google.common.util.concurrent.ListenableFuture
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.effects.model.EffectResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityDeeparEffetcsBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.ui.create_story.AddToStoryActivity
import com.outgoer.ui.create_story.CreateStoryActivity
import com.outgoer.ui.create_story.model.SelectedMedia
import com.outgoer.ui.deepar.view.EffectAdapter
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.ui.sponty.CreateSpontyActivity
import com.outgoer.ui.video_preview.VideoPreviewActivity
import com.outgoer.utils.DeeparDetails
import com.outgoer.utils.FileUtils
import me.hamedsj.centerzoomlayoutmanager.CenterZoomLinearLayoutManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException


class DeeparEffectsActivity : AppCompatActivity(), SurfaceHolder.Callback, AREventListener {

    private lateinit var binding: ActivityDeeparEffetcsBinding

    // Default camera lens value, change to CameraSelector.LENS_FACING_BACK to initialize with back camera
    private val defaultLensFacing = CameraSelector.LENS_FACING_BACK
    private var lensFacing = defaultLensFacing
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private val NUMBER_OF_BUFFERS = 2
    private var buffers: Array<ByteBuffer?>? = arrayOfNulls(NUMBER_OF_BUFFERS)
    private var currentBuffer = 0
    private var buffersInitialized = false
    private var imageAnalysis: ImageAnalysis? = null

    private var deepAR: DeepAR? = null
    var effects: ArrayList<String>? = null
    private var recording = false
    private var currentSwitchRecording = false
    private var width = 0
    private var height = 0
    private var videoFileName: File? = null
    private lateinit var effectAdapter: EffectAdapter
    private var mediaType: String = "post"
    private var isReels: Boolean = false
    private lateinit var handlePathOz: HandlePathOz
    private var handler: Handler? = null
    private var indexOfMask = -1
    private var indexOfEffect = -1
    private var indexOfFilter = -1
    private var indexOfBackground = -1

    private val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        if (!buffersInitialized) {
            buffersInitialized = true
            initializeBuffers(ySize + uSize + vSize)
        }
        val byteData = ByteArray(ySize + uSize + vSize)
        val width = image.width
        val yStride = image.planes[0].rowStride
        val uStride = image.planes[1].rowStride
        val vStride = image.planes[2].rowStride
        var outputOffset = 0
        if (width == yStride) {
            yBuffer[byteData, outputOffset, ySize]
            outputOffset += ySize
        } else {
            var inputOffset = 0
            while (inputOffset < ySize) {
                yBuffer.position(inputOffset)
                yBuffer[byteData, outputOffset, Math.min(yBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += yStride
            }
        }
        //U and V are swapped
        if (width == vStride) {
            vBuffer[byteData, outputOffset, vSize]
            outputOffset += vSize
        } else {
            var inputOffset = 0
            while (inputOffset < vSize) {
                vBuffer.position(inputOffset)
                vBuffer[byteData, outputOffset, Math.min(vBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += vStride
            }
        }
        if (width == uStride) {
            uBuffer[byteData, outputOffset, uSize]
            outputOffset += uSize
        } else {
            var inputOffset = 0
            while (inputOffset < uSize) {
                uBuffer.position(inputOffset)
                uBuffer[byteData, outputOffset, Math.min(uBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += uStride
            }
        }
        buffers!![currentBuffer]?.put(byteData)
        buffers!![currentBuffer]?.position(0)
        if (deepAR != null) {
            deepAR?.receiveFrame(
                buffers!![currentBuffer],
                image.width, image.height,
                image.imageInfo.rotationDegrees,
                lensFacing == CameraSelector.LENS_FACING_FRONT,
                DeepARImageFormat.YUV_420_888,
                image.planes[1].pixelStride
            )
        }
        currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        image.close()
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 123
        private val IS_STORY_INFO = "IS_STORY_INFO"
        private val IS_SPONTY_INFO = "IS_SPONTY_INFO"
        fun getIntent(context: Context, isStoryInfo: Boolean? = null, isSpontyInfo: Boolean? = null): Intent {
            var intent = Intent(context, DeeparEffectsActivity::class.java)
            isStoryInfo?.let {
                intent.putExtra(IS_STORY_INFO, isStoryInfo)
            }
            isSpontyInfo?.let {
                intent.putExtra(IS_SPONTY_INFO, isSpontyInfo)
            }
            return intent
        }
    }

    private fun openEditor(inputImage: Uri) {

        val cR: ContentResolver = this@DeeparEffectsActivity.contentResolver
        val mime = MimeTypeMap.getSingleton()
        val mediaTypeRaw = mime.getExtensionFromMimeType(cR.getType(inputImage));
        if (inputImage.toString().contains("image")) {
            if (mediaType.equals(CreateMediaType.sponty.name)) {
                startActivity(
                    CreateSpontyActivity.getIntent(
                        this@DeeparEffectsActivity,
                        mediaType,
                        inputImage?.let {
                            FileUtils.getPath(
                                this@DeeparEffectsActivity,
                                it
                            )
                        })
                )
            } else if (mediaType.equals(CreateMediaType.story.name)) {
                val outgoerFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), BaseConstants.TEXT_IMAGE_FOLDER_NAME)
                if (outgoerFolder.exists()) {
                    outgoerFolder.listFiles()?.forEach { file ->
                        file.delete()
                    }
                    outgoerFolder.delete()
                }


                startActivity(
                    CreateStoryActivity.getIntent(
                        this@DeeparEffectsActivity,
                        arrayListOf(inputImage.let {
                            FileUtils.getPath(
                                this@DeeparEffectsActivity,
                                it
                            )
                        } ?: ""), CreateMediaType.story.name))
            } else {
                val listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
                val imagePath = inputImage?.let {
                    FileUtils.getPath(
                        this@DeeparEffectsActivity,
                        it
                    )
                }
                imagePath?.let {
                    listOfSelectedFiles.add(SelectedMedia(imagePath, false))
                    val intent = AddNewPostInfoActivity.getIntent(
                        this,
                        postType = mediaType ?: "",
                        listOfSelectedFiles = listOfSelectedFiles
                    )

                    startActivity(intent)
                    finish()
                }
            }

        }

    }

    private fun openVideoEditor(inputImage: Uri?) {
        if (mediaType.equals(CreateMediaType.reels.name) || mediaType.equals(CreateMediaType.reels_video.name) || mediaType.equals(CreateMediaType.sponty_video.name) || mediaType.equals(
                CreateMediaType.post_video.name
            )
        ) {
            startActivity(
                VideoPreviewActivity.launchActivity(
                    this,
                    mediaType,
                    inputImage?.let {
                        FileUtils.getPath(
                            this@DeeparEffectsActivity,
                            it
                        )
                    } ?: "")
            )
            finish()
        } else {
            if (mediaType.equals(CreateMediaType.sponty.name)) {
                startActivity(
                    CreateSpontyActivity.getIntent(
                        this@DeeparEffectsActivity,
                        mediaType,
                        inputImage?.let {
                            FileUtils.getPath(
                                this@DeeparEffectsActivity,
                                it
                            )
                        })
                )
                finish()
            } else if (mediaType.equals(CreateMediaType.story.name) || mediaType.equals(CreateMediaType.story_video.name)) {
                startActivity(
                    CreateStoryActivity.getIntent(
                        this@DeeparEffectsActivity,
                        arrayListOf(inputImage?.let {
                            FileUtils.getPath(
                                this@DeeparEffectsActivity,
                                it
                            )
                        } ?: ""), CreateMediaType.story.name
                    )
                )
                finish()
            } else {
                val listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
                val imagePath = inputImage?.let {
                    FileUtils.getPath(
                        this@DeeparEffectsActivity,
                        it
                    )
                }
                imagePath?.let {
                    listOfSelectedFiles.add(SelectedMedia(imagePath, false))
                    val intent = AddNewPostInfoActivity.getIntent(
                        this,
                        postType = mediaType ?: "",
                        listOfSelectedFiles = listOfSelectedFiles
                    )

                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeeparEffetcsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        handlePathOz = HandlePathOz(this, listener)
        handler = Handler(Looper.getMainLooper())
        initializeDeepAR()

        val mp3UriAfterTrim = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS.plus("/outgoer"))
        val mp3MainFile = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        mp3UriAfterTrim?.deleteRecursively()
        mp3MainFile?.deleteRecursively()
        val result: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.deleteIfExists(mp3UriAfterTrim?.toPath())
        } else {
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
            } else {
                deleteFilesInDirectory()
            }
        } else {
            deleteFilesInDirectory()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with deleting files
                deleteFilesInDirectory()
            } else {
                // Permission denied, inform the user
                Timber.e("Permission denied. Cannot delete files without storage permission.")
            }
        }
    }

    private fun deleteFilesInDirectory() {
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val moviesOutgoerDir = File(externalStorageDir, "Movies/outgoer")

        if (moviesOutgoerDir.exists() && moviesOutgoerDir.isDirectory) {
            val files = moviesOutgoerDir.listFiles()

            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        val deleted = file.delete()
                        Timber.tag("DeeparActivity").i("${file.name} deleted: $deleted")
                    }
                }
                MediaScannerConnection.scanFile(
                    applicationContext, arrayOf<String>(moviesOutgoerDir.path), null
                ) { path, uri ->
                    Timber.tag("UploadingPostReelsService").i("Scanned $path:")
                    Timber.tag("UploadingPostReelsService").i("-> uri=$uri")

                }
            } else {
                Timber.tag("DeeparActivity").i("No files found in $moviesOutgoerDir")
            }
        } else {
            Timber.tag("DeeparActivity").i("$moviesOutgoerDir does not exist or is not a directory")
        }
    }

    fun initUI() {
        intent?.let {
            if(it.hasExtra(IS_STORY_INFO)){
                binding.spontyAppCompatTv.visibility = View.INVISIBLE
                binding.storyAppCompatTextView.visibility = if (it.hasExtra(IS_STORY_INFO)) View.VISIBLE else View.INVISIBLE
                binding.mediaTypeLinearLayout.visibility = if (it.hasExtra(IS_STORY_INFO)) View.INVISIBLE else View.VISIBLE
                if (it.hasExtra(IS_STORY_INFO)) mediaType = CreateMediaType.story.name
            } else {
                binding.storyAppCompatTextView.visibility = View.INVISIBLE
                binding.spontyAppCompatTv.visibility = if (it.hasExtra(IS_SPONTY_INFO)) View.VISIBLE else View.INVISIBLE
                binding.mediaTypeLinearLayout.visibility = if (it.hasExtra(IS_SPONTY_INFO)) View.INVISIBLE else View.VISIBLE
                if (it.hasExtra(IS_SPONTY_INFO)) mediaType = CreateMediaType.sponty.name
            }
        }


        initializeViews()


        binding.backAppCompatImageView.setOnClickListener {
            finish()
        }

        effectAdapter = EffectAdapter(this@DeeparEffectsActivity).apply {
            effectItemClicks.subscribeAndObserveOnMainThread {
                try {
                    OutgoerApplication.assetManager?.let { assetManager ->
                        try {
                            if (it.effectName == DeeparDetails.NONE) {
                                val pathNone: String? = null
                                deepAR?.switchEffect(it.type, pathNone)
                                saveLastUsedEffect(it)
                            } else {
                                val stream: InputStream = assetManager.open(it.effectFileName)

                                println("effectFileName: " + it.effectFileName)
                                println("stream: " + stream)
                                deepAR?.switchEffect(it.type, stream)
                                saveLastUsedEffect(it)
                            }
                            effectAdapter.listOfEffects?.indexOf(it)?.let { it1 ->
                                binding.effectsRecyclerView.smoothScrollToPosition(it1)
                            }
                        } catch (e: Exception) {
                            println("Exception: " + e.printStackTrace())
                            Timber.e(e)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }

            }
        }

        val myLayoutManager =
            CenterZoomLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        myLayoutManager.percentDiffFromCenter = 0.4f
        myLayoutManager.minScale = 0.2f


        binding.effectsRecyclerView.apply {
            adapter = effectAdapter
            layoutManager = myLayoutManager

            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (myLayoutManager.findFirstCompletelyVisibleItemPosition() >= 0) {
                        effectAdapter.listOfEffects?.elementAt(myLayoutManager.findFirstCompletelyVisibleItemPosition())
                            ?.let { response ->
                                try {
                                    OutgoerApplication.assetManager?.let { assetManager ->
                                        try {
                                            if (response.effectName == DeeparDetails.NONE) {
                                                val pathNone: String? = null
                                                deepAR?.switchEffect(response.type, pathNone)
                                                saveLastUsedEffect(response)
                                            } else {
                                                val stream: InputStream =
                                                    assetManager.open(response.effectFileName)
                                                deepAR?.switchEffect(response.type, stream)
                                                saveLastUsedEffect(response)
                                            }
                                        } catch (e: Exception) {
                                            Timber.e(e)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }

                            }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (!recyclerView.canScrollHorizontally(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {

                        effectAdapter.listOfEffects?.lastOrNull()?.let { response ->
                            try {
                                OutgoerApplication.assetManager?.let { assetManager ->
                                    try {
                                        if (response.effectName == DeeparDetails.NONE) {
                                            val pathNone: String? = null
                                            deepAR?.switchEffect(response.type, pathNone)
                                            saveLastUsedEffect(response)
                                        } else {
                                            val stream: InputStream =
                                                assetManager.open(response.effectFileName)
                                            deepAR?.switchEffect(response.type, stream)
                                            saveLastUsedEffect(response)

                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }

                                }

                            } catch (e: Exception) {
                                Timber.e(e)
                            }
                        }
                        binding.effectsRecyclerView.findViewHolderForAdapterPosition(
                            effectAdapter.listOfEffects?.size ?: 0 - 1
                        )?.itemView?.performClick()

                    }
                }
            })
        }

        binding.openGalleryRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
            if (mediaType.equals(CreateMediaType.post.name)) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                try {
//                    intent.type = "image/* video/*"
//                    intent.setAction(Intent.ACTION_GET_CONTENT);
//                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//                    resultLauncher.launch(intent)
                    //startActivityForResult(intent, MainHomeActivity.GALLERY_RESULT)


                    startActivity(AddToStoryActivity.launchActivity(this@DeeparEffectsActivity, CreateMediaType.post.name))
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "No Gallery APP installed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else if (mediaType.equals(CreateMediaType.reels.name)) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                try {
                    intent.type = "video/*"
                    resultLauncher.launch(intent)
                    //startActivityForResult(intent, MainHomeActivity.GALLERY_RESULT)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "No Gallery APP installed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else if (mediaType.equals(CreateMediaType.story.name) || mediaType.equals(CreateMediaType.story_video.name)) {
                startActivity(AddToStoryActivity.launchActivity(this@DeeparEffectsActivity, CreateMediaType.story.name))
            } else if (mediaType.equals(CreateMediaType.sponty.name)) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                try {
                    intent.type = "image/* video/*"
                    resultLauncher.launch(intent)
                    //startActivityForResult(intent, MainHomeActivity.GALLERY_RESULT)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "No Gallery APP installed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.storyIAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            handler = null
            binding.cvVideoTimer.isVisible = false
            mediaType = CreateMediaType.story.name
            binding.reelAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.spontyAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.postAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.storyIAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.md_white,
                    null
                )
            )
            binding.recordButton.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.photo_capture, null)
            )
        }

        binding.postAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            handler = null
            binding.cvVideoTimer.isVisible = false
            mediaType = CreateMediaType.post.name
            binding.reelAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.spontyAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.postAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.md_white,
                    null
                )
            )
            binding.storyIAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.recordButton.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.photo_capture, null)
            )
        }

        binding.reelAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            mediaType = CreateMediaType.reels.name
            binding.reelAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.md_white,
                    null
                )
            )
            binding.postAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.spontyAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.storyIAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.recordButton.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.video_start, null)
            )
        }


        binding.spontyAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            mediaType = CreateMediaType.sponty.name
            binding.reelAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.postAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )
            binding.spontyAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.md_white,
                    null
                )
            )
            binding.storyIAppCompatTextView.setTextColor(
                resources.getColor(
                    R.color.color_A7A7A7,
                    null
                )
            )

            binding.recordButton.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.photo_capture, null)
            )
        }
        effectAdapter.listOfEffects = DeeparDetails.getMasks()


        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.effectsRecyclerView)

        binding.effectsRecyclerView.setOnFlingListener(snapHelper)

        binding.maskAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            effectAdapter.listOfEffects = DeeparDetails.getMasks()
            if (indexOfMask != -1) {
                binding.effectsRecyclerView.smoothScrollToPosition(indexOfMask)
            }
        }
        binding.filtersAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            effectAdapter.listOfEffects = DeeparDetails.getFilters()
            if (indexOfFilter != -1) {
                binding.effectsRecyclerView.smoothScrollToPosition(indexOfFilter)
            }
        }
        binding.effectsAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            effectAdapter.listOfEffects = DeeparDetails.getEffects()
            if (indexOfEffect != -1) {
                binding.effectsRecyclerView.smoothScrollToPosition(indexOfEffect)
            }
        }
        binding.backgroundAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            effectAdapter.listOfEffects = DeeparDetails.getBackground()
            if (indexOfBackground != -1) {
                binding.effectsRecyclerView.smoothScrollToPosition(indexOfBackground)
            }
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initializeViews() {
        binding.surface.holder.addCallback(this)


        binding.recordButton.setOnLongClickListener {
            if (mediaType.equals(CreateMediaType.post.name)) {
                mediaType = CreateMediaType.post_video.name

                if (!recording) {
                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.video_pause,
                            null
                        )
                    )

                    videoFileName = File(
                        getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "video_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(
                            Date()
                        ) + ".mp4"
                    )
                    deepAR!!.startVideoRecording(
                        videoFileName.toString(),
                        width / 2,
                        height / 2
                    )
                    binding.cvVideoTimer.visibility = View.VISIBLE
                    runTimer()
                }
                recording = !recording
            } else if (mediaType.equals(CreateMediaType.sponty.name)) {
                mediaType = CreateMediaType.sponty_video.name

                if (!recording) {
                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.video_pause,
                            null
                        )
                    )

                    videoFileName = File(
                        getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "video_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(
                            Date()
                        ) + ".mp4"
                    )
                    deepAR!!.startVideoRecording(
                        videoFileName.toString(),
                        width / 2,
                        height / 2
                    )
                    binding.cvVideoTimer.visibility = View.VISIBLE
                    runTimer()
                }
                recording = !recording
            } else if (mediaType.equals(CreateMediaType.story.name)) {
                mediaType = CreateMediaType.story_video.name

                if (!recording) {
                    if(handler == null)
                        handler = Handler(Looper.getMainLooper())

                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.video_pause,
                            null
                        )
                    )

                    videoFileName = File(
                        getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "video_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(
                            Date()
                        ) + ".mp4"
                    )
                    deepAR!!.startVideoRecording(
                        videoFileName.toString(),
                        width / 2,
                        height / 2
                    )
                    binding.cvVideoTimer.visibility = View.VISIBLE
                    runTimer()
                }
                recording = !recording
            }
            true
        }

        binding.recordButton.setOnClickListener {
            if (mediaType.equals(CreateMediaType.post.name) || mediaType.equals(CreateMediaType.sponty.name) || mediaType.equals(
                    CreateMediaType.story.name
                )
            ) {
                deepAR?.takeScreenshot()
            } else if (mediaType.equals(CreateMediaType.post_video.name) || mediaType.equals(
                    CreateMediaType.story_video.name
                )
            ) {
                if (recording) {
                    binding.cvVideoTimer.visibility = View.GONE
                    handler = null

                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.photo_capture,
                            null
                        )
                    )

                    deepAR!!.stopVideoRecording()
                    recording = !recording
                }
            } else if (mediaType.equals(CreateMediaType.reels.name)) {
                mediaType = CreateMediaType.reels_video.name

                if (!recording) {
                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.video_pause,
                            null
                        )
                    )

                    videoFileName = File(
                        getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "video_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(
                            Date()
                        ) + ".mp4"
                    )
                    deepAR!!.startVideoRecording(
                        videoFileName.toString(),
                        width / 2,
                        height / 2
                    )
                    binding.cvVideoTimer.visibility = View.VISIBLE
                    runTimer()
                }
                recording = !recording
            } else if (mediaType.equals(CreateMediaType.reels_video.name)) {
                if (recording) {
                    binding.cvVideoTimer.visibility = View.GONE
                    handler = null

                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.photo_capture,
                            null
                        )
                    )

                    deepAR!!.stopVideoRecording()
                    recording = !recording
                }
            } else if (mediaType.equals(CreateMediaType.sponty_video.name)) {
                if (recording) {
                    binding.cvVideoTimer.visibility = View.GONE
                    handler = null

                    binding.recordButton.setImageDrawable(
                        resources.getDrawable(
                            com.outgoer.R.drawable.photo_capture,
                            null
                        )
                    )

                    deepAR!!.stopVideoRecording()
                    recording = !recording
                }
            }

        }

        binding.switchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            unBindCameraProvider()
            setupCamera()
        }
    }

    /*
        get interface orientation from
        https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164
     */
    private fun getScreenOrientation(): Int {
        val rotation = windowManager.defaultDisplay.rotation
        width = ScreenSizeCompat.getScreenSize(this).width
        height = ScreenSizeCompat.getScreenSize(this).width
        // if the device's natural orientation is portrait:
        val orientation: Int = if ((rotation == Surface.ROTATION_0
                    || rotation == Surface.ROTATION_180) && height > width ||
            (rotation == Surface.ROTATION_90
                    || rotation == Surface.ROTATION_270) && width > height
        ) {
            when (rotation) {
                Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            when (rotation) {
                Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        return orientation
    }

    private fun initializeDeepAR() {
        deepAR = DeepAR(this)
        deepAR?.setLicenseKey(resources.getString(com.outgoer.R.string.deepar_license_key))
        deepAR?.initialize(this, this)
        setupCamera()
        binding.surface.holder.addCallback(this)
        // Surface might already be initialized, so we force the call to onSurfaceChanged
        binding.surface.visibility = View.GONE
        binding.surface.visibility = View.VISIBLE
    }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture?.addListener({
            try {
                val cameraProvider = cameraProviderFuture?.get()
                if (cameraProvider != null) {
                    bindImageAnalysis(cameraProvider)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        val cameraResolutionPreset = CameraResolutionPreset.P1920x1080
        val width: Int
        val height: Int
        val orientation = getScreenOrientation()
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraResolutionPreset.width
            height = cameraResolutionPreset.height
        } else {
            width = cameraResolutionPreset.height
            height = cameraResolutionPreset.width
        }
        val cameraResolution = Size(width, height)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(cameraResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
        buffersInitialized = false
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    private fun initializeBuffers(size: Int) {
        buffers = arrayOfNulls(NUMBER_OF_BUFFERS)
        for (i in 0 until NUMBER_OF_BUFFERS) {
            buffers!![i] = ByteBuffer.allocateDirect(size)
            buffers!![i]?.order(ByteOrder.nativeOrder())
            buffers!![i]?.position(0)
        }
    }

    override fun onPause() {
        super.onPause()
        handler = null
        binding.cvVideoTimer.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (deepAR == null) {
            return
        }
        releaseCameraProvider()
        releaseDeepAR()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // If we are using on screen rendering we have to set surface view where DeepAR will render
        deepAR?.setRenderSurface(holder.surface, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (deepAR != null) {
            deepAR?.setRenderSurface(null, 0, 0)
        }
    }


    override fun screenshotTaken(bitmap: Bitmap?) {
        val now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", Date())
        try {
            val imageFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "image_$now.jpg"
            )
            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            MediaScannerConnection.scanFile(
                this@DeeparEffectsActivity,
                arrayOf<String>(imageFile.toString()),
                null,
                null
            )

            openEditor(Uri.fromFile(imageFile))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun videoRecordingStarted() {}

    override fun videoRecordingFinished() {
        videoFileName?.let {
            openVideoEditor(Uri.fromFile(videoFileName))
        }
    }

    override fun videoRecordingFailed() {}

    override fun videoRecordingPrepared() {}

    override fun shutdownFinished() {}

    override fun initialized() {
        Timber.e("DeepAr initialized")
        imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer)
        try {
            val pathNone: String? = null
            deepAR?.switchEffect("effect", pathNone)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun faceVisibilityChanged(b: Boolean) {}

    override fun imageVisibilityChanged(s: String?, b: Boolean) {}

    override fun frameAvailable(image: Image?) {}

    override fun error(arErrorType: ARErrorType?, s: String?) {}


    override fun effectSwitched(s: String?) {}


    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data

                if (data == null) {
                    Log.d("TAG", "Data returned is null")
                    return@registerForActivityResult
                } else {
                    data.data?.let { data ->

                        val selectedMedia: Uri = data
                        val cr: ContentResolver = getContentResolver()
                        val mime = cr.getType(selectedMedia)

                        val videoUri: Uri = data
                        val videoPath = parsePath(videoUri)
                        if (!videoPath.isNullOrEmpty()) {
                            if (mime?.lowercase()?.contains("video") ?: false) {
                                mediaType =
                                    if (mediaType.equals(CreateMediaType.post.name)) CreateMediaType.post_video.name else if (mediaType.equals(CreateMediaType.story.name) || mediaType.equals(CreateMediaType.story_video.name)) CreateMediaType.story_video.name  else if (mediaType.equals(CreateMediaType.sponty.name) || mediaType.equals(CreateMediaType.sponty_video.name)) CreateMediaType.sponty_video.name else CreateMediaType.reels_video.name

                                if (mediaType.equals(CreateMediaType.story.name) || mediaType.equals(CreateMediaType.story_video.name)) {
                                    startActivityWithDefaultAnimation(
                                        CreateStoryActivity.getIntent(
                                            this,
                                            arrayListOf(videoPath),
                                            CreateMediaType.story.name
                                        )
                                    )
                                } else {
                                    startActivityWithDefaultAnimation(
                                        VideoPreviewActivity.launchActivity(
                                            this,
                                            mediaType,
                                            videoPath
                                        )
                                    )
                                }
//                                finish()
                            } else if (mime?.lowercase()?.contains("image") ?: false) {
                                if (mediaType.equals(CreateMediaType.sponty.name)) {
                                    startActivity(
                                        CreateSpontyActivity.getIntent(
                                            this@DeeparEffectsActivity,
                                            mediaType,
                                            videoPath
                                        )
                                    )
                                } else {
                                    val listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
                                    listOfSelectedFiles.add(SelectedMedia(videoPath, false))
                                    val intent = AddNewPostInfoActivity.getIntent(
                                        this,
                                        postType = mediaType ?: "",
                                        listOfSelectedFiles = listOfSelectedFiles
                                    )
                                    startActivityWithDefaultAnimation(intent)
//                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun parsePath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor: Cursor? = this
            .contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            val columnIndex: Int = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            val path = cursor.getString(columnIndex)
            cursor.close() // Make sure you close cursor after use

            path
        } else null
    }


    private val listener = object : HandlePathOzListener.SingleUri {
        override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
            if (tr != null) {
                showToast(getString(R.string.error_in_finding_file_path))
            } else {
                val filePath = pathOz.path
                if (filePath.isNotEmpty()) {
                    val listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
                    listOfSelectedFiles.add(SelectedMedia(filePath, false))
                    val intent = AddNewPostInfoActivity.getIntent(
                        this@DeeparEffectsActivity,
                        postType = mediaType ?: "",
                        listOfSelectedFiles = listOfSelectedFiles
                    )
                    startActivityWithDefaultAnimation(intent)
                    finish()
                }
            }
        }
    }

    private fun runTimer() {
        var seconds = 0
        handler?.post(object : Runnable {
            override fun run() {
                val minutes: Int = seconds % 3600 / 60
                val secs: Int = seconds % 60
                val time: String = java.lang.String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    minutes,
                    secs
                )

                binding.timerTextView.text = time
                seconds++
                if (secs == 30) {
                    handler = null
                    videoFileName?.let {
                        openVideoEditor(Uri.fromFile(videoFileName))
                    }
                }
                handler?.postDelayed(this, 1000)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
//        if (mediaType == "photo") {
//            binding.recordButton.setImageDrawable(
//                resources.getDrawable(
//                    com.outgoer.R.drawable.photo_capture,
//                    null
//                )
//            )
//        } else {
//            binding.recordButton.setImageDrawable(
//                resources.getDrawable(
//                    com.outgoer.R.drawable.video_start,
//                    null
//                )
//            )
//        }

    }

    private fun saveLastUsedEffect(effect: EffectResponse) {
        when (effect.type) {
            "mask" -> {
                indexOfMask = effect.effectId - 1
            }

            "effects" -> {
                indexOfEffect = effect.effectId - 1
            }

            "filters" -> {
                indexOfFilter = effect.effectId - 1
            }

            "background" -> {
                indexOfBackground = effect.effectId - 1
            }
        }
    }

    private fun releaseDeepAR() {
        deepAR?.setAREventListener(null)
        deepAR?.release()
        deepAR = null
    }

    private fun releaseCameraProvider() {
        recording = false
        unBindCameraProvider()
    }

    private fun unBindCameraProvider() {
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = cameraProviderFuture?.get()
            cameraProvider?.unbindAll()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}