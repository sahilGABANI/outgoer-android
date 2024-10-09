package com.outgoer.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityForResultWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityChatSelectImageBinding
import com.outgoer.mediapicker.fragments.PhotoAlbumListBSDialogFragment
import com.outgoer.mediapicker.fragments.VideoAlbumListBSDialogFragment
import com.outgoer.mediapicker.models.AlbumPhotoModel
import com.outgoer.mediapicker.models.AlbumVideoModel
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.mediapicker.utils.PhotoUtils
import com.outgoer.mediapicker.utils.VideoUtils
import com.outgoer.ui.chat.view.SelectImageForChatAdapter
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.post.PostCameraBottomSheet

class ChatSelectImageActivity : BaseActivity() {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "CHAT_TYPE_IMAGE"
        private const val MEDIA_TYPE_VIDEO = "CHAT_TYPE_VIDEO"

        fun getIntent(context: Context): Intent {
            return Intent(context, ChatSelectImageActivity::class.java)
        }
    }

    private lateinit var binding: ActivityChatSelectImageBinding

    private var albumWithPhotoArrayList = ArrayList<AlbumPhotoModel>()
    private var photoModelArrayList = ArrayList<PhotoModel>()

    private var albumWithVideoArrayList = ArrayList<AlbumVideoModel>()
    private var videoModelArrayList = ArrayList<VideoModel>()

    private lateinit var selectImageForChatAdapter: SelectImageForChatAdapter
    private var selectedMediaType = MEDIA_TYPE_IMAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatSelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvents()
        loadAlbumWithPhotos(this)
        loadAlbumWihVideos(this)
    }

    private fun listenToViewEvents() {
        albumWithPhotoArrayList = ArrayList()
        photoModelArrayList = ArrayList()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        binding.tvAlbumName.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                showPhotoAlbumListDialog()
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                showVideoAlbumListDialog()
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

        binding.cvMultipleSelect.throttleClicks().subscribeAndObserveOnMainThread {
            if (selectedMediaType == MEDIA_TYPE_IMAGE) {
                if (albumWithVideoArrayList.isNotEmpty()) {
                    selectedMediaType = MEDIA_TYPE_VIDEO
                    binding.ivSwitchMedia.setImageResource(R.drawable.ic_post_multiple_select)
                    loadSelectedAlbumWithVideoList(0)
                } else {
                    showToast(getString(R.string.msg_there_are_no_videos))
                }
            } else if (selectedMediaType == MEDIA_TYPE_VIDEO) {
                if (albumWithPhotoArrayList.isNotEmpty()) {
                    selectedMediaType = MEDIA_TYPE_IMAGE
                    binding.ivSwitchMedia.setImageResource(R.drawable.ic_video_play)

                    loadSelectedAlbumWithPhotoList(0)
                } else {
                    showToast(getString(R.string.msg_there_are_no_photos))
                }
            }
        }.autoDispose()

        selectImageForChatAdapter = SelectImageForChatAdapter(this)
        selectImageForChatAdapter.apply {
            imageClick.subscribeAndObserveOnMainThread {
                val intent = Intent()
                intent.putExtra(NewChatActivity.INTENT_EXTRA_SELECTED_IMAGE_PATH, it.path)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }.autoDispose()

            videoClick.subscribeAndObserveOnMainThread {
                val intent = Intent()
                intent.putExtra(NewChatActivity.INTENT_EXTRA_SELECTED_VIDEO_PATH, it.filePath)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }.autoDispose()
        }

        binding.rvPhotoList.apply {
            layoutManager = GridLayoutManager(this@ChatSelectImageActivity, 4, RecyclerView.VERTICAL, false)
            adapter = selectImageForChatAdapter
        }
    }

    private fun showVideoAlbumListDialog() {
        val bsFragment = VideoAlbumListBSDialogFragment(albumWithVideoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithVideoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, VideoAlbumListBSDialogFragment::class.java.name)
    }

    private fun loadAlbumWihVideos(context: Context) {
        VideoUtils.loadAlbumsWithVideoList(context, onAlbumLoad = {
            albumWithVideoArrayList = it
        })
    }

    private fun loadSelectedAlbumWithVideoList(mPos: Int) {
        val albumModelKT = albumWithVideoArrayList[mPos]
        binding.tvAlbumName.text = albumModelKT.albumName
        videoModelArrayList = albumModelKT.videoModelArrayList
        selectImageForChatAdapter.listOfDataItems = arrayListOf()
        selectImageForChatAdapter.listOfVideoItems = videoModelArrayList
    }

    private fun loadAlbumWithPhotos(context: Context) {
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
        selectImageForChatAdapter.listOfVideoItems = arrayListOf()
        selectImageForChatAdapter.listOfDataItems = photoModelArrayList
    }

    private fun showPhotoAlbumListDialog() {
        val bsFragment = PhotoAlbumListBSDialogFragment(albumWithPhotoArrayList)
        bsFragment.itemClick.subscribeAndObserveOnMainThread {
            bsFragment.dismissBottomSheet()
            loadSelectedAlbumWithPhotoList(it)
        }.autoDispose()
        bsFragment.show(supportFragmentManager, PhotoAlbumListBSDialogFragment::class.java.name)
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
                            val intent = Intent()
                            intent.putExtra(NewChatActivity.INTENT_EXTRA_SELECTED_IMAGE_PATH, filePath)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        } else {
                            val intent = Intent()
                            intent.putExtra(NewChatActivity.INTENT_EXTRA_SELECTED_VIDEO_PATH, filePath)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }
                }
            }
        }
    }
}