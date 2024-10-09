package com.outgoer.ui.reelsdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.ablanco.zoomy.Zoomy
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
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
import com.outgoer.databinding.ActivityReelsDetailBinding
import com.outgoer.ui.home.home.SharePostReelBottomSheet
import com.outgoer.ui.home.newReels.ReelMoreOptionBottomSheet
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.reels.view.PlayReelAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reelsdetail.viewmodel.ReelsDetailViewModel
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.Utility.player
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import com.outgoer.videoplayer.OnViewPagerListener
import com.outgoer.videoplayer.ViewPagerLayoutManager
import javax.inject.Inject
import kotlin.properties.Delegates


class ReelsDetailActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_REEL_ID = "INTENT_EXTRA_REEL_ID"
        private const val REEL_SHOW_COMMENTS = "REEL_SHOW_COMMENTS"
        private const val REEL_SHOW_TAGGED_PEOPLE = "REEL_SHOW_TAGGED_PEOPLE"

        fun getIntent(context: Context, reelId: Int, showComments: Boolean = false, showTaggedPeople: Boolean = false): Intent {
            val intent = Intent(context, ReelsDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_REEL_ID, reelId)
            intent.putExtra(REEL_SHOW_COMMENTS, showComments)
            intent.putExtra(REEL_SHOW_TAGGED_PEOPLE, showTaggedPeople)
            return intent
        }

