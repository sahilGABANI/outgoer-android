package com.outgoer.ui.home.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityReelsBinding
import com.outgoer.ui.home.newReels.ReelMoreOptionBottomSheet
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewState
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import com.outgoer.ui.home.reels.view.PlayReelAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.SnackBarUtils
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import com.outgoer.videoplayer.OnViewPagerListener
import com.outgoer.videoplayer.ViewPagerLayoutManager
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReelsActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_REEL_INFO = "INTENT_EXTRA_REEL_INFO"
        fun getIntent(context: Context, reelInfo: ReelInfo? = null): Intent {
            val intent = Intent(context, ReelsActivity::class.java)
            intent.putExtra(INTENT_EXTRA_REEL_INFO, reelInfo)
            return intent
        }
    }

    private lateinit var binding: ActivityReelsBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsViewModel>
    private lateinit var reelsViewModel: ReelsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var playReelAdapter: PlayReelAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager
    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1
    private var listOfReelsInfo: List<ReelInfo> = listOf()
    private var isMute = false
    private var reelInfo: ReelInfo? = null
    private var isFromHomePageTopReelClick = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        reelsViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityReelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            val reelInfo = it.getParcelableExtra<ReelInfo>(INTENT_EXTRA_REEL_INFO)
            if (reelInfo != null) {
                this.reelInfo = reelInfo
                isFromHomePageTopReelClick = true
            }
        }

        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        videoLayoutManager = ViewPagerLayoutManager(this, OrientationHelper.VERTICAL)
        playReelAdapter = PlayReelAdapter(this).apply {
            playReelViewClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ReelsPageState.MuteUnmuteClick -> {
                        isMute = state.isMute
                        listOfReelsInfo.forEach {
                            it.isMute = isMute
                        }
                        playReelAdapter.listOfDataItems = listOfReelsInfo
                    }
                    is ReelsPageState.UserProfileClick -> {
                        if (state.reelInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.reelInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        this@ReelsActivity, 0,
                                        state.reelInfo.user.id ?: 0
                                    )
                                )
                            }

                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@ReelsActivity,
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
                        val reelsCommentBottomSheet = NewReelsCommentBottomSheet(state.reelInfo)
                        reelsCommentBottomSheet.reelsCommentIncrementViewState.subscribeAndObserveOnMainThread { reelInfo ->
                            val mPos = listOfReelsInfo.indexOfFirst {
                                it.id == reelInfo.id
                            }
                            if (mPos != -1) {
                                listOfReelsInfo.toMutableList()[mPos] = reelInfo
                                playReelAdapter.listOfDataItems = listOfReelsInfo
                            }
                        }
                        reelsCommentBottomSheet.dismissClick.subscribeAndObserveOnMainThread { reelInfo ->
                            reelsCommentBottomSheet.dismiss()
//                            Jzvd.goOnPlayOnResume()
                        }
                        reelsCommentBottomSheet.show(
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
                        ShareHelper.shareDeepLink(this@ReelsActivity, false, state.reelInfo.id) {
                            ShareHelper.shareText(this@ReelsActivity, it)
                        }
                    }
                    is ReelsPageState.MoreClick -> {
                        Jzvd.goOnPlayOnPause()
                        val bottomSheetFragment = ReelMoreOptionBottomSheet.newInstance(state.showReport)
                        bottomSheetFragment.isCancelable = false
                        bottomSheetFragment.reelMoreOptionClick.subscribeAndObserveOnMainThread {
                            when(it) {
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
                                PostMoreOption.BlockClick -> {}
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
            adapter = playReelAdapter
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (isVideoInitCompleted) {
                    return
                }
                isVideoInitCompleted = true
                autoPlayVideo()
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                if (mCurrentPosition == position) {
                    //Jzvd.releaseAllVideos()
                    Timber.tag("onPageRelease").i("if condition empty")
                }
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }
                autoPlayVideo()
                mCurrentPosition = position
                if (isBottom) {
                    reelsViewModel.loadMore(1)
                }
            }
        })

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
            reelsViewModel.pullToRefresh(1)
        }.autoDispose()

        reelsViewModel.getAllReels(1)
    }

    private fun listenToViewModel() {
        reelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ReelsViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is ReelsViewState.LoadingState -> {
//                    buttonVisibility(it.isLoading)
                }
                is ReelsViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ReelsViewState.GetAllReelsInfo -> {
                    listOfReelsInfo = it.listOfReelsInfo
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    playReelAdapter.listOfDataItems = it.listOfReelsInfo
                    hideShowNoData(listOfReelsInfo)

                    if (isFromHomePageTopReelClick) {
                        isFromHomePageTopReelClick = false

                        val mPos = listOfReelsInfo.indexOfFirst {
                            it.id == reelInfo?.id
                        }
                        if (mPos != -1) {
                            Jzvd.goOnPlayOnPause()

                            mCurrentPosition = mPos
                            binding.reelsRecyclerView.scrollToPosition(mPos)

                            Observable.timer(500, TimeUnit.MILLISECONDS)
                                .subscribeAndObserveOnMainThread {
                                    autoPlayVideo()
                                }.autoDispose()
                        }
                    }
                }
                is ReelsViewState.FollowStatusUpdate -> {
                    listOfReelsInfo = it.listOfReelsInfo ?: listOf()
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    playReelAdapter.listOfDataItems = it.listOfReelsInfo ?: listOf()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfReelsInfo: List<ReelInfo>) {
        if (listOfReelsInfo.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    private fun autoPlayVideo() {
        if (binding.reelsRecyclerView.getChildAt(0) == null) {
            return
        }
        val player: JzvdStdOutgoer? =
            binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.outgoerVideoPlayer)
        player?.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )
            if((player.posterImageView.width > player.posterImageView.height + 100) || (player.posterImageView.width > player.posterImageView.height)) {
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
//        Jzvd.goOnPlayOnResume()
        autoPlayVideo()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}