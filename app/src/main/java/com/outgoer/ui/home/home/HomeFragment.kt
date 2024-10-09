package com.outgoer.ui.home.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.HomePageStoryInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.ReportSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentHomeBinding
import com.outgoer.service.StoryUploadingService
import com.outgoer.service.UploadingPostReelsService
import com.outgoer.ui.comment.PostCommentBottomSheet
import com.outgoer.ui.create_story.model.SelectedMedia
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.fullscreenimage.FullScreenImageActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.home.view.HomePagePostAdapter
import com.outgoer.ui.home.home.viewmodel.HomePageViewState
import com.outgoer.ui.home.home.viewmodel.HomeViewModel
import com.outgoer.ui.home.home.viewmodel.HomeViewModel.Companion.homePageState
import com.outgoer.ui.like.LikesActivity
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.posttags.PostTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.story.StoryInfoActivity
import com.outgoer.ui.temp.TempActivity
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import com.outgoer.utils.Utility.prefetchedUrls
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.outgoer.cache.VideoPrefetch
import com.outgoer.utils.Utility.player
import com.outgoer.utils.Utility.previousFeedViewPosition
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList


class HomeFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var listOfPosts: ArrayList<PostInfo> = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var homeViewModel: HomeViewModel

    var height: Int? = null
    private var autoplayFirst = true
    private var currentItemIsVideo = false
    private var onShareIntent = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isResumed = false
    private lateinit var homePagePostAdapter: HomePagePostAdapter
    private var postType: String? = null
    private var videoPath: String? = null
    private var thumbnailPath: String? = null
    private var createPostData: CreatePostRequest? = null
    private var createReelsData: CreateReelRequest? = null
    private var listOfSelectedFiles: ArrayList<SelectedMedia> = arrayListOf()
    private lateinit var homeFragmentContext: Context
    private var isTemp = 0
    private lateinit var videoPrefetch: VideoPrefetch
    private var mCurrentPosition = -1
    private lateinit var llm: LinearLayoutManager

    private val hasNextItem: Boolean
        get() = mCurrentPosition < (homePagePostAdapter.listOfDataItems?.size ?: 0) - 1

    private val nextVideoUrls: List<String>?
        get() {
            val listOfItems = homePagePostAdapter.listOfDataItems
            if (!listOfItems.isNullOrEmpty() && hasNextItem) {
                val listData: ArrayList<String> = arrayListOf()
                listOfItems.forEach {
                    if (it.type != 1) {
                        it.images?.forEach {media ->
                            if (!media.videoUrl.isNullOrEmpty()) {
                                listData.add(media.videoUrl.plus("?clientBandwidthHint=2.5"))
                            }
                        }
                    }
                }
                listData.remove("")
                return listData
            }
            return null
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        homeViewModel = getViewModelFromFactory(viewModelFactory)
        homeFragmentContext = view.context
        videoPrefetch = VideoPrefetch(
            view.context,
            viewLifecycleOwner.lifecycleScope
        )
        llm = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        listenToViewModel()
        listenToViewEvents()

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "Feed") {
                Jzvd.goOnPlayOnPause()
                lifecycleScope.launch {
                    delay(1000)
                    isTemp = 0
                    homeViewModel.pullToRefresh(false)
                    val cal: Calendar = Calendar.getInstance()
                    val tz: TimeZone = cal.getTimeZone()
                    homeViewModel.pullToRefreshStory(tz.id, false)
                }
                binding.rvHomePagePost.scrollToPosition(0)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun openStoryView() {
        XXPermissions.with(requireContext()).permission(listOf(Permission.CAMERA)).permission(Permission.READ_MEDIA_VIDEO).permission(Permission.READ_MEDIA_IMAGES).request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                if (all) {
                    Jzvd.goOnPlayOnPause()
                    startActivity(DeeparEffectsActivity.getIntent(requireContext(), true))
                } else {
                    showToast(getString(R.string.msg_some_permission_denied))
                }
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)

                XXPermissions.startPermissionActivity(requireContext(), permissions)
                showToast(getString(R.string.msg_permission_denied))
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged", "LogNotTimber")
    private fun listenToViewEvents() {
        val cal: Calendar = Calendar.getInstance()
        val tz: TimeZone = cal.getTimeZone()
        homeViewModel.pullToRefreshStory(tz.id, true)
        homeViewModel.pullToRefresh(true)
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

        height = if (displayMetrics.heightPixels >= 1000) {
            displayMetrics.heightPixels - 500
        } else {
            displayMetrics.heightPixels - 400
        }

        homePagePostAdapter = HomePagePostAdapter(requireContext()).apply {
            venueDetailActionState.subscribeAndObserveOnMainThread {
                startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.id))
            }
            spontyActionState.subscribeAndObserveOnMainThread {
                when(it) {
                    is SpontyActionState.ReportSponty -> {
                        val postMoreOptionBottomSheet: PostMoreOptionBottomSheet = PostMoreOptionBottomSheet.newInstanceWithData(true, true)
                        postMoreOptionBottomSheet.postMoreOptionClick.subscribeAndObserveOnMainThread { more ->
                            when(more) {
                                is PostMoreOption.ReportClick -> {
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        homeViewModel.spontyReport(ReportSpontyRequest(it.commentInfo.id, reportId))
                                        reportOptionBottomSheet.dismiss()

                                        postMoreOptionBottomSheet.dismissBottomSheet()
                                    }.autoDispose()
                                    reportOptionBottomSheet.show(
                                        childFragmentManager, ReportBottomSheet::class.java.name
                                    )

                                }
                                is PostMoreOption.DismissClick -> {
                                    postMoreOptionBottomSheet.dismissBottomSheet()
                                }
                                else -> {}
                            }
                        }

                        postMoreOptionBottomSheet.show(childFragmentManager, PostMoreOptionBottomSheet.Companion::class.java.name)
                    }
                    is SpontyActionState.CommentClick -> {
                        val spontyId = it.commentInfo.id
                        val spontyReplyBottomSheet = SpontyReplyBottomSheet.newInstance(it.commentInfo.id)
                        spontyReplyBottomSheet.commentActionState.subscribeAndObserveOnMainThread { res ->
                            listOfPosts.getOrNull(2)?.sponties?.find { item -> item.id == spontyId }?.apply {
                                totalComments = res
                            }

                            homePagePostAdapter.listOfDataItems = listOfPosts
                        }
                        spontyReplyBottomSheet.show(childFragmentManager, "SpontyReplyBottomSheet")
                    }
                    is SpontyActionState.LikeDisLike ->{
                        it.commentInfo.id.let { it1 ->
                            homeViewModel.addRemoveSpontyLike(SpontyActionRequest(it1))
                        }
                    }
                    is SpontyActionState.JoinUnJoinClick -> {
                        homeViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = it.spontyResponse.id))
                    }
                    is SpontyActionState.TaggedUser -> {
                        val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                        val clickedText = it.clickedText
                        val tagsList = it.commentInfo.spontyTags ?: arrayListOf()
                        tagsList.addAll(it.commentInfo.descriptionTags ?: arrayListOf())

                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.user?.username == clickedText
                            }
                            if (tag != null) {
                                if (loggedInUserId != tag.userId) {
                                    if (tag.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        if (loggedInUserCache.getUserId() == tag.userId) {
                                            RxBus.publish(RxEvent.OpenVenueUserProfile)
                                        } else {
                                            startActivityWithDefaultAnimation(
                                                NewVenueDetailActivity.getIntent(
                                                    requireContext(),
                                                    0,
                                                    tag.userId
                                                )
                                            )
                                        }
                                    } else {
                                        startActivityWithDefaultAnimation(
                                            NewOtherUserProfileActivity.getIntent(
                                                requireContext(),
                                                tag.userId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is SpontyActionState.UserImageClick -> {
                        if (it.commentInfo.user?.storyCount == 1) {
                            listOfPosts.getOrNull(2)?.sponties?.find { it1 -> it1.userId == it.commentInfo.userId }?.user?.storyCount = 0
                            homePagePostAdapter.listOfDataItems = listOfPosts
                            toggleSelectedStory(
                                requireContext(),
                                storyListUtil,
                                it.commentInfo.userId
                            )
                        } else if (it.commentInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == it.commentInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, it.commentInfo.userId
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(), it.commentInfo.userId
                                )
                            )
                        }
                    }
                    is SpontyActionState.VideoViewClick -> {
                        Outgoer.exoCacheManager.prepareCacheVideo(it.postVideoUrl.plus("?clientBandwidthHint=2.5"))
                        startActivityWithDefaultAnimation(TempActivity.getIntent(requireContext(), it.postVideoUrl,it.postVideoThumbnailUrl))
                    }
                    is SpontyActionState.ImageClick -> {
                        requireActivity().startActivity(FullScreenActivity.getIntent(requireContext(), it.imageUrl))
                    }
                    is SpontyActionState.VenueClick -> {
//                        val navigationIntentUri =  Uri.parse("google.navigation:q=" + state.commentInfo.latitude + "," + state.commentInfo.longitude)
//
//                        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
//                        mapIntent.setPackage("com.google.android.apps.maps")
//                        startActivity(mapIntent)

                        startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.commentInfo.venueTags?.id ?: 0))
                    }
                    is SpontyActionState.CheckAction -> {
                        startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.commentInfo.venueTags?.id ?: 0))
                    }
                    else -> {

                    }
                }
            }

            homePagePostViewClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is HomePagePostInfoState.UserProfileClick -> {
                        if (state.postInfo.user?.storyCount == 1) {

                            listOfPosts.find { it.userId == state.postInfo.userId }?.user?.storyCount = 0
                            homePagePostAdapter.listOfDataItems = listOfPosts
                            toggleSelectedStory(homeFragmentContext, storyListUtil, state.postInfo.userId)
                        } else if (state.postInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.postInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, state.postInfo.userId
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), state.postInfo.userId))
                        }
                    }
                    is HomePagePostInfoState.MoreClick -> {
                        if (currentItemIsVideo) Jzvd.goOnPlayOnPause()
                        val showReport = state.postInfo.userId != loggedInUserCache.getUserId()
                        val bottomSheetFragment = PostMoreOptionBottomSheet.newInstance(showReport)
                        bottomSheetFragment.isCancelable = false
                        bottomSheetFragment.postMoreOptionClick.subscribeAndObserveOnMainThread {
                            when (it) {
                                PostMoreOption.DeleteClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    val currentJzvd = Jzvd.CURRENT_JZVD
                                    if (currentItemIsVideo && currentJzvd != null && currentJzvd.state != null && currentJzvd.state == Jzvd.STATE_PAUSE) {
                                        Jzvd.goOnPlayOnResume()
                                    }

                                    val list = homePagePostAdapter.listOfDataItems as ArrayList<PostInfo>
                                    list.remove(state.postInfo)

                                    homePagePostAdapter.listOfDataItems = listOfDataItems?.filter { it.objectType.equals("post") }

                                    homeViewModel.deletePost(state.postInfo.id)
                                }
                                PostMoreOption.ReportClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        reportOptionBottomSheet.dismiss()

                                        if (currentItemIsVideo && Jzvd.CURRENT_JZVD.state != null && Jzvd.CURRENT_JZVD.state  == Jzvd.STATE_PAUSE) Jzvd.goOnPlayOnResume()
                                        homeViewModel.reportPost(state.postInfo.id, reportId)
                                    }.autoDispose()
                                    reportOptionBottomSheet.show(
                                        childFragmentManager, ReportBottomSheet::class.java.name
                                    )
                                }
                                PostMoreOption.DismissClick -> {
                                    if (currentItemIsVideo && Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.state != null && Jzvd.CURRENT_JZVD.state  == Jzvd.STATE_PAUSE) Jzvd.goOnPlayOnResume()
                                    bottomSheetFragment.dismissBottomSheet()
                                }
                                PostMoreOption.BlockClick -> {}
                            }

                        }.autoDispose()
                        bottomSheetFragment.show(
                            childFragmentManager, PostMoreOptionBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.TaggedPeopleClick -> {
                        val bottomSheetFragment = PostTaggedPeopleBottomSheet(state.postInfo)
                        bottomSheetFragment.show(
                            childFragmentManager, PostTaggedPeopleBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.OpenTaggedPeopleClick -> {
                        startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), state.postInfo.id))
                    }
                    is HomePagePostInfoState.AddPostLikeClick -> {
                        homeViewModel.addPostLike(state.postInfo)
                    }
                    is HomePagePostInfoState.RemovePostLikeClick -> {
                        homeViewModel.removeLikeFromPost(state.postInfo)
                    }
                    is HomePagePostInfoState.PostLikeCountClick -> {
                        val likesActivity = LikesActivity.newInstanceWithData(state.postInfo.id)
                        likesActivity.show(childFragmentManager, LikesActivity.Companion::class.java.name)
                    }
                    is HomePagePostInfoState.CommentClick -> {
//                        Jzvd.goOnPlayOnPause()
                        val reelsCommentBottomSheet = PostCommentBottomSheet(state.postInfo).apply {
                            dismissClick.subscribeAndObserveOnMainThread {
                                dismiss()
//                                if (currentItemIsVideo) {
//                                    Jzvd.goOnPlayOnResume()
//                                }
                            }.autoDispose()
                        }
                        reelsCommentBottomSheet.show(
                            childFragmentManager, PostCommentBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.ShareClick -> {
                        onShareIntent = true
                        ShareHelper.shareDeepLink(requireContext(), true, state.postInfo.id) {
                            val sharePostReelBottomSheet = SharePostReelBottomSheet.newInstance(it, state.postInfo.id, "post")

                            sharePostReelBottomSheet.apply {
                                shareOptionClick.subscribeAndObserveOnMainThread {
                                    listOfPosts.find { post -> post.id == state.postInfo.id }?.let { info ->
                                        info.shareCount = (info.shareCount ?: 0).plus(1)
                                    }

                                    homePagePostAdapter.listOfDataItems = listOfPosts
                                }
                            }
                            sharePostReelBottomSheet.show(childFragmentManager, SharePostReelBottomSheet.Companion::class.java.name)
                        }
                    }
                    is HomePagePostInfoState.AddBookmarkClick -> {
                        homeViewModel.addPostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.RemoveBookmarkClick -> {
                        homeViewModel.removePostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.VenueTaggedProfileClick -> {
                        startActivityWithDefaultAnimation(
                            NewVenueDetailActivity.getIntent(
                                requireContext(), 0, state.postInfo.venueTags?.id ?: 0
                            )
                        )
                    }
                    is HomePagePostInfoState.HashtagItemClicks -> {
                    }
                    is HomePagePostInfoState.PhotoViewClick -> {
                        startActivityWithDefaultAnimation(
                            FullScreenActivity.getIntent(
                                requireContext(), state.postImageUrl
                            )
                        )
                    }
                    is HomePagePostInfoState.VideoViewClick -> {
                        Log.e("mediaVideoViewClick", state.postVideoUrl)
                        Jzvd.goOnPlayOnPause()
                        startActivityWithFadeInAnimation(
                            FullScreenImageActivity.getIntent(
                                requireContext(),
                                state.postVideoUrl
                            )
                        )
                    }
                    is HomePagePostInfoState.ChangesVideoPosition -> {
                        Log.e("ChangesVideoPosition", state.postVideoUrl.toString())
                        Jzvd.goOnPlayOnPause()
                        Observable.timer(500, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            Timber.tag("HomeFragment").i("ChangesVideoPosition -> autoPlayVideo()")
                            autoPlayVideo()
                        }.autoDispose()
                    }
                }
            }.autoDispose()

            storyViewClick.subscribeAndObserveOnMainThread {clickRes ->
                when (clickRes) {
                    is HomePageStoryInfoState.UserProfileClick -> {
                        if (clickRes.storyListResponse.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == clickRes.storyListResponse.stories.lastOrNull()?.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, clickRes.storyListResponse.stories.lastOrNull()?.userId ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(), clickRes.storyListResponse.stories.lastOrNull()?.userId ?: 0
                                )
                            )
                        }
                    }

                    is HomePageStoryInfoState.AddStoryResponseInfo -> {
                        if(clickRes.storyInfo.equals("1")) {
                            val addToYourStoryBottomSheet = AddToYourStoryBottomSheet().apply {
                                addToStoryOptionClick.subscribeAndObserveOnMainThread {
                                    openStoryView()
                                }
                            }
                            addToYourStoryBottomSheet.show(childFragmentManager, AddToYourStoryBottomSheet.Companion::class.java.name)
                        } else {
                            openStoryView()
                        }
                    }
                    is HomePageStoryInfoState.StoryResponseData -> {
                        val list = homePagePostAdapter.listOfStories
                        val index = list?.indexOf(clickRes.storyListResponse) ?: 0
                        list?.forEach { story ->
                            story.isSelected = false
                        }
                        list?.get(index)?.isSelected = !(list?.get(index)?.isSelected ?: false)
                        startActivity(StoryInfoActivity.getIntent(requireContext(), list ?: arrayListOf()))
                    }
                }
            }.autoDispose()
        }

        homePagePostAdapter.deviceHeight = height

        homePagePostAdapter.listOfStories = arrayListOf()

        llm.isMeasurementCacheEnabled = true
        llm.isItemPrefetchEnabled = true
        llm.initialPrefetchItemCount = 10
        llm.isSmoothScrollbarEnabled = true
        binding.rvHomePagePost.apply {
            layoutManager = llm
            adapter = homePagePostAdapter
            val scrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val firstVisiblePosition = llm.findFirstVisibleItemPosition()
                    val lastVisiblePosition = llm.findLastVisibleItemPosition()
                    var maxVisiblePercentage = 0f
                    var mostVisiblePosition = firstVisiblePosition

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        for (i in firstVisiblePosition..lastVisiblePosition) {
                            val view = llm.findViewByPosition(i) ?: continue
                            val visibilityPercentage =
                                calculateVisibilityPercentage(view, recyclerView)
                            if (visibilityPercentage > maxVisiblePercentage) {
                                maxVisiblePercentage = visibilityPercentage
                                mostVisiblePosition = i
                            }
                        }
                        if (mostVisiblePosition == 0) {
                            mostVisiblePosition = 1
                        }

                        Timber.tag("OnScrollStateChanged").i("mostVisiblePosition: $mostVisiblePosition")
                        if (previousFeedViewPosition != mostVisiblePosition) {
                            playVideoAtPosition(mostVisiblePosition)
                        } else {
                            val viewAtPosition = llm.findViewByPosition(mostVisiblePosition)
                            if (viewAtPosition != null) {
                                val viewPager = viewAtPosition.findViewById<ViewPager2>(R.id.viewPager2)
                                if (viewPager != null) {
                                    val childAt = viewPager.getChildAt(0)
                                    if (childAt != null) {
                                        player = childAt.findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer)
                                        player?.alpha = 1f
                                        player?.hideScreenProgress()
                                        Jzvd.goOnPlayOnResume()
                                    }
                                }
                            }
                        }
                        previousFeedViewPosition = mostVisiblePosition
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        player = null
                        Jzvd.goOnPlayOnPause()
                    }

                    val visibleItemCount = llm.childCount
                    val totalItemCount = llm.itemCount
                    val firstVisibleItemPosition = llm.findFirstVisibleItemPosition()
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                    ) {
                        isTemp = listOfPosts.size
                        homeViewModel.loadMore()
                    }
                }
            }
            addOnScrollListener(scrollListener)
        }

        binding.rvHomePagePost.setHasFixedSize(true)
        binding.rvHomePagePost.setItemAnimator(null)
        binding.rvHomePagePost.setItemViewCacheSize(50)


        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            autoplayFirst = true
            isTemp = 0
            binding.swipeRefreshLayout.isRefreshing = false
            Jzvd.goOnPlayOnPause()
            homeViewModel.pullToRefresh(false)
            homeViewModel.pullToRefreshStory(Calendar.getInstance().timeZone.id, false)
        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            UploadingPostReelsService.stopUploading()
            binding.progressDialog.isVisible = false
        }.autoDispose()

        binding.retryAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.retryAppCompatTextView.isVisible = false
            if(!postType.equals(CreateMediaType.story.name)) {
                val intent = Intent(requireContext(), UploadingPostReelsService::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE, postType)
                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH, listOfSelectedFiles.firstOrNull()?.filePath)
                intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                    intent.putExtra("createReelRequest", createReelsData)
                } else {
                    intent.putExtra("createPostData", createPostData)
                }
                requireActivity().startService(intent)
            } else {
                val intent = Intent(requireContext(), StoryUploadingService::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(StoryUploadingService.LIST_OF_STORY_DATA, listOfSelectedFiles)
                intent.putExtra(StoryUploadingService.RETRY_OPTION, true)
                requireActivity().startService(intent)

                startActivity(HomeActivity.getIntent(requireContext()))
            }

            binding.progressDialog.isVisible = false
        }.autoDispose()

        RxBus.listen(RxEvent.RefreshHomePagePostPlayVideo::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.isVisible) {
                Timber.tag("HomeFragment").i("RxEvent.RefreshHomePagePostPlayVideo -> autoPlayVideo()")
                autoPlayVideo()
            } else {
                Jzvd.goOnPlayOnPause()
            }
        }, {
            Timber.e(it)
        }).autoDispose()

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mMessageReceiver, IntentFilter("ShowProgressDialog")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            sMessageReceiver, IntentFilter("HideCompressingShowUploading")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            pMessageReceiver, IntentFilter("HideUploadingShowProcessing")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            fMessageReceiver, IntentFilter("HideProcessingShowFinished")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            cMessageReceiver, IntentFilter("cancelUploading")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            storyShowUploadingMessageReceiver, IntentFilter("ShowUploading")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            storyShowCompressingMessageReceiver, IntentFilter("ShowCompressing")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            storyShowProcessingMessageReceiver, IntentFilter("ShowProcessing")
        )
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            storyShowFinishMessageReceiver, IntentFilter("showFinish")
        )
    }

    private fun playVideoAtPosition(position: Int) {
        val viewAtPosition = llm.findViewByPosition(position)
        if (viewAtPosition != null) {
            val viewPager = viewAtPosition.findViewById<ViewPager2>(R.id.viewPager2)
            if (viewPager != null) {
                val childAt = viewPager.getChildAt(0)
                if (childAt != null) {
                    val player = childAt.findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer)
//                    player?.alpha = 1f
                    player?.apply {
                        posterImageView.scaleType = ImageView.ScaleType.CENTER
                        val jzDataSource = JZDataSource(this.videoUrl)
                        setUp(jzDataSource, Jzvd.SCREEN_TINY, JZMediaExoKotlin::class.java)
                        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                        startVideoAfterPreloading()
                    }
                    player?.hideScreenProgress()
                }
            }
        }
    }

    private fun calculateVisibilityPercentage(view: View, recyclerView: RecyclerView): Float {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        val recyclerViewRect = Rect()
        recyclerView.getGlobalVisibleRect(recyclerViewRect)
        val intersection = Rect()
        val isIntersected = intersection.setIntersect(rect, recyclerViewRect)
        if (!isIntersected) return 0f

        val visibleHeight = intersection.height()
        val totalHeight = view.height
        return visibleHeight / totalHeight.toFloat()
    }

    private val cMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)

            if(!postType.equals(CreateMediaType.story.name)) {
                videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
                thumbnailPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH)
                listOfSelectedFiles = intent.getParcelableArrayListExtra<SelectedMedia>(
                    UploadingPostReelsService.INTENT_EXTRA_LIST_OF_POST
                ) ?: arrayListOf()

                if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                    createReelsData = intent.getParcelableExtra<CreateReelRequest>("createReelRequest")
                } else {
                    createPostData = intent.getParcelableExtra<CreatePostRequest>("createPostData")
                }
            } else {
                listOfSelectedFiles = intent.getParcelableArrayListExtra<SelectedMedia>(
                    StoryUploadingService.LIST_OF_STORY_DATA
                ) as ArrayList<SelectedMedia>
            }
            binding.retryAppCompatTextView.isVisible = true
        }
    }

    private val storyShowUploadingMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val videoPath = intent.getStringExtra(StoryUploadingService.FILE_PATH)
            binding.progressDialog.isVisible = true
            binding.ivClose.isVisible = false
            binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.uploading)
            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                binding.progressDialog.isVisible = true
            }
            if (context != null) {
                Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
            }
        }
    }
    private val storyShowCompressingMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val videoPath = intent.getStringExtra(StoryUploadingService.FILE_PATH)
            binding.progressDialog.isVisible = false
            binding.ivClose.isVisible = false
            binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.compressing)
            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                binding.progressDialog.isVisible = true
            }
            if (context != null) {
                Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
            }
        }
    }
    private val storyShowProcessingMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val videoPath = intent.getStringExtra(StoryUploadingService.FILE_PATH)
            binding.progressDialog.isVisible = false
            binding.ivClose.isVisible = false
            binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.processing)
            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                binding.progressDialog.isVisible = true
            }
            if (context != null) {
                Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
            }
        }
    }
    private val storyShowFinishMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val videoPath = intent.getStringExtra(StoryUploadingService.FILE_PATH)
            binding.progressDialog.isVisible = false
            binding.progressBarIndicator.isVisible = false
            binding.ivClose.isVisible = false
            binding.ivCheck.isVisible = true
            binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.finishing_up)
            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                binding.progressDialog.isVisible = true
                val cal: Calendar = Calendar.getInstance()
                val tz: TimeZone = cal.getTimeZone()
                homeViewModel.pullToRefreshStory(tz.id, false)
            }
            Observable.timer(6000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                binding.progressDialog.isVisible = false
            }
            if (context != null) {
                Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
            }
        }
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            val videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
            Timber.tag("HomeFragment").i("postType :$postType")

            if (postType == CreateMediaType.post_video.name || postType == CreateMediaType.post.name) {
                binding.progressBarIndicator.isVisible = true
                binding.progressDialog.isVisible = true
                binding.ivClose.isVisible = true
                binding.ivCheck.isVisible = false
                binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.compressing)
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
            }
        }
    }

    private val sMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            val videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
            val isCompressingDone = intent.getBooleanExtra(UploadingPostReelsService.INTENT_VIDEO_COMPRESSING_DONE, false)
            Timber.tag("HomeFragment").i("postType :$postType")

            if (postType == CreateMediaType.post_video.name || postType == CreateMediaType.post.name) {
                binding.progressDialog.isVisible = false
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
                if (isCompressingDone) {
                    binding.ivClose.isVisible = false
                    binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.uploading)
                    Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                        binding.progressDialog.isVisible = true
                    }
                }
            }
        }
    }

    private val pMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            val videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
            val isCompressingDone = intent.getBooleanExtra(UploadingPostReelsService.INTENT_VIDEO_COMPRESSING_DONE, false)
            Timber.tag("HomeFragment").i("postType :$postType")

            if (postType == CreateMediaType.post_video.name || postType == CreateMediaType.post.name) {
                binding.progressDialog.isVisible = false
                binding.ivClose.isVisible = false
                binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.processing)
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
                if (isCompressingDone) {
                    binding.progressDialog.isVisible = true
                }
            }
        }
    }

    private val fMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            val videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
            val isCompressingDone = intent.getBooleanExtra(UploadingPostReelsService.INTENT_VIDEO_COMPRESSING_DONE, false)
            Timber.tag("HomeFragment").i("postType :$postType")

            if (postType == CreateMediaType.post_video.name || postType == CreateMediaType.post.name) {
                binding.progressDialog.isVisible = false
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
                if (isCompressingDone) {
                    binding.progressBarIndicator.isVisible = false
                    binding.ivClose.isVisible = false
                    binding.ivCheck.isVisible = true
                    binding.musicTitleItemAppCompatTextView.text = homeFragmentContext.resources.getString(R.string.finishing_up)
                    Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                        binding.progressDialog.isVisible = true
                    }
                    Observable.timer(6000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {

                        val dir = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES)

                        val outgoerDir = File(dir, "outgoer")
                        if (outgoerDir.isDirectory && outgoerDir.exists()) {
                            for (c in outgoerDir.listFiles()) {
                                c.delete()
                            }
                        }

                        val dirDownloadFile: File? = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

                        val outgoerFile = File(dirDownloadFile, "merge_video_file.mp4")
                        Timber.tag("HomeFragment").i("outgoerDir : ${outgoerDir.exists()}")
                        if (outgoerDir.exists()) {
                            outgoerFile.delete()
                        }

                        lifecycleScope.launch {
                            delay(2000)
                            Jzvd.goOnPlayOnPause()
                            homeViewModel.pullToRefresh(false)
                            binding.progressDialog.isVisible = false
                        }
                    }
                }
            }
        }
    }


    private fun autoPlayVideo() {
        if (binding.rvHomePagePost.getChildAt(0) == null && previousFeedViewPosition != 0) {
            return
        }

        if ((homeFragmentContext as HomeActivity).tabManager.activatedTab.equals(0)) {
            if (binding.rvHomePagePost.scrollX == 0) {
                if (binding.rvHomePagePost.getChildAt(0) != null && (binding.rvHomePagePost.getChildAt(
                        0
                    )
                        .findViewById<RecyclerView>(R.id.storyRecyclerView) != null || binding.rvHomePagePost.getChildAt(
                        0
                    ).findViewById<ViewPager2>(R.id.viewPager2) != null)
                ) {
                    if (binding.rvHomePagePost.getChildAt(1) != null && binding.rvHomePagePost.getChildAt(
                            1
                        )
                            .findViewById<ViewPager2>(R.id.viewPager2) != null && binding.rvHomePagePost.getChildAt(
                            1
                        ).findViewById<ViewPager2>(R.id.viewPager2).getChildAt(0)
                            .findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer) != null
                    ) {
                        player = binding.rvHomePagePost.getChildAt(1)
                            .findViewById<ViewPager2>(R.id.viewPager2).getChildAt(0)
                            .findViewById(R.id.outgoerVideoPlayer)
                        player?.alpha = 1f
                        player?.apply {
                            Timber.tag("HomeFragment").i("videoUrl :${this.videoUrl}")
                            Timber.tag("HomeFragment").i("state :${this.state}")

                            val jzDataSource = JZDataSource(this.videoUrl)
                            this.setUp(
                                jzDataSource, Jzvd.SCREEN_TINY, JZMediaExoKotlin::class.java
                            )
                            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                            startVideoAfterPreloading()
                        }
                        player?.hideScreenProgress()
                    }
                }
            }
        }
    }

    private fun listenToViewModel() {
        homePageState.subscribeAndObserveOnMainThread {
            when (it) {
                is HomePageViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("HomePageViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(requireView())
                    } else {
                        activity?.showLongToast(it.errorMessage)
                    }
                }
                is HomePageViewState.LoadingState -> {
                    if (homePagePostAdapter.listOfDataItems.isNullOrEmpty()) {
                        if (!it.isLoading) {
                            binding.progressBar.visibility = View.GONE
                            binding.llDataContainer.isVisible = !it.isLoading
                        } else {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                    }
                }
                is HomePageViewState.SuccessMessage -> {
                    activity?.showLongToast(it.successMessage)
                }
                is HomePageViewState.GetAllStoryInfo -> {
                    storyListUtil.clear()
                    storyListUtil.addAll(it.storyListInfo as ArrayList<StoryListResponse>)
                    homePagePostAdapter.listOfStories = it.storyListInfo
                }
                is HomePageViewState.GetAllPostInfo -> {
                    Jzvd.goOnPlayOnPause()
                    binding.rvHomePagePost.setItemViewCacheSize(listOfPosts.size)
                    listOfPosts = it.postInfoList as ArrayList<PostInfo>
                    prefetchInitialVideos()


                    binding.progressBar.visibility = View.GONE
                    if (it.postInfoList.isNotEmpty()) {
                        homePagePostAdapter.listOfDataItems = listOfPosts
                    }
                    hideShowNoData(it.postInfoList)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Timber.tag("HomeFragment").i("HomePageViewState.GetAllPostInfo -> autoPlayVideo()")
                        autoPlayVideo()
                    }, 1000)
                }
                is HomePageViewState.ReloadData -> {
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun prefetchInitialVideos() {
        Timber.tag("ViewScroll").w("prefetchInitialVideos() -> isTemp: $isTemp && listOfDataItems.size: ${listOfPosts.size}")
        nextVideoUrls?.let {
            Timber.i("nextVideoUrl $nextVideoUrls")
            it.forEach { videoUrl ->
                if (!videoUrl.isNullOrEmpty() && prefetchedUrls.add(videoUrl)) {
                    videoPrefetch.prefetchHlsVideo(Uri.parse(videoUrl))
                }
            }
        }
    }

    private fun hideShowNoData(postInfoList: List<PostInfo>) {
        if (postInfoList.isNotEmpty()) {
            binding.rvHomePagePost.visibility = View.VISIBLE
            binding.llNoData.visibility = View.GONE
        } else {
            binding.rvHomePagePost.visibility = View.GONE
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (isResumed) {
            autoplayFirst = true
            Jzvd.goOnPlayOnPause()
            Timber.tag("HomeFragment").i("onResume() -> autoPlayVideo()")
//            autoPlayVideo()
            playVideoAtPosition(previousFeedViewPosition)
        }
        Timber.tag("HomeFragment").i("onResume isVisible :$isVisible")
        Timber.tag("HomeFragment").i("onResume isResumed :$isResumed")
    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()
        player = null
        Timber.tag("HomeFragment").i("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Jzvd.releaseAllVideos()
        player = null
        Timber.tag("HomeFragment").i("onDestroy")
    }

    override fun onStop() {
        super.onStop()
        Jzvd.goOnPlayOnPause()
        player = null
        Timber.tag("HomeFragment").i("onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Jzvd.releaseAllVideos()
        Timber.tag("HomeFragment").i("onDestroyView")
    }
}