package com.outgoer.ui.create_story

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAddToStoryBinding
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.mediapicker.interfaces.PhotoListByAlbumAdapterCallback
import com.outgoer.mediapicker.models.AlbumPhotoModel
import com.outgoer.mediapicker.models.AlbumVideoModel
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.mediapicker.utils.MediaUtils
import com.outgoer.ui.create_story.view.MediaListByAlbumAdapter
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.video_preview.VideoPreviewActivity
import java.io.File

class AddToStoryActivity : BaseActivity(), PhotoListByAlbumAdapterCallback {


    private var type: String? = null
    private lateinit var binding: ActivityAddToStoryBinding

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"
        private const val MEDIA_DATA = "MEDIA_DATA"
        private const val DEFAULT_IMAGE_SIZE = "DEFAULT_IMAGE_SIZE"
        private const val TYPE = "TYPE"

        fun launchActivity(context: Context, name: String): Intent {
            val intent = Intent(context, AddToStoryActivity::class.java)
            intent.putExtra(TYPE, name)
            return intent
        }

        private var selectedPhotoPathArrayList = ArrayList<String>()
        private var selectedVideoPathArrayList = ArrayList<String>()
        private var mediaUrl = ArrayList<String>()

        fun isPhotoSelected(photoModel: PhotoModel): Int {
            for (i in 0 until selectedPhotoPathArrayList.size) {
                if (selectedPhotoPathArrayList[i] == photoModel.path) {
                    return i
                }
            }
            return -1
        }
    }

//    @Inject
//    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
//    private lateinit var createEventsViewModel: CreateEventsViewModel

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()

    private var selectedMediaType = MEDIA_TYPE_IMAGE

    private var cloudFlareConfig: CloudFlareConfig? = null
    private var isUploadInProgress: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
//        createEventsViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityAddToStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        listenToViewModel()
        listenToViewEvent()

        loadAlbumWihPhotos(this)
        mediaUrl.clear()
//        createEventsViewModel.getCloudFlareConfig()

        intent?.let {
            type = it.getStringExtra(TYPE)
            if (type == CreateMediaType.post.name) {
                binding.tvAlbumName.text = resources.getString(R.string.add_to_post)
            }
        }
    }


    private fun listenToViewEvent() {
        selectedPhotoPathArrayList = ArrayList()
        selectedVideoPathArrayList = arrayListOf()

        albumWithPhotoArrayList = ArrayList()
        photoModelArrayList = ArrayList()

        albumWithVideoArrayList = ArrayList()
        videoModelArrayList = ArrayList()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.btnUpload.throttleClicks().subscribeAndObserveOnMainThread {
            if (isUploadInProgress) {
                showToast(getString(R.string.msg_media_upload_is_in_progress))
            } else if (selectedPhotoPathArrayList.isNotEmpty()) {
                uploadMedia()
            }
        }.autoDispose()

        binding.rvPhotoList.layoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
    }

    //--------------------------Photo--------------------------
    private fun loadAlbumWihPhotos(context: Context) {
        MediaUtils.loadAlbumsWithPhotoList(context, onAlbumLoad = {
            albumWithPhotoArrayList = it
            if (albumWithPhotoArrayList.isNotEmpty()) {
                loadSelectedAlbumWithPhotoList(0)
            } else {
                showToast(getString(R.string.msg_there_are_no_photos))
            }
        })
    }

    private fun loadSelectedAlbumWithPhotoList(mPos: Int) {
        val albumModelKT = albumWithPhotoArrayList[mPos]
        photoModelArrayList = albumModelKT.photoModelArrayList
        binding.rvPhotoList.adapter = MediaListByAlbumAdapter(this, photoModelArrayList, this)
    }

    override fun onPhotoItemClick(mPos: Int) {
        if (isUploadInProgress) {
            showToast(getString(R.string.msg_media_upload_is_in_progress))
            return
        }

        val aPhotoModel = photoModelArrayList[mPos]
        aPhotoModel.path?.let { photoPath ->

            if (selectedPhotoPathArrayList.contains(photoPath)) {
                selectedPhotoPathArrayList.remove(photoPath)
                if (selectedVideoPathArrayList.contains(photoPath)) {
                    selectedVideoPathArrayList.remove(photoPath)
                }
                binding.selectedItemAppCompatTextView.text = selectedPhotoPathArrayList.size.toString()
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < 5) {
                    if (aPhotoModel.type?.startsWith(resources.getString(R.string.label_video).lowercase()) == true) {
                        if (selectedVideoPathArrayList.size < 2) {
                            selectedVideoPathArrayList.add(photoPath)
                            selectedPhotoPathArrayList.add(photoPath)
                            binding.selectedItemAppCompatTextView.text = selectedPhotoPathArrayList.size.toString()
                        } else {
                            showToast(getString(R.string.msg_max_video_2_count))
                        }
                    } else {
                        selectedPhotoPathArrayList.add(photoPath)
                        binding.selectedItemAppCompatTextView.text = selectedPhotoPathArrayList.size.toString()
                    }

                    checkSelectedMediaManageVisibility()
                } else {
                    showToast(getString(R.string.msg_max_media_count))
                }
            }
        }
    }

    //--------------------------Common--------------------------
    private fun checkSelectedMediaManageVisibility() {
        val selectedMediaFilePathArrayList = ArrayList<String>()

        if (selectedPhotoPathArrayList.isNotEmpty()) {
            selectedMediaFilePathArrayList.addAll(selectedPhotoPathArrayList)
        }

        if (selectedMediaFilePathArrayList.isNotEmpty()) {
            binding.rlFooter.visibility = View.VISIBLE
        } else {
            binding.rlFooter.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val isCapturePhoto = data.getBooleanExtra(
                        PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO, false
                    )
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        if (isCapturePhoto) {
                            val filePathArrayList = ArrayList<String>()
                            filePathArrayList.add(filePath)
                            selectedPhotoPathArrayList = filePathArrayList
                            checkSelectedMediaManageVisibility()
                            uploadMedia()
                        } else {
                            checkSelectedMediaManageVisibility()
                            uploadMedia()
                        }
                    }
                }
            }
        }
    }

