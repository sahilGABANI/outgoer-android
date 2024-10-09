package com.outgoer.ui.tag_venue

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.OrientationHelper
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentVenueTaggedReelBinding
import com.outgoer.ui.home.newReels.ReelMoreOptionBottomSheet
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.newReels.hashtag.NewReelsHashtagActivity
import com.outgoer.ui.home.newReels.view.NewPlayReelAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.tag_venue.viewmodel.TaggedReelsPhotosViewModel
import com.outgoer.ui.tag_venue.viewmodel.VenueTaggedViewState
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.Utility
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
import kotlin.properties.Delegates

class VenueTaggedReelFragment : BaseFragment() {

    companion object {
        private val VENUE_ID = "VENUE_ID"

        @JvmStatic
        fun newInstance(venueId: Int): VenueTaggedReelFragment {
            val venueTaggedReelFragment = VenueTaggedReelFragment()

            val bundle = Bundle()
            bundle.putInt(VENUE_ID, venueId)

            venueTaggedReelFragment.arguments = bundle

            return venueTaggedReelFragment
        }
    }

    private var _binding: FragmentVenueTaggedReelBinding? = null
    private val binding get() = _binding!!

    private lateinit var newPlayReelAdapter: NewPlayReelAdapter

    private lateinit var videoLayoutManager: ViewPagerLayoutManager

    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1

    private var listOfReelsInfo: ArrayList<ReelInfo> = arrayListOf()
    private var isMute = false

