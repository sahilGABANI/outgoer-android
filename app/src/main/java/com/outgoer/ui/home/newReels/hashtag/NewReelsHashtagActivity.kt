package com.outgoer.ui.home.newReels.hashtag

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsHashTagsItem
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewReelsHashtagBinding
import com.outgoer.ui.home.newReels.hashtag.view.ReelsByHashTagAdapter
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewState
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import com.outgoer.videoplayer.JzvdStd
import javax.inject.Inject

class NewReelsHashtagActivity : BaseActivity() {

    companion object {
        private const val INTENT_REELS_HASHTAG_INFO = "REELS_HASHTAG_INFO"
        fun getIntent(context: Context, reelsHashTagInfo: ReelsHashTagsItem): Intent {
            val intent = Intent(context, NewReelsHashtagActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(INTENT_REELS_HASHTAG_INFO, reelsHashTagInfo)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsViewModel>
    private lateinit var reelsViewModel: ReelsViewModel
    private lateinit var binding: ActivityNewReelsHashtagBinding
    private var reelsHashTagInfo: ReelsHashTagsItem? = null
    private var isVideoInitCompleted = false
    private var listOfReelsInfo: List<ReelInfo> = listOf()
    private var isMute = false
    private lateinit var reelsByHashTagAdapter: ReelsByHashTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReelsHashtagBinding.inflate(layoutInflater)
        setContentView(binding.root)
        OutgoerApplication.component.inject(this)
        reelsViewModel = getViewModelFromFactory(viewModelFactory)
        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            reelsHashTagInfo = it.getParcelableExtra(INTENT_REELS_HASHTAG_INFO)

            if (reelsHashTagInfo != null) {
                binding.tvHashtagTitle.text = reelsHashTagInfo?.title
                listenToViewModel()
                listenToViewEvents()
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        reelsByHashTagAdapter = ReelsByHashTagAdapter(this).apply {
            reelsHashtagItemClicks.subscribeAndObserveOnMainThread { reelInfo ->
                startActivityWithDefaultAnimation(
                    PlayReelsByHashtagActivity.getIntent(
                        this@NewReelsHashtagActivity,
                        listOfReelsInfo as ArrayList<ReelInfo>, reelInfo
                    )
                )
            }
        }

        binding.rvReels.apply {
            layoutManager = GridLayoutManager(this@NewReelsHashtagActivity, 3)
            adapter = reelsByHashTagAdapter
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
            reelsViewModel.pullToRefreshReelsByHashTag(reelsHashTagInfo?.tagId ?: 0)
        }.autoDispose()

        reelsViewModel.getReelByHashTag(reelsHashTagInfo?.tagId ?: 0)
    }

    private fun listenToViewModel() {
        reelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ReelsViewState.LoadingState -> {
//                    buttonVisibility(it.isLoading)
                }
                is ReelsViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ReelsViewState.GetReelsByTagInfo -> {
                    listOfReelsInfo = it.listOfReelsInfo
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    reelsByHashTagAdapter.listOfDataItems = listOfReelsInfo
                }
                else -> {}
            }
        }.autoDispose()
    }

    override fun onDestroy() {
        JzvdStd.releaseAllVideos()
        super.onDestroy()
    }
}