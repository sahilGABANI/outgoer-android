package com.outgoer.mediapicker.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.showLongToast
import com.outgoer.databinding.ActivityVideoListBinding
import com.outgoer.mediapicker.adapters.VideoListAdapter
import com.outgoer.mediapicker.interfaces.VideoListAdapterCallback
import com.outgoer.mediapicker.models.VideoModel
import com.outgoer.mediapicker.utils.VideoUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VideoListActivity : BaseActivity(), VideoListAdapterCallback, View.OnClickListener {

    companion object {
        private val selectionClicksSubject: PublishSubject<ArrayList<String>> = PublishSubject.create()
        val selectionClicks: Observable<ArrayList<String>> = selectionClicksSubject.hide()

        private const val INTENT_EXTRA_IS_SINGLE_SELECTION = "INTENT_EXTRA_IS_SINGLE_SELECTION"
        fun launchActivity(context: Context, isSingleSelection: Boolean = true): Intent {
            val intent = Intent(context, VideoListActivity::class.java)
            intent.putExtra(INTENT_EXTRA_IS_SINGLE_SELECTION, isSingleSelection)
            return intent
        }
    }

    private lateinit var binding: ActivityVideoListBinding

    private var selectedVideoFilePathStringArrayList = ArrayList<String>()

    private var videoModelKTArrayList = ArrayList<VideoModel>()
    private lateinit var videoListAdapter: VideoListAdapter
    private var isSingleSelection = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadVideoList(this)
        loadDataFromIntent()
    }

    private fun initViews() {
        videoModelKTArrayList = ArrayList()
        selectedVideoFilePathStringArrayList = ArrayList()

        binding.rvVideoList.layoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)

        videoListAdapter = VideoListAdapter(this, videoModelKTArrayList, this@VideoListActivity)
        binding.rvVideoList.adapter = videoListAdapter

        binding.ivBack.setOnClickListener(this)
        binding.ivDone.setOnClickListener(this)
    }

    private fun loadVideoList(context: Context) {
        VideoUtils.loadAlbumsWithVideoList(context, onAlbumLoad = {
//            videoModelKTArrayList = it
//            videoListAdapter = VideoListAdapter(context, videoModelKTArrayList, this@VideoListActivity)
//            binding.rvVideoList.adapter = videoListAdapter
        })
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_IS_SINGLE_SELECTION)) {
                val isSingleSelection = it.getBooleanExtra(INTENT_EXTRA_IS_SINGLE_SELECTION, true)
                this.isSingleSelection = isSingleSelection
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    override fun onClick(v: View?) {
        val mId = v?.id
        if (mId == R.id.ivBack) {
            finish()
        } else if (mId == R.id.ivDone) {
            selectionClicksSubject.onNext(selectedVideoFilePathStringArrayList)
            finish()
        }
    }

    override fun onVideoListItemClick(mPos: Int) {
        val videoModelKT = videoModelKTArrayList[mPos]
        val videoFilePath = videoModelKT.filePath
        if (videoFilePath != null && videoFilePath.isNotEmpty()) {
            if (isSingleSelection) {
                for (i in 0 until videoModelKTArrayList.size) {
                    videoModelKTArrayList[i].isSelected = false
                }
                videoModelKTArrayList[mPos].isSelected = true
                videoListAdapter.notifyDataSetChanged()

                selectedVideoFilePathStringArrayList = ArrayList()
                selectedVideoFilePathStringArrayList.add(videoFilePath)
            } else {
                videoModelKTArrayList[mPos].isSelected = !videoModelKT.isSelected
                videoListAdapter.notifyItemChanged(mPos)

                if (selectedVideoFilePathStringArrayList.contains(videoFilePath)) {
                    selectedVideoFilePathStringArrayList.remove(videoFilePath)
                } else {
                    selectedVideoFilePathStringArrayList.add(videoFilePath)
                }
            }
            if (selectedVideoFilePathStringArrayList.isNotEmpty()) {
                binding.ivDone.visibility = View.VISIBLE
            } else {
                binding.ivDone.visibility = View.INVISIBLE
            }
        } else {
            showLongToast(getString(R.string.msg_error_in_retrieving_selected_video_path))
        }
    }
}