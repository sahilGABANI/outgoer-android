package com.outgoer.ui.createevent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.cloudflare.model.CloudFlareConfig
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddMediaEventBinding
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
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventViewState
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.post.PostCameraBottomSheet
import java.io.File
import javax.inject.Inject

class AddMediaEventActivity : BaseActivity(), PhotoListByAlbumAdapterCallback,
    VideoListByAlbumAdapterCallback {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"
        private const val MEDIA_DATA = "MEDIA_DATA"
        private const val DEFAULT_IMAGE_SIZE = "DEFAULT_IMAGE_SIZE"
        private const val IS_SPONTY_INFO = "IS_SPONTY_INFO"

        fun launchActivity(context: Context): Intent {
            return Intent(context, AddMediaEventActivity::class.java)
        }

        fun getIntentWithData(context: Context, selectedType: String, defaultImageSize: Int = 3,isSpontyInfo: Boolean? = null): Intent {
            val intent = Intent(context, AddMediaEventActivity::class.java)
            intent.putExtra(MEDIA_DATA, selectedType)

            if(defaultImageSize < 3 || defaultImageSize == -1)
                intent.putExtra(DEFAULT_IMAGE_SIZE, defaultImageSize)
                intent.putExtra(IS_SPONTY_INFO, isSpontyInfo)
            return intent
        }

        private var selectedPhotoPathArrayList = ArrayList<String>()
        private var mediaUrl = ArrayList<String>()

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
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    private lateinit var binding: ActivityAddMediaEventBinding

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
        createEventsViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityAddMediaEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listenToViewModel()
        listenToViewEvent()

        loadAlbumWihPhotos(this)
        loadAlbumWihVideos(this)
        mediaUrl.clear()

        intent?.let {
            selectedMediaType = it.getStringExtra(MEDIA_DATA) ?: MEDIA_TYPE_IMAGE
            binding.llSelection.visibility = if(it.hasExtra(DEFAULT_IMAGE_SIZE)) View.VISIBLE else View.GONE

            if(selectedMediaType == MEDIA_TYPE_VIDEO) {
                selectedPhotoPathArrayList.clear()
                selectedVideoPathArrayList.clear()
                checkSelectedMediaManageVisibility()
                loadSelectedAlbumWithVideoList(0)
            }
        }

        createEventsViewModel.getCloudFlareConfig()
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
                startActivity(DeeparEffectsActivity.getIntent(context = this@AddMediaEventActivity, isSpontyInfo = intent.hasExtra(IS_SPONTY_INFO)))
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

                if(intent.getIntExtra(DEFAULT_IMAGE_SIZE, 3) == 1) {

                    if (selectedPhotoPathArrayList.size < 1) {
                        selectedPhotoPathArrayList.add(photoPath)
                        checkSelectedMediaManageVisibility()
                    } else {
                        showToast(getString(R.string.msg_max_1_photo_count))
                    }

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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val isCapturePhoto = data.getBooleanExtra(
                        PostCameraActivity.INTENT_EXTRA_IS_CAPTURE_PHOTO,
                        false
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
        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.GetCloudFlareConfig -> {
                    cloudFlareConfig = it.cloudFlareConfig
                }
                is EventViewState.CloudFlareConfigErrorMessage -> {
                    showLongToast(it.errorMessage)
                    onBackPressed()
                }
                is EventViewState.UploadMediaCloudFlareSuccess -> {
                    if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                        mediaUrl.add(it.mediaUrl)
                        selectedPhotoPathArrayList.removeFirstOrNull()
                        if (selectedPhotoPathArrayList.isNotEmpty()) {
                            cloudFlareConfig?.let { cConfig ->
                                uploadMediaToCloudFlare(cConfig)
                            }
                        } else {
                            val intent = Intent()
                            intent.putStringArrayListExtra("MEDIA_URL", mediaUrl)
                            intent.putExtra("MEDIA_URL_TYPE", MEDIA_TYPE_IMAGE)
                            setResult(RESULT_OK, intent)
                            finish()
                        }
                    } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                        if (selectedVideoPathArrayList.isNotEmpty()) {
                            val intent = Intent()
                            intent.putExtra("MEDIA_URL", arrayListOf(it.mediaUrl))
                            intent.putExtra("MEDIA_URL_TYPE", MEDIA_TYPE_VIDEO)
                            setResult(RESULT_OK, intent)
                            finish()
                        } else {
                            finish()
                        }
                    }
                }

                is EventViewState.UploadMediaCloudFlareVideoSuccess -> {
                    if (selectedVideoPathArrayList.isNotEmpty()) {

                        println("video url: " + it.videoUrl)
                        val intent = Intent()
                        intent.putExtra("MEDIA_URL", arrayListOf(it.mediaUrl))
                        intent.putExtra("UID", it.uid)
                        intent.putExtra("MEDIA_UID", it.videoUrl)
                        intent.putExtra("MEDIA_URL_TYPE", MEDIA_TYPE_VIDEO)

                        setResult(RESULT_OK, intent)
                        finish()
                    } else {
                        finish()
                    }
                }
                is EventViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is EventViewState.UploadMediaCloudFlareLoading -> {
                    buttonVisibility(it.isLoading)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun uploadMedia() {
        cloudFlareConfig?.let {
            uploadMediaToCloudFlare(it)
        } ?: createEventsViewModel.getCloudFlareConfig()
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
                createEventsViewModel.uploadImageToCloudFlare(
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
                createEventsViewModel.uploadVideoToCloudFlare(
                    this,
                    cloudFlareConfig,
                    File(selectedVideoPathArrayList.first()),
                    selectedMediaType
                )
                isUploadInProgress = true
            } else {
                finish()
            }
        }
    }
}