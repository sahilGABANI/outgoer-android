package com.outgoer.ui.home.newReels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.aws.CreateMediaType
import com.outgoer.api.post.model.CreatePostRequest
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.reels.model.CreateReelRequest
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentDiscoverReelsBinding
import com.outgoer.service.UploadingPostReelsService
import com.outgoer.ui.create_story.model.SelectedMedia
import com.outgoer.ui.home.home.SharePostReelBottomSheet
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.newReels.hashtag.NewReelsHashtagActivity
import com.outgoer.ui.home.newReels.view.NewPlayReelAdapter
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel.Companion.reelsViewState
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.Utility.prefetchedUrls
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.outgoer.cache.VideoPrefetch
import com.outgoer.utils.Utility.firstVisiblePosition
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.utils.Utility.player
import com.outgoer.videoplayer.OnViewPagerListener
import com.outgoer.videoplayer.ViewPagerLayoutManager
import io.reactivex.Observable
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class DiscoverReelsFragment : BaseFragment() {

    companion object {
        private const val VENUE_REEL_INFO = "VENUE_REEL_INFO"

        @JvmStatic
        fun newInstance() = DiscoverReelsFragment()

        @JvmStatic
        fun newInstanceWithData(listOfReelsInfo: ArrayList<ReelInfo>): DiscoverReelsFragment {
            val discoverReelsFragment = DiscoverReelsFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList(VENUE_REEL_INFO, listOfReelsInfo)
            discoverReelsFragment.arguments = bundle
            return discoverReelsFragment
        }
    }


    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsViewModel>
    private lateinit var reelsViewModel: ReelsViewModel

    private var _binding: FragmentDiscoverReelsBinding? = null
    private val binding get() = _binding!!

    private lateinit var newPlayReelAdapter: NewPlayReelAdapter
    private lateinit var videoLayoutManager: ViewPagerLayoutManager
    private var isVideoInitCompleted = false
    private var mCurrentPosition = -1
    private var listOfReelsInfo: ArrayList<ReelInfo> = arrayListOf()
    private var isMute = false
    private var loggedInUserId by Delegates.notNull<Int>()
    private var onShareIntent = false
    private var isvisible = false

    private var postType: String? = null
    private var videoPath: String? = null
    private var thumbnailPath: String? = null
    private var createPostData: CreatePostRequest? = null
    private var createReelsData: CreateReelRequest? = null
    private var listOfSelectedFiles: java.util.ArrayList<SelectedMedia> = arrayListOf()
    private var refreshTapCounter = 0
    private var isTemp = 0
    private lateinit var videoPrefetch: VideoPrefetch
    private lateinit var discoverReelsContext: Context

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        reelsViewModel = getViewModelFromFactory(viewModelFactory)
        discoverReelsContext = view.context
        videoPrefetch = VideoPrefetch(
            view.context,
            viewLifecycleOwner.lifecycleScope
        )
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

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

        if (arguments == null) {
            listenToViewModel()
            listenToViewEvents()
        }

        arguments?.let {
            if (it.getParcelableArrayList<ReelInfo>(VENUE_REEL_INFO) != null) {
                Timber.tag("VENUE_REEL_INFO").i("empty if condition")
            }
        }

        RxBus.listen(RxEvent.DataReloadReel::class.java).subscribeOnIoAndObserveOnMainThread({
            println("tabType Selected tab:  "+ it.selectedTab)
            if (it.selectedTab == "Reels") {
                isTemp = 0
                refreshTapCounter++
                Jzvd.goOnPlayOnResume()
                Timber.tag("DiscoverFragment").i("refreshTapCounter: $refreshTapCounter && firstVisiblePosition: $firstVisiblePosition")
                if (refreshTapCounter == 2 && firstVisiblePosition != 0 && firstVisiblePosition != -1) {
                    Jzvd.goOnPlayOnPause()
                    Handler(Looper.getMainLooper()).postDelayed({
                        Timber.tag("DiscoverFragment").i("RxEvent.DataReloadReel -> autoPlayVideo()")
                        autoPlayVideo()
                    }, 1000)
                    binding.reelsRecyclerView.scrollToPosition(0)
                }

                if (refreshTapCounter == 2) refreshTapCounter = 0
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private val cMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Timber.tag("DiscoverReelsFragment").i("Failed Creating Reels")
//            binding.progressDialog.isVisible = false
            postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)

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

            binding.retryAppCompatTextView.isVisible = true
        }
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val postType = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE)
            val videoPath = intent.getStringExtra(UploadingPostReelsService.INTENT_EXTRA_VIDEO_PATH)
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                binding.progressBar.isVisible = true
                binding.ivClose.isVisible = true
                binding.ivCheck.isVisible = false
                binding.progressDialog.isVisible = true

                binding.musicTitleItemAppCompatTextView.text = discoverReelsContext.resources.getString(R.string.compressing)
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
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                binding.progressDialog.isVisible = true
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
                if (isCompressingDone) {
                    binding.progressDialog.isVisible = false
                    binding.ivClose.isVisible = false
                    binding.musicTitleItemAppCompatTextView.text = discoverReelsContext.resources.getString(R.string.uploading)
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
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                binding.progressDialog.isVisible = false
                binding.ivClose.isVisible = false
                binding.musicTitleItemAppCompatTextView.text = discoverReelsContext.resources.getString(R.string.processing)
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
            Timber.tag("DiscoverReelsFragment").i("postType :$postType")
            if (postType == CreateMediaType.reels.name || postType == CreateMediaType.reels_video.name) {
                binding.progressDialog.isVisible = false
                if (context != null) {
                    Glide.with(context).load(videoPath).into(binding.ivSelectedMedia)
                }
                if (isCompressingDone) {
                    binding.progressBar.isVisible = false
                    binding.ivClose.isVisible = false
                    binding.ivCheck.isVisible = true
                    binding.musicTitleItemAppCompatTextView.text = discoverReelsContext.resources.getString(R.string.finishing_up)
                    Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                        binding.progressDialog.isVisible = true
                        reelsViewModel.pullToRefresh(1)
                    }
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
                    Observable.timer(6000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                        autoPlayVideo()
                        binding.progressDialog.isVisible = false
                    }
                }
            }
        }
    }

    private fun listenToViewEvents() {
        videoLayoutManager = ViewPagerLayoutManager(requireContext(), OrientationHelper.VERTICAL)
//        videoLayoutManager.setPreloadItemCount(3)
        videoLayoutManager.isItemPrefetchEnabled = true
        videoLayoutManager.isMeasurementCacheEnabled = true
        videoLayoutManager.initialPrefetchItemCount = 10
        videoLayoutManager.isSmoothScrollbarEnabled = true

        newPlayReelAdapter = NewPlayReelAdapter(requireContext()).apply {
            playReelViewClicks.subscribeAndObserveOnMainThread { state ->
                if (loggedInUserId != 0) {
                    when (state) {
                        is ReelsPageState.MuteUnmuteClick -> {
                            isMute = state.isMute
                            listOfReelsInfo.forEach {
                                it.isMute = isMute
                            }
                            newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                        }
                        is ReelsPageState.UserProfileClick -> {
                            Jzvd.goOnPlayOnPause()
                            if (state.reelInfo.user?.storyCount == 1) {
                                listOfDataItems?.find { it.userId == state.reelInfo.userId }?.user?.storyCount = 0
                                newPlayReelAdapter.listOfDataItems = listOfDataItems
                                toggleSelectedStory(discoverReelsContext, storyListUtil, state.reelInfo.userId)
                            } else if (state.reelInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                if (loggedInUserCache.getUserId() == state.reelInfo.userId) {
                                    RxBus.publish(RxEvent.OpenVenueUserProfile)
                                } else {
                                    startActivityWithDefaultAnimation(
                                        NewVenueDetailActivity.getIntent(
                                            requireContext(), 0, state.reelInfo.user.id ?: 0
                                        )
                                    )
                                }
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewOtherUserProfileActivity.getIntent(
                                        requireContext(), state.reelInfo.userId
                                    )
                                )
                            }
                        }
                        is ReelsPageState.TaggedPeopleClick -> {
                            Jzvd.goOnPlayOnPause()
                            val bottomSheetFragment = ReelTaggedPeopleBottomSheet(state.reelInfo)
                            bottomSheetFragment.show(
                                childFragmentManager, ReelTaggedPeopleBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.FollowClick -> {
                            reelsViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                        }
                        is ReelsPageState.UnfollowClick -> {
                            reelsViewModel.followUnfollowUser(listOfDataItems, state.reelInfo)
                        }
                        is ReelsPageState.AddReelLikeClick -> {
                            val index = listOfReelsInfo.indexOf(state.reelInfo)
                            listOfReelsInfo[index] = state.reelInfo
                            reelsViewModel.addLikeToReel(state.reelInfo)
                        }
                        is ReelsPageState.RemoveReelLikeClick -> {
                            val index = listOfReelsInfo.indexOf(state.reelInfo)
                            listOfReelsInfo[index] = state.reelInfo
                            reelsViewModel.removeLikeFromReel(state.reelInfo)
                        }
                        is ReelsPageState.CommentClick -> {
//                            Jzvd.goOnPlayOnPause()
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
//                                Jzvd.goOnPlayOnResume()
                            }
                            newReelsCommentBottomSheet.show(
                                childFragmentManager, NewReelsCommentBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.AddBookmarkClick -> {
                            reelsViewModel.addReelToBookmark(state.reelInfo)
                        }
                        is ReelsPageState.RemoveBookmarkClick -> {
                            reelsViewModel.removeReelToBookmark(state.reelInfo)
                        }
                        is ReelsPageState.ShareClick -> {
                            onShareIntent = true
                            ShareHelper.shareDeepLink(requireContext(), false, state.reelInfo.id) {
//                                ShareHelper.shareText(requireContext(), it)
                                val sharePostReelBottomSheet = SharePostReelBottomSheet.newInstance(it, state.reelInfo.id, "reel").apply {
                                    shareOptionClick.subscribeAndObserveOnMainThread {
                                        listOfReelsInfo.find { it.id == state.reelInfo.id }?.apply {
                                            this.shareCount = (this.shareCount ?: 0).plus(1)
                                        }

                                        newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                                    }
                                }
                                sharePostReelBottomSheet.show(childFragmentManager, SharePostReelBottomSheet.Companion::class.java.name)
                            }
                        }
                        is ReelsPageState.MoreClick -> {
                            Jzvd.goOnPlayOnPause()
                            val bottomSheetFragment = ReelMoreOptionBottomSheet.newInstance(state.showReport)
                            bottomSheetFragment.isCancelable = false
                            bottomSheetFragment.reelMoreOptionClick.subscribeAndObserveOnMainThread {
                                when (it) {
                                    PostMoreOption.BlockClick -> {}
                                    PostMoreOption.DeleteClick -> {
                                        bottomSheetFragment.dismissBottomSheet()
                                        listOfReelsInfo.remove(state.reelInfo)
                                        newPlayReelAdapter.listOfDataItems = listOfReelsInfo

                                        reelsViewModel.deleteReel(state.reelInfo.id, 1)
                                        Handler().postDelayed({
                                            autoPlayVideo()
                                        }, 300)
                                    }
                                    PostMoreOption.ReportClick -> {
                                        bottomSheetFragment.dismissBottomSheet()
                                        val reportOptionBottomSheet = ReportBottomSheet()
                                        reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->

                                            if(Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE)
                                                Jzvd.goOnPlayOnResume()
                                            reportOptionBottomSheet.dismiss()
                                            reelsViewModel.reportReels(state.reelInfo.id, reportId)
                                        }.autoDispose()
                                        reportOptionBottomSheet.show(
                                            childFragmentManager, ReportBottomSheet::class.java.name
                                        )
                                    }
                                    PostMoreOption.DismissClick -> {
                                        bottomSheetFragment.dismissBottomSheet()

                                        if(Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.state == Jzvd.STATE_PAUSE)
                                            Jzvd.goOnPlayOnResume()
                                    }
                                }

                            }.autoDispose()

                            bottomSheetFragment.show(
                                childFragmentManager, ReelMoreOptionBottomSheet::class.java.name
                            )
                        }
                        is ReelsPageState.VenueTaggedProfileClick -> {
                            val venueTags = state.reelInfo.venueTags
                            startActivity(
                                NewVenueDetailActivity.getIntent(
                                    requireContext(), 0, venueTags?.id ?: 0
                                )
                            )
                        }
                    }
                }
            }

            reelsHashtagItemClicks.subscribeAndObserveOnMainThread {
                if (loggedInUserId != 0) {
                    startActivityWithDefaultAnimation(
                        NewReelsHashtagActivity.getIntent(
                            requireContext(), it
                        )
                    )
                }
            }
        }
        binding.reelsRecyclerView.apply {
            layoutManager = videoLayoutManager
            adapter = newPlayReelAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    firstVisiblePosition = videoLayoutManager.findFirstCompletelyVisibleItemPosition()
                    Timber.tag("SetOnViewPagerListener").i("firstVisiblePosition: $firstVisiblePosition")
                }
            })
        }

        videoLayoutManager.setOnViewPagerListener(object : OnViewPagerListener {
            override fun onInitComplete() {
                if (isVideoInitCompleted) {
                    return
                }
                isVideoInitCompleted = true
                if (isResumed) {
                    println("init complete")
//                    if(Jzvd.CURRENT_JZVD != null && !Jzvd.CURRENT_JZVD.state.equals(1))
                    autoPlayVideo()
                }

                val reelInfo = if (listOfReelsInfo.isNotEmpty()) {
                    listOfReelsInfo[0]
                } else {
                    Timber.e("listOfReelsInfo is empty")
                    null
                }

                if (reelInfo != null) {
                    RxBus.publish(RxEvent.CurrentPostionReels(0, reelInfo.width ?: 0, reelInfo.height ?: 0))
                } else {
                    Timber.e("listOfReelsInfo is empty")
                }
            }

            override fun onPageRelease(isNext: Boolean, position: Int) {
                if (mCurrentPosition == position) {
                    Timber.tag("onPageRelease").i("empty if condition")
                }
            }

            override fun onPageSelected(position: Int, isBottom: Boolean, isLoadMore: Boolean) {
                if (mCurrentPosition == position) {
                    return
                }
                if (listOfReelsInfo.isNotEmpty()) {
                    val reelInfo = listOfReelsInfo[position]
                    RxBus.publish(RxEvent.CurrentPostionReels(position, reelInfo.width ?: 0, reelInfo.height ?: 0))
                }
                mCurrentPosition = position

                autoPlayVideo()

                if ((listOfReelsInfo.size - 4).compareTo(position) == 0) {
                    isTemp = listOfReelsInfo.size
                    reelsViewModel.loadMore(1)
                }
            }
        })

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            isVideoInitCompleted = false
            binding.swipeRefreshLayout.isRefreshing = false
            reelsViewModel.pullToRefresh(1)
        }.autoDispose()

        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            UploadingPostReelsService.stopUploading()
            binding.progressDialog.isVisible = false
            val sendIntent = Intent("cancelUploading")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(sendIntent)
        }.autoDispose()

        binding.retryAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            binding.retryAppCompatTextView.isVisible = false
            val intent = Intent(requireContext(), UploadingPostReelsService::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

            intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_POST_TYPE, postType)
            intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_THUMBNAIL_PATH, listOfSelectedFiles?.firstOrNull()?.filePath)
            intent.putExtra(UploadingPostReelsService.INTENT_EXTRA_LIST_OF_POST, listOfSelectedFiles)
            if (postType == CreateMediaType.reels_video.name || postType == CreateMediaType.reels.name) {
                intent.putExtra("createReelRequest", createReelsData)
            } else {
                intent.putExtra("createPostData", createPostData)
            }

            requireActivity().startService(intent)

            binding.progressDialog.isVisible = false
        }.autoDispose()

        reelsViewModel.pullToRefresh(1, true)
    }

    private fun listenToViewModel() {
        reelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ReelsViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is ReelsViewState.LoadingState -> {
                }
                is ReelsViewState.SuccessMessage -> {
                    newPlayReelAdapter.listOfDataItems = listOfReelsInfo
                    showLongToast(it.successMessage)
                }
                is ReelsViewState.GetAllReelsInfo -> {
                    listOfReelsInfo = it.listOfReelsInfo as ArrayList<ReelInfo>
                    prefetchInitialVideos()
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    newPlayReelAdapter.listOfDataItems = it.listOfReelsInfo
                    hideShowNoData(listOfReelsInfo)
                }
                is ReelsViewState.FollowStatusUpdate -> {
                    listOfReelsInfo = (it.listOfReelsInfo ?: arrayListOf()) as ArrayList<ReelInfo>
                    listOfReelsInfo.forEach {
                        it.isMute = isMute
                    }
                    newPlayReelAdapter.listOfDataItems = it.listOfReelsInfo ?: listOf()
                }
                ReelsViewState.RefreshData -> {
                    reelsViewModel.pullToRefresh(1)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun prefetchInitialVideos() {
        IntRange(isTemp, listOfReelsInfo.size).forEach {
            if (it >= listOfReelsInfo.size) return@forEach
            val videoUrl = listOfReelsInfo[it].videoUrl
            if (!videoUrl.isNullOrEmpty() && prefetchedUrls.add(videoUrl)) {
                videoPrefetch.prefetchHlsVideo(Uri.parse(listOfReelsInfo[it].videoUrl.plus("?clientBandwidthHint=2.5")))
            }
        }
    }

    private fun hideShowNoData(listOfReelsInfo: List<ReelInfo>) {
        if (listOfReelsInfo.isNotEmpty()) {
            Timber.tag("hideShowNoData").i("if condition empty")
        } else {
            Timber.tag("hideShowNoData").i("else condition empty")
        }
    }

    private fun autoPlayVideo() {
        println("0: " + binding.reelsRecyclerView.getChildAt(0))
        if (binding.reelsRecyclerView.getChildAt(0) == null) {
            return
        }
        isVideoInitCompleted = true
        player =
            binding.reelsRecyclerView.getChildAt(0).findViewById(R.id.outgoerVideoPlayer)
        player?.startButton?.visibility = View.GONE
        player?.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUpProgress(
                jzDataSource, Jzvd.SCREEN_NORMAL, JZMediaExoKotlin::class.java, 1998
            )
            startVideoAfterPreloading()

            if ((player?.posterImageView?.width!! > (player?.posterImageView?.height?.plus(100)
                    ?: 0)) || (player?.posterImageView?.width!! > (player?.posterImageView?.height
                    ?: 0))
            ) {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
            } else {
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isvisible = true
        println("Call resume method: " + onShareIntent)
        if (onShareIntent) {
            onShareIntent = false
            Jzvd.goOnPlayOnResume()
        } else if (isResumed) {
            Jzvd.goOnPlayOnPause()
            Observable.timer(100,TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                autoPlayVideo()
            }.autoDispose()
        }
    }
    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        player = null
        super.onPause()
        Timber.tag("DiscoverReelsFragment").i("onPause")
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        player = null
        super.onDestroy()
    }

    override fun onStop() {
        Jzvd.goOnPlayOnPause()
        player = null
        super.onStop()
        Timber.tag("DiscoverReelsFragment").i("onStop")
    }

    override fun onDestroyView() {
        Jzvd.releaseAllVideos()
        player = null
        super.onDestroyView()
        Timber.tag("DiscoverReelsFragment").i("onDestroyView")
    }
}