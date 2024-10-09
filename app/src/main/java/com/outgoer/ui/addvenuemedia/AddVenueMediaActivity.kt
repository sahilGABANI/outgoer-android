package com.outgoer.ui.addvenuemedia

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.outgoer.R
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.api.venue.model.AddVenueMediaItemRequest
import com.outgoer.api.venue.model.AddVenueMediaListRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddVenueMediaBinding
import com.outgoer.mediapicker.adapters.PhotoListByAlbumAdapter
import com.outgoer.mediapicker.adapters.VideoListByAlbumAdapter
import com.outgoer.mediapicker.fragments.PhotoAlbumListBSDialogFragment
import com.outgoer.mediapicker.fragments.VideoAlbumListBSDialogFragment
import com.outgoer.mediapicker.interfaces.PhotoListByAlbumAdapterCallback
import com.outgoer.mediapicker.interfaces.VideoListByAlbumAdapterCallback
import com.outgoer.mediapicker.models.AlbumPhotoModel
import com.outgoer.mediapicker.models.AlbumVideoModel
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.mediapicker.utils.PhotoUtils
import com.outgoer.mediapicker.utils.VideoUtils
import com.outgoer.ui.addvenuemedia.viewmodel.AddVenueMediaViewModel
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.post.PostCameraBottomSheet
import com.outgoer.ui.progress_dialog.ProgressDialogFragment
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class  AddVenueMediaActivity : BaseActivity(), PhotoListByAlbumAdapterCallback, VideoListByAlbumAdapterCallback {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"

        fun launchActivity(context: Context): Intent {
            return Intent(context, AddVenueMediaActivity::class.java)
        }

        private var selectedPhotoPathArrayList = ArrayList<String>()

        fun isPhotoSelected(photoModel: PhotoModel): Int {
            for (i in 0 until selectedPhotoPathArrayList.size) {
                if (selectedPhotoPathArrayList[i] == photoModel.path) {
                    return i
                }
            }
            return -1
        }

        private var selectedVideoPathArrayList = ArrayList<String>()

        fun isVideoSelected(videoModel: VideoModel): Int {
            for (i in 0 until selectedVideoPathArrayList.size) {
                if (selectedVideoPathArrayList[i] == videoModel.filePath) {
                    return i
                }
            }
            return -1
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddVenueMediaViewModel>
    private lateinit var addVenueMediaViewModel: AddVenueMediaViewModel

    private lateinit var binding: ActivityAddVenueMediaBinding

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
        addVenueMediaViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityAddVenueMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewModel()
        listenToViewEvent()

        loadAlbumWihPhotos(this)
        loadAlbumWihVideos(this)

        addVenueMediaViewModel.getCloudFlareConfig()
    }

    private fun listenToViewEvent() {
        selectedPhotoPathArrayList = ArrayList()
        selectedVideoPathArrayList = ArrayList()

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
            } else {
                if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                    if (selectedPhotoPathArrayList.isNotEmpty()) {
                        uploadMedia()
                    }
                } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                    if (selectedVideoPathArrayList.isNotEmpty()) {
                        uploadMedia()
                    }
                }
            }
        }.autoDispose()

        binding.tvAlbumName.throttleClicks().subscribeAndObserveOnMainThread {
            if (isUploadInProgress) {
                showToast(getString(R.string.msg_media_upload_is_in_progress))
            } else {
                if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                    showPhotoAlbumListDialog()
                } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                    showVideoAlbumListDialog()
                }
            }
        }.autoDispose()

        binding.cvMultipleSelect.throttleClicks().subscribeAndObserveOnMainThread {
            if (isUploadInProgress) {
                showToast(getString(R.string.msg_media_upload_is_in_progress))
            } else {
                if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                    if (albumWithVideoArrayList.isNotEmpty()) {
                        selectedMediaType = MEDIA_TYPE_VIDEO
                        binding.ivSwitchMedia.setImageResource(R.drawable.ic_post_multiple_select)
                        selectedPhotoPathArrayList.clear()
                        selectedVideoPathArrayList.clear()
                        checkSelectedMediaManageVisibility()
                        loadSelectedAlbumWithVideoList(0)
                    } else {
                        showToast(getString(R.string.msg_there_are_no_videos))
                    }
                } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                    if (albumWithPhotoArrayList.isNotEmpty()) {
                        selectedMediaType = MEDIA_TYPE_IMAGE
                        binding.ivSwitchMedia.setImageResource(R.drawable.ic_video_play)
                        selectedPhotoPathArrayList.clear()
                        selectedVideoPathArrayList.clear()
                        checkSelectedMediaManageVisibility()
                        loadSelectedAlbumWithPhotoList(0)
                    } else {
                        showToast(getString(R.string.msg_there_are_no_photos))
                    }
                }
            }
        }.autoDispose()

        binding.cvCamera.throttleClicks().subscribeAndObserveOnMainThread {
            if (isUploadInProgress) {
                showToast(getString(R.string.msg_media_upload_is_in_progress))
            } else {
                val bsFragment = PostCameraBottomSheet()
                bsFragment.postCameraItemClicks.subscribeAndObserveOnMainThread {
                    bsFragment.dismissBottomSheet()
                    startActivityForResultWithDefaultAnimation(
                        PostCameraActivity.launchActivity(this, it),
                        PostCameraActivity.RC_CAPTURE_PICTURE
                    )
                }.autoDispose()
                bsFragment.show(supportFragmentManager, PostCameraBottomSheet::class.java.name)
            }
        }.autoDispose()

        binding.rvPhotoList.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)
    }

    //--------------------------Photo--------------------------
    private fun loadAlbumWihPhotos(context: Context) {
        PhotoUtils.loadAlbumsWithPhotoList(context, onAlbumLoad = {
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
        binding.tvAlbumName.text = albumModelKT.albumName
        photoModelArrayList = albumModelKT.photoModelArrayList
        binding.rvPhotoList.adapter = PhotoListByAlbumAdapter(this, photoModelArrayList, this)
    }

    private fun showPhotoAlbumListDialog() {
        val bsFragment = PhotoAlbumListBSDialogFragment(albumWithPhotoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithPhotoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, PhotoAlbumListBSDialogFragment::class.java.name)
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
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < 3) {
                    selectedPhotoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                } else {
                    showToast(getString(R.string.msg_max_photo_count))
                }
            }
        }
    }

    //--------------------------Video--------------------------
    private fun loadAlbumWihVideos(context: Context) {
        VideoUtils.loadAlbumsWithVideoList(context, onAlbumLoad = {
            albumWithVideoArrayList = it
        })
    }

    private fun loadSelectedAlbumWithVideoList(mPos: Int) {
        val albumModelKT = albumWithVideoArrayList[mPos]
        binding.tvAlbumName.text = albumModelKT.albumName
        videoModelArrayList = albumModelKT.videoModelArrayList
        binding.rvPhotoList.adapter = VideoListByAlbumAdapter(this, videoModelArrayList, this)
    }

    private fun showVideoAlbumListDialog() {
        val bsFragment = VideoAlbumListBSDialogFragment(albumWithVideoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithVideoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, VideoAlbumListBSDialogFragment::class.java.name)
    }

    override fun onVideoItemClick(mPos: Int) {
        if (isUploadInProgress) {
            showToast(getString(R.string.msg_media_upload_is_in_progress))
            return
        }
        val aVideoModel = videoModelArrayList[mPos]
        aVideoModel.filePath?.let { photoPath ->
            if (selectedVideoPathArrayList.contains(photoPath)) {
                selectedVideoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedVideoPathArrayList.size < 1) {
                    selectedVideoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                } else {
                    showToast(getString(R.string.msg_max_video_count))
                }
            }
        }
    }

    //--------------------------Common--------------------------
    private fun checkSelectedMediaManageVisibility() {
        val selectedMediaFilePathArrayList = ArrayList<String>()

        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (selectedPhotoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedPhotoPathArrayList)
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedVideoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedVideoPathArrayList)
            }
        }

        if (selectedMediaFilePathArrayList.isNotEmpty()) {
            binding.rlFooter.visibility = View.VISIBLE
        } else {
            binding.rlFooter.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val isCapturePhoto = data.getBooleanExtra(PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO, false)
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        if (isCapturePhoto) {
                            val filePathArrayList = ArrayList<String>()
                            filePathArrayList.add(filePath)
                            selectedPhotoPathArrayList = filePathArrayList
                            checkSelectedMediaManageVisibility()
                            uploadMedia()
                        } else {
                            selectedVideoPathArrayList = arrayListOf(filePath)
                            checkSelectedMediaManageVisibility()
                            uploadMedia()
                        }
                    }
                }
            }
        }
    }

    private fun listenToViewModel() {
        addVenueMediaViewModel.addVenueMediaState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddVenueMediaViewModel.AddVenueMediaViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareSuccess -> {
                    if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                        if (selectedPhotoPathArrayList.isNotEmpty()) {
                            addVenueMedia(it.selectedMediaType, it.mediaUrl)
                        } else {
                            finish()
                        }
                    } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                        if (selectedVideoPathArrayList.isNotEmpty()) {
                            addVenueMedia(it.selectedMediaType, it.mediaUrl)
                        } else {
                            finish()
                        }
                    }
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.AddVenueMediaSuccess -> {
                    uploadMedia()
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UploadMediaCloudFlareLoading -> {
                    buttonVisibility(it.isLoading)
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun uploadMedia() {
        cloudFlareConfig?.let {
            uploadMediaToCloudFlare(it)
        } ?: addVenueMediaViewModel.getCloudFlareConfig()
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
        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (selectedPhotoPathArrayList.isNotEmpty()) {
                buttonVisibility(true)
                val imageFile = File(selectedPhotoPathArrayList.first())
                addVenueMediaViewModel.uploadImageToCloudFlare(
                    this,
                    cloudFlareConfig,
                    imageFile,
                    selectedMediaType
                )
                isUploadInProgress = true
            } else {
                finish()
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedVideoPathArrayList.isNotEmpty()) {
                buttonVisibility(true)
                compressVideoFile()
                isUploadInProgress = true
            } else {
                finish()
            }
        }
    }

    private fun compressVideoFile() {
        val videoUris = listOf(Uri.fromFile(File(selectedVideoPathArrayList.first())))
        lifecycleScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                videoUris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = "outgoer"
                ),
                configureWith = Configuration(
                    quality = VideoQuality.HIGH,
                    videoNames = videoUris.map { uri -> uri.pathSegments.last() },
                    isMinBitrateCheckEnabled = true,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {

                    }

                    override fun onStart(index: Int) {

                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        cloudFlareConfig?.let {
                            addVenueMediaViewModel.uploadVideoToCloudFlare(
                                this@AddVenueMediaActivity,
                                it,
                                File(selectedVideoPathArrayList.first()),
                                selectedMediaType
                            )
                        }
                        var progressDialogFragment = ProgressDialogFragment.newInstance()
                        progressDialogFragment.progressState.subscribeAndObserveOnMainThread {
                            progressDialogFragment.dismiss()
                        }
                        progressDialogFragment.show(supportFragmentManager, ProgressDialogFragment.javaClass.name)
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Timber.wtf(failureMessage)
                    }

                    override fun onCancelled(index: Int) {
                        Timber.wtf("compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
            )
        }
    }

    private fun addVenueMedia(selectedMediaType: String, mediaUrl: String) {
        val request = if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            selectedPhotoPathArrayList.removeFirstOrNull()
            AddVenueMediaItemRequest(
                type = 1,
                media = mediaUrl
            )
        } else {
            selectedVideoPathArrayList.removeFirstOrNull()
            AddVenueMediaItemRequest(
                type = 2,
                media = mediaUrl
            )
        }
        addVenueMediaViewModel.addVenueMedia(AddVenueMediaListRequest(listOf(request)))
    }
}