//    private fun listenToViewModel() {
//        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
//            when (it) {
//                is EventViewState.GetCloudFlareConfig -> {
//                    cloudFlareConfig = it.cloudFlareConfig
//                }
//
//                is EventViewState.CloudFlareConfigErrorMessage -> {
//                    showLongToast(it.errorMessage)
//                    onBackPressed()
//                }
//
//                is EventViewState.UploadMediaCloudFlareSuccess -> {
//                    mediaUrl.add(it.mediaUrl)
//                    selectedPhotoPathArrayList.removeFirstOrNull()
//                    if (selectedPhotoPathArrayList.isNotEmpty()) {
//                        cloudFlareConfig?.let { cConfig ->
//                            uploadMediaToCloudFlare(cConfig)
//                        }
//                    } else {
//                        val intent = Intent()
//                        intent.putStringArrayListExtra(
//                            "MEDIA_URL",
//                            mediaUrl
//                        )
//                        intent.putExtra(
//                            "MEDIA_URL_TYPE",
//                            MEDIA_TYPE_IMAGE
//                        )
//                        setResult(RESULT_OK, intent)
//                        finish()
//                    }
//                }
//
//                is EventViewState.UploadMediaCloudFlareVideoSuccess -> {
//                    finish()
//                }
//
//                is EventViewState.ErrorMessage -> {
//                    showLongToast(it.errorMessage)
//                }
//
//                is EventViewState.UploadMediaCloudFlareLoading -> {
//                    buttonVisibility(it.isLoading)
//                }
//
//                else -> {}
//            }
//        }.autoDispose()
//    }

    private fun uploadMedia() {
//        cloudFlareConfig?.let {
//            uploadMediaToCloudFlare(it)
//        } ?: createEventsViewModel.getCloudFlareConfig()

        val outgoerFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), BaseConstants.TEXT_IMAGE_FOLDER_NAME)
        if (outgoerFolder.exists()) {
            outgoerFolder.listFiles()?.forEach { file ->
                file.delete()
            }
            outgoerFolder.delete()
        }

        if (type == CreateMediaType.post.name) {
            startActivity(CreateStoryActivity.getIntent(this, selectedPhotoPathArrayList, CreateMediaType.post.name))
        } else {
            startActivity(CreateStoryActivity.getIntent(this, selectedPhotoPathArrayList, CreateMediaType.story.name))
        }
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnUpload.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnUpload.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun uploadMediaToCloudFlare(cloudFlareConfig: CloudFlareConfig) {
//        if (selectedPhotoPathArrayList.isNotEmpty()) {
//            buttonVisibility(true)
//            val imageFile = File(selectedPhotoPathArrayList.first())
//            createEventsViewModel.uploadImageToCloudFlare(
//                this,
//                cloudFlareConfig,
//                imageFile,
//                selectedMediaType
//            )
//            isUploadInProgress = true
//        } else {
//            finish()
//        }
    }
}