//        fun getIntentWithClearTop(context: Context, reelId: Int): Intent {
//            val intent = Intent(context, ReelsDetailActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra(INTENT_EXTRA_REEL_ID, reelId)
//            return intent
//        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsDetailViewModel>
    private lateinit var reelsDetailViewModel: ReelsDetailViewModel

    private lateinit var binding: ActivityReelsDetailBinding

    private lateinit var playReelAdapter: PlayReelAdapter

    private lateinit var videoLayoutManager: ViewPagerLayoutManager

    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1

    private var listOfReelsInfo: List<ReelInfo> = listOf()
    private var reelId: Int = -1
    private var showComments: Boolean = false
    private var showTaggedPeople: Boolean = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private var loggedInUserId by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        reelsDetailViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityReelsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            val reelId = it.getIntExtra(INTENT_EXTRA_REEL_ID, -1)
            if (reelId != -1) {
                this.reelId = reelId
                this.showComments = it.getBooleanExtra(REEL_SHOW_COMMENTS, false)
                this.showTaggedPeople = it.getBooleanExtra(REEL_SHOW_TAGGED_PEOPLE, false)
                listenToViewEvents()
                listenToViewModel()
                reelsDetailViewModel.getReelById(reelId)
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        videoLayoutManager = ViewPagerLayoutManager(this, OrientationHelper.VERTICAL)
        playReelAdapter = PlayReelAdapter(this).apply {
            playReelViewClicks.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ReelsPageState.MuteUnmuteClick -> {
                        listOfReelsInfo.forEach {
                            it.isMute = state.isMute
                        }
                        playReelAdapter.listOfDataItems = listOfReelsInfo
                    }
                    is ReelsPageState.UserProfileClick -> {
                        if(state.reelInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if(loggedInUserCache.getUserId() ==  state.reelInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            }else {
                                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(this@ReelsDetailActivity,0,state.reelInfo.user?.id ?: 0))
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@ReelsDetailActivity,
                                    state.reelInfo.userId
                                )
                            )
                        }
                    }
                    is ReelsPageState.TaggedPeopleClick -> {
                        openTaggedPeopleBottomSheet(state.reelInfo)
                    }
                    is ReelsPageState.FollowClick -> {
                        reelsDetailViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                    }
                    is ReelsPageState.UnfollowClick -> {
                        reelsDetailViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                    }
                    is ReelsPageState.AddReelLikeClick -> {
                        reelsDetailViewModel.addLikeToReel(state.reelInfo)
                    }
                    is ReelsPageState.RemoveReelLikeClick -> {
                        reelsDetailViewModel.removeLikeFromReel(state.reelInfo)
                    }
                    is ReelsPageState.CommentClick -> {
                        openCommentBottomSheet(state.reelInfo)
                    }
                    is ReelsPageState.AddBookmarkClick -> {
                        reelsDetailViewModel.addReelToBookmark(state.reelInfo)
                    }
                    is ReelsPageState.RemoveBookmarkClick -> {
                        reelsDetailViewModel.removeReelToBookmark(state.reelInfo)
                    }
                    is ReelsPageState.ShareClick -> {
                        ShareHelper.shareDeepLink(this@ReelsDetailActivity, false, state.reelInfo.id) {
//                            ShareHelper.shareText(this@ReelsDetailActivity, it)
                            var sharePostReelBottomSheet = SharePostReelBottomSheet.newInstance(it, state.reelInfo.id, "post")
                            sharePostReelBottomSheet.show(supportFragmentManager, SharePostReelBottomSheet.javaClass.name)
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
                                    reelsDetailViewModel.deleteReel(state.reelInfo.id)
                                }
                                PostMoreOption.ReportClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        Jzvd.goOnPlayOnResume()
                                        reportOptionBottomSheet.dismiss()
                                        reelsDetailViewModel.reportReels(state.reelInfo.id, reportId)
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
                        bottomSheetFragment.show(supportFragmentManager, ReelMoreOptionBottomSheet::class.java.name)
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
                }
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }
                autoPlayVideo()
                mCurrentPosition = position
            }
        })
    }

    private fun listenToViewModel() {
        reelsDetailViewModel.reelsDetailViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsDetailViewModel.ReelsDetailViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ReelsDetailViewModel.ReelsDetailViewState.LoadingState -> {
//                    buttonVisibility(it.isLoading)
                }
                is ReelsDetailViewModel.ReelsDetailViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    onBackPressed()
                }
                is ReelsDetailViewModel.ReelsDetailViewState.FollowStatusUpdate -> {
                    listOfReelsInfo = it.listOfReelsInfo ?: listOf()
                    playReelAdapter.listOfDataItems = it.listOfReelsInfo ?: listOf()
                }
                is ReelsDetailViewModel.ReelsDetailViewState.GetReelInfo -> {
                    listOfReelsInfo = listOf(it.reelInfo)
                    playReelAdapter.listOfDataItems = listOfReelsInfo
                    if (showComments) {
                        openCommentBottomSheet(it.reelInfo)
                    } else if (showTaggedPeople) {
                        openTaggedPeopleBottomSheet(it.reelInfo)
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun openCommentBottomSheet(reelInfo: ReelInfo) {
//        Jzvd.goOnPlayOnPause()
        val bottomSheet = NewReelsCommentBottomSheet(reelInfo)
        val mPos = listOfReelsInfo.indexOfFirst {
            it.id == reelInfo.id
        }
        if (mPos != -1) {
            listOfReelsInfo.toMutableList()[mPos] = reelInfo
            playReelAdapter.listOfDataItems = listOfReelsInfo
        }
        bottomSheet.show(supportFragmentManager, NewReelsCommentBottomSheet::class.java.name)
    }

    private fun openTaggedPeopleBottomSheet(reelInfo: ReelInfo) {
        Jzvd.goOnPlayOnPause()
        val bottomSheet = ReelTaggedPeopleBottomSheet(reelInfo)
        bottomSheet.show(supportFragmentManager, ReelTaggedPeopleBottomSheet::class.java.name)
    }

    private fun autoPlayVideo() {
        if (binding.reelsRecyclerView.getChildAt(0) == null) {
            return
        }
        player = binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.outgoerVideoPlayer)
        player?.alpha = 1f
        player?.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )
//            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
            startVideoAfterPreloading()
        }
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onResume() {
        Jzvd.goOnPlayOnResume()
        super.onResume()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        player = null
        super.onDestroy()
    }
}