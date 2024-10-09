package com.outgoer.ui.home.newReels.hashtag

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityPlayReelsByHahtagBinding
import com.outgoer.ui.home.newReels.ReelMoreOptionBottomSheet
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.newReels.view.NewPlayReelAdapter
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewState
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.Utility
import com.outgoer.utils.Utility.player
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import com.outgoer.videoplayer.OnViewPagerListener
import com.outgoer.videoplayer.ViewPagerLayoutManager
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayReelsByHashtagActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_REELS_BY_HASHTAG = "INTENT_EXTRA_REELS_BY_HASHTAG"
        private const val INTENT_EXTRA_REEL_INFO = "INTENT_EXTRA_REEL_INFO"

        fun getIntent(
            context: Context,
            listOfReelInfo: ArrayList<ReelInfo>,
            reelInfo: ReelInfo? = null
        ): Intent {
            val intent = Intent(context, PlayReelsByHashtagActivity::class.java)
            intent.putParcelableArrayListExtra(INTENT_EXTRA_REELS_BY_HASHTAG, listOfReelInfo)
            intent.putExtra(INTENT_EXTRA_REEL_INFO, reelInfo)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsViewModel>
    private lateinit var reelsViewModel: ReelsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var binding: ActivityPlayReelsByHahtagBinding
    private lateinit var newPlayReelAdapter: NewPlayReelAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager
    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1
    private var listOfReelsInfo: ArrayList<ReelInfo> = arrayListOf()
    private var isMute = false
    private var reelInfo: ReelInfo? = null
    private var isFromReelItemClick = false


    private val hasNextItem: Boolean
        get() = mCurrentPosition < (newPlayReelAdapter.listOfDataItems?.size ?: 0) - 1

    private val nextVideoUrls: List<String>?
        get() {
            val currentPosition = mCurrentPosition
            val listOfItems = newPlayReelAdapter.listOfDataItems
            if (!listOfItems.isNullOrEmpty() && hasNextItem) {
                val endIndex = minOf(currentPosition + 3, listOfItems.size)
                return listOfItems.subList(currentPosition + 1, endIndex).map { it.videoUrl.toString().plus("?clientBandwidthHint=2.5") }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayReelsByHahtagBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        reelsViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            listOfReelsInfo = it.getParcelableArrayListExtra(INTENT_EXTRA_REELS_BY_HASHTAG) ?: arrayListOf()

            val reelInfo = it.getParcelableExtra<ReelInfo>(INTENT_EXTRA_REEL_INFO)
            if (reelInfo != null) {
                this.reelInfo = reelInfo
                isFromReelItemClick = true
            }
            if (!listOfReelsInfo.isNullOrEmpty()) {
                listenToViewEvents()
                listenToViewModel()
            } else {
                onBackPressed()
            }

        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        videoLayoutManager = ViewPagerLayoutManager(this, OrientationHelper.VERTICAL)
        newPlayReelAdapter = NewPlayReelAdapter(this).apply {
            playReelViewClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ReelsPageState.MuteUnmuteClick -> {
                        isMute = state.isMute
                        listOfReelsInfo.forEach {
                            it.isMute = isMute
                        }
                        newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                    }
                    is ReelsPageState.UserProfileClick -> {
                        if (state.reelInfo.user?.storyCount == 1) {
                            listOfDataItems?.find { it.userId == state.reelInfo.userId }?.user?.storyCount = 0
                            newPlayReelAdapter.listOfDataItems = listOfDataItems
                            toggleSelectedStory(
                                this@PlayReelsByHashtagActivity,
                                storyListUtil,
                                state.reelInfo.userId
                            )
                        } else if (state.reelInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.reelInfo.user.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        this@PlayReelsByHashtagActivity, 0,
                                        state.reelInfo.user.id ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@PlayReelsByHashtagActivity,
                                    state.reelInfo.userId
                                )
                            )
                        }
                    }
                    is ReelsPageState.TaggedPeopleClick -> {
                        Jzvd.goOnPlayOnPause()
                        val bottomSheetFragment = ReelTaggedPeopleBottomSheet(state.reelInfo)
                        bottomSheetFragment.show(
                            supportFragmentManager,
                            ReelTaggedPeopleBottomSheet::class.java.name
                        )
                    }
                    is ReelsPageState.FollowClick -> {
                        reelsViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                    }
                    is ReelsPageState.UnfollowClick -> {
                        reelsViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                    }
                    is ReelsPageState.AddReelLikeClick -> {
                        reelsViewModel.addLikeToReel(state.reelInfo)
                    }
                    is ReelsPageState.RemoveReelLikeClick -> {
                        reelsViewModel.removeLikeFromReel(state.reelInfo)
                    }
                    is ReelsPageState.CommentClick -> {
//                        Jzvd.goOnPlayOnPause()
                        val newReelsCommentBottomSheet = NewReelsCommentBottomSheet(state.reelInfo)
                        newReelsCommentBottomSheet.reelsCommentIncrementViewState.subscribeAndObserveOnMainThread { reelInfo ->
                            val mPos = listOfReelsInfo.indexOfFirst {
                                it.id == reelInfo.id
                            }
                            if (mPos != -1) {
                                listOfReelsInfo.toMutableList()[mPos] = reelInfo
                                newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                            }
                        }
                        newReelsCommentBottomSheet.dismissClick.subscribeAndObserveOnMainThread { reelInfo ->
                            newReelsCommentBottomSheet.dismiss()
//                            Jzvd.goOnPlayOnResume()
                        }
                        newReelsCommentBottomSheet.show(
                            supportFragmentManager,
                            NewReelsCommentBottomSheet::class.java.name
                        )
                    }
                    is ReelsPageState.AddBookmarkClick -> {
                        reelsViewModel.addReelToBookmark(state.reelInfo)
                    }
                    is ReelsPageState.RemoveBookmarkClick -> {
                        reelsViewModel.removeReelToBookmark(state.reelInfo)
                    }
                    is ReelsPageState.ShareClick -> {
                        ShareHelper.shareDeepLink(
                            this@PlayReelsByHashtagActivity,
                            false,
                            state.reelInfo.id
                        ) {
                            ShareHelper.shareText(this@PlayReelsByHashtagActivity, it)
                        }
                    }
                    is ReelsPageState.MoreClick -> {
                        Jzvd.goOnPlayOnPause()
                        val bottomSheetFragment = ReelMoreOptionBottomSheet.newInstance(state.showReport)
                        bottomSheetFragment.isCancelable = false
                        bottomSheetFragment.reelMoreOptionClick.subscribeAndObserveOnMainThread {
                            when(it) {
                                PostMoreOption.BlockClick -> {}
                                PostMoreOption.DeleteClick -> {
                                    Jzvd.goOnPlayOnResume()
                                    bottomSheetFragment.dismissBottomSheet()
                                    reelsViewModel.deleteReel(state.reelInfo.id, 1)
                                }
                                PostMoreOption.ReportClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        Jzvd.goOnPlayOnResume()
                                        reportOptionBottomSheet.dismiss()
                                        reelsViewModel.reportReels(state.reelInfo.id, reportId)
                                    }.autoDispose()
                                    reportOptionBottomSheet.show(
                                        supportFragmentManager,
                                        ReportBottomSheet::class.java.name
                                    )
                                }
                                PostMoreOption.DismissClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    Jzvd.goOnPlayOnResume()
                                }
                            }
                        }.autoDispose()
                        bottomSheetFragment.show(
                            supportFragmentManager,
                            ReelMoreOptionBottomSheet::class.java.name
                        )
                    }
                    else -> {}
                }
            }
        }
        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = newPlayReelAdapter
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (isVideoInitCompleted) {
                    return
                }
                isVideoInitCompleted = true
                autoPlayVideo()
                nextVideoUrls?.let {
                    Timber.i("nextVideoUrl $nextVideoUrls")
                    Outgoer.exoCacheManager.prepareCacheVideos(it)
                }
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                if (mCurrentPosition == position) {
                    Timber.tag("PlayReelsByHashtagActivity").i("onPageRelease empty if condition")
                }
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }
                autoPlayVideo()
                mCurrentPosition = position
                nextVideoUrls?.let {
                    Timber.i("nextVideoUrl $nextVideoUrls")
                    Outgoer.exoCacheManager.prepareCacheVideos(it)
                }
                if (isBottom) {
                    Timber.tag("PlayReelsByHashtagActivity").i("onPageSelected empty if condition")
                }
            }
        })


        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
        }.autoDispose()

        prepareData()
    }

    private fun listenToViewModel() {
        reelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsViewState.FollowStatusUpdate -> {
                    listOfReelsInfo = (it.listOfReelsInfo ?: listOf()) as ArrayList<ReelInfo>
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    newPlayReelAdapter.listOfDataItems = it.listOfReelsInfo ?: listOf()
                }
                is ReelsViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    onBackPressed()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun prepareData() {
        listOfReelsInfo.forEach {
            it.isMute = isMute
        }
        newPlayReelAdapter.listOfDataItems = listOfReelsInfo

        if (isFromReelItemClick) {
            isFromReelItemClick = false

            val mPos = listOfReelsInfo.indexOfFirst {
                it.id == reelInfo?.id
            }
            if (mPos != -1) {
                Jzvd.goOnPlayOnPause()

                mCurrentPosition = mPos
                binding.reelsRecyclerView.scrollToPosition(mPos)

                Observable.timer(500, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                    autoPlayVideo()
                }.autoDispose()
            }
        }
    }


    private fun autoPlayVideo() {
        if (binding.reelsRecyclerView.getChildAt(0) == null) {
            return
        }
        player =
            binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.outgoerVideoPlayer)

        player?.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )

            if((player?.posterImageView?.width!! > player?.posterImageView?.height?.plus(100)!!) || (player?.posterImageView?.width!! > (player?.posterImageView?.height
                    ?: 0))
            ) {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
            } else {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
            }

            startVideoAfterPreloading()
        }
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        autoPlayVideo()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }

}