    private var reelInfo: ReelInfo? = null
    private var isFromHomePageTopReelClick = false
    private var loggedInUserId by Delegates.notNull<Int>()
    private var venueId: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<TaggedReelsPhotosViewModel>
    private lateinit var taggedReelsPhotosViewModel: TaggedReelsPhotosViewModel
    private lateinit var venueReelsContext: Context


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
        OutgoerApplication.component.inject(this)
        taggedReelsPhotosViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVenueTaggedReelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loggedInUserId = loggedInUserCache.getUserId() ?: 0
        venueReelsContext = view.context
        venueId = arguments?.let {
            it.getInt(VENUE_ID)
        } ?: 0
        listenToViewModel()
        listenToViewEvents()

    }

    private fun listenToViewEvents() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
        newPlayReelAdapter = NewPlayReelAdapter(requireContext()).apply {
            if (loggedInUserId != 0) {
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
                                    venueReelsContext,
                                    storyListUtil,
                                    state.reelInfo.userId
                                )
                            } else if (state.reelInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                if (loggedInUserCache.getUserId() == state.reelInfo.userId) {
                                    RxBus.publish(RxEvent.OpenVenueUserProfile)
                                } else {
                                    startActivityWithDefaultAnimation(
                                        NewVenueDetailActivity.getIntent(
                                            requireContext(),
                                            0,
                                            state.reelInfo.user.id ?: 0
                                        )
                                    )
                                }
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewOtherUserProfileActivity.getIntent(
                                        requireContext(),
                                        state.reelInfo.userId
                                    )
                                )
                            }
                        }
                        is ReelsPageState.TaggedPeopleClick -> {
                            Jzvd.goOnPlayOnPause()
                            val bottomSheetFragment = ReelTaggedPeopleBottomSheet(state.reelInfo)
                            bottomSheetFragment.show(
                                childFragmentManager,
                                ReelTaggedPeopleBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.FollowClick -> {
                            taggedReelsPhotosViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                        }
                        is ReelsPageState.UnfollowClick -> {
                            taggedReelsPhotosViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                        }
                        is ReelsPageState.AddReelLikeClick -> {
                            val index = listOfReelsInfo.indexOf(state.reelInfo)
                            listOfReelsInfo.set(index, state.reelInfo)
                            taggedReelsPhotosViewModel.addLikeToReel(state.reelInfo)
                        }
                        is ReelsPageState.RemoveReelLikeClick -> {

                            val index = listOfReelsInfo.indexOf(state.reelInfo)
                            listOfReelsInfo.set(index, state.reelInfo)
                            taggedReelsPhotosViewModel.removeLikeFromReel(state.reelInfo)
                        }
                        is ReelsPageState.CommentClick -> {
//                            Jzvd.goOnPlayOnPause()
                            val newReelsCommentBottomSheet =
                                NewReelsCommentBottomSheet(state.reelInfo)
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
//                                Jzvd.goOnPlayOnResume()
                            }
                            newReelsCommentBottomSheet.show(
                                childFragmentManager,
                                NewReelsCommentBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.AddBookmarkClick -> {
                            taggedReelsPhotosViewModel.addReelToBookmark(state.reelInfo)
                        }
                        is ReelsPageState.RemoveBookmarkClick -> {
                            taggedReelsPhotosViewModel.removeReelToBookmark(state.reelInfo)
                        }
                        is ReelsPageState.ShareClick -> {
                            ShareHelper.shareDeepLink(requireContext(), false, state.reelInfo.id) {
                                ShareHelper.shareText(requireContext(), it)
                            }
                        }
                        is ReelsPageState.MoreClick -> {
                            Jzvd.goOnPlayOnPause()
                            val bottomSheetFragment = ReelMoreOptionBottomSheet()
                            bottomSheetFragment.reelMoreOptionClick.subscribeAndObserveOnMainThread {
                                bottomSheetFragment.dismissBottomSheet()
                                listOfReelsInfo.remove(state.reelInfo)
                                taggedReelsPhotosViewModel.deleteReel(state.reelInfo.id, "1", venueId)
                            }.autoDispose()

                            bottomSheetFragment.show(
                                childFragmentManager,
                                ReelMoreOptionBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.VenueTaggedProfileClick -> {
                            if(state.reelInfo.venueTags?.id?: 0 > 0) {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(),
                                        0,
                                        state.reelInfo.venueTags?.id ?: 0
                                    )
                                )
                            }
                        }

                    }
                }

                reelsHashtagItemClicks.subscribeAndObserveOnMainThread {
                    startActivityWithDefaultAnimation(
                        NewReelsHashtagActivity.getIntent(
                            requireContext(),
                            it
                        )
                    )
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
                nextVideoUrls?.let {
                    Timber.i("nextVideoUrl $nextVideoUrls")
                    Outgoer.exoCacheManager.prepareCacheVideos(it)
                }
                autoPlayVideo()
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                if (mCurrentPosition == position) {
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
                    taggedReelsPhotosViewModel.loadMoreVenuePostReelList(TaggedPostReelsRequest("1", venueId))
                }
            }
        })

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
            taggedReelsPhotosViewModel.resetPaginationVenuePostReelList(TaggedPostReelsRequest("1", venueId))
        }.autoDispose()

        taggedReelsPhotosViewModel.getVenuePostReelList(TaggedPostReelsRequest("1", venueId))
    }

    private fun listenToViewModel() {
        taggedReelsPhotosViewModel.venueTaggedState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueTaggedViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is VenueTaggedViewState.LoadingState -> {
                }
                is VenueTaggedViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                }
                is VenueTaggedViewState.ListOfReelInfo -> {
                    taggedReelsPhotosViewModel.getTaggedViewChange(TaggedPostReelsViewRequest("reel", venueId))

                    binding.noReelsAppCompatTextView.visibility = if(it.listofreel.size > 0) View.GONE else View.VISIBLE

                    listOfReelsInfo = it.listofreel
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    newPlayReelAdapter.listOfDataItems = it.listofreel
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
                is VenueTaggedViewState.FollowStatusUpdate -> {
                    listOfReelsInfo = (it.listOfReelsInfo ?: arrayListOf()) as ArrayList<ReelInfo>
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    newPlayReelAdapter.listOfDataItems = it.listOfReelsInfo ?: listOf()
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfReelsInfo: List<ReelInfo>) {
        if (listOfReelsInfo.isNotEmpty()) {
            //binding.llNoData.visibility = View.GONE
        } else {
            //binding.llNoData.visibility = View.VISIBLE
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
        super.onPause()
        if (isVideoInitCompleted) {
            Jzvd.goOnPlayOnPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVisible) {
            if (!isVideoInitCompleted) {
                Jzvd.goOnPlayOnPause()
                Observable.timer(500, TimeUnit.MILLISECONDS)
                    .subscribeAndObserveOnMainThread {
                        autoPlayVideo()
                    }.autoDispose()

            } else {
                Jzvd.goOnPlayOnResume()
            }
        }
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}