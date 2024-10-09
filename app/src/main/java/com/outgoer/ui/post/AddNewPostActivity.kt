package com.outgoer.ui.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddNewPostBinding
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
import com.outgoer.ui.croppostimages.CropPostImagesActivity
import com.outgoer.videoplayer.JZMediaExoKotlin
import java.io.File

class
AddNewPostActivity : BaseActivity(), PhotoListByAlbumAdapterCallback, VideoListByAlbumAdapterCallback {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "POST_TYPE_VIDEO"

        fun launchActivity(context: Context): Intent {
            return Intent(context, AddNewPostActivity::class.java)
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

    private lateinit var binding: ActivityAddNewPostBinding

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()

    private var selectedMediaType = MEDIA_TYPE_IMAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
        loadAlbumWihPhotos(this)
        loadAlbumWihVideos(this)
    }

    private fun listenToViewEvents() {
        selectedPhotoPathArrayList = ArrayList()
        selectedVideoPathArrayList = ArrayList()

        albumWithPhotoArrayList = ArrayList()
        photoModelArrayList = ArrayList()

        albumWithVideoArrayList = ArrayList()
        videoModelArrayList = ArrayList()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivNext.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                if (selectedPhotoPathArrayList.isNotEmpty()) {
                    val intent = CropPostImagesActivity.getIntent(
                        this,
                        imagePathList = selectedPhotoPathArrayList
                    )
                    startActivityWithDefaultAnimation(intent)
                }
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                if (selectedVideoPathArrayList.isNotEmpty()) {
                    val intent = AddNewPostInfoActivity.launchActivity(
                        this,
                        postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                        videoPath = selectedVideoPathArrayList.first()
                    )
                    startActivityWithDefaultAnimation(intent)
                }
            }
        }.autoDispose()

        binding.tvAlbumName.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                showPhotoAlbumListDialog()
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                showVideoAlbumListDialog()
            }
        }.autoDispose()

        binding.cvMultipleSelect.throttleClicks().subscribeAndObserveOnMainThread {
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
        }.autoDispose()

        binding.cvCamera.throttleClicks().subscribeAndObserveOnMainThread {
            val bsFragment = PostCameraBottomSheet()
            bsFragment.postCameraItemClicks.subscribeAndObserveOnMainThread {
                bsFragment.dismissBottomSheet()
                startActivityForResultWithDefaultAnimation(
                    PostCameraActivity.launchActivity(this, it),
                    PostCameraActivity.RC_CAPTURE_PICTURE
                )
            }.autoDispose()
            bsFragment.show(supportFragmentManager, PostCameraBottomSheet::class.java.name)
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
        val aPhotoModel = photoModelArrayList[mPos]
        aPhotoModel.path?.let { photoPath ->
            if (selectedPhotoPathArrayList.contains(photoPath)) {
                selectedPhotoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedPhotoPathArrayList.size < 3) {
                    selectedPhotoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
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
        val aVideoModel = videoModelArrayList[mPos]
        aVideoModel.filePath?.let { photoPath ->
            if (selectedVideoPathArrayList.contains(photoPath)) {
                selectedVideoPathArrayList.remove(photoPath)
                checkSelectedMediaManageVisibility()
            } else {
                if (selectedVideoPathArrayList.size < 1) {
                    selectedVideoPathArrayList.add(photoPath)
                    checkSelectedMediaManageVisibility()
                }
            }
        }
    }

    //--------------------------Common--------------------------
    private fun checkSelectedMediaManageVisibility() {
        val selectedMediaFilePathArrayList = ArrayList<String>()

        Jzvd.releaseAllVideos()

        binding.outgoerVideoPlayer.visibility = View.GONE
        binding.ivSelectedPhoto.visibility = View.GONE

        if (selectedMediaType == MEDIA_TYPE_IMAGE) {
            if (selectedPhotoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedPhotoPathArrayList)

                binding.ivSelectedPhoto.visibility = View.VISIBLE

                Glide.with(this)
                    .load(selectedPhotoPathArrayList.last())
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivSelectedPhoto)
            }
        } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
            if (selectedVideoPathArrayList.isNotEmpty()) {
                selectedMediaFilePathArrayList.addAll(selectedVideoPathArrayList)

                binding.outgoerVideoPlayer.visibility = View.VISIBLE

                val videoFile = File(selectedVideoPathArrayList.last())
                binding.outgoerVideoPlayer.apply {
                    videoUrl = videoFile.path
                    isVideMute = false
                    Glide.with(this)
                        .load(videoFile)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(posterImageView)

                    val jzDataSource = JZDataSource(videoUrl)
                    jzDataSource.looping = true
                    this.setUp(
                        jzDataSource,
                        Jzvd.SCREEN_NORMAL,
                        JZMediaExoKotlin::class.java
                    )
                    unMute()
//                    Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                    startVideoAfterPreloading()
                }
            }
        }

        if (selectedMediaFilePathArrayList.isNotEmpty()) {
            binding.ivNext.visibility = View.VISIBLE
            binding.tvSelectMediaHint.visibility = View.GONE
        } else {
            binding.ivNext.visibility = View.INVISIBLE
            binding.tvSelectMediaHint.visibility = View.VISIBLE
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
                            val intent = CropPostImagesActivity.getIntent(
                                this,
                                imagePathList = filePathArrayList
                            )
                            startActivityWithDefaultAnimation(intent)
                        } else {
                            val intent = AddNewPostInfoActivity.launchActivity(
                                this,
                                postType = AddNewPostInfoActivity.POST_TYPE_VIDEO,
                                videoPath = filePath
                            )
                            startActivityWithDefaultAnimation(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()

